package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.NvidiaApiService
import com.example.api.StreamEvent
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class LogEntry(val text: String, val type: LogType)
enum class LogType { INFO, OK, BRAIN, COMPILING, ERROR, USER }

class WorkspaceViewModel(
    private val context: Context,
    private val apiService: NvidiaApiService,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _reasoningContent = MutableStateFlow("")
    val reasoningContent: StateFlow<String> = _reasoningContent.asStateFlow()

    private val _workspaceHtml = MutableStateFlow("")
    val workspaceHtml: StateFlow<String> = _workspaceHtml.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val chatHistory = JSONArray()
    
    init {
        // Init with Empty File or reading existing index.html
        loadInitialHtml()
        log(LogType.INFO, "Aura Terminal Initialized.")
        log(LogType.INFO, "Ready for website generation. Enter prompt below.")
    }

    private fun log(type: LogType, message: String) {
        _logs.update { it + LogEntry("[${type.name}] $message", type) }
    }

    private fun loadInitialHtml() {
        val file = File(context.filesDir, "index.html")
        if (file.exists()) {
            _workspaceHtml.value = file.readText()
            log(LogType.OK, "Loaded existing workspace index.html")
        } else {
            val defaultHtml = "<html><head><script src=\"https://cdn.tailwindcss.com\"></script><script src=\"https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js\"></script></head><body class=\"bg-gray-900 text-white flex items-center justify-center h-screen\"><h1>Aura Workspace Empty</h1></body></html>"
            file.writeText(defaultHtml)
            _workspaceHtml.value = defaultHtml
        }
    }

    private fun getSystemInstruction(): String {
        return try {
            context.assets.open("prompts/system_instruction.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "You are an expert AI Web Developer."
        }
    }

    private fun getUserTemplate(): String {
        return try {
            context.assets.open("prompts/user_template.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "USER REQUEST: {{user_request}}\nCURRENT WORKSPACE HTML:\n```html\n{{current_html}}\n```"
        }
    }

    fun submitPrompt(prompt: String) {
        if (_isGenerating.value) return
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isEmpty()) {
            log(LogType.ERROR, "API Key is missing. Please configure it in Settings.")
            return
        }

        log(LogType.USER, prompt)
        _isGenerating.value = true
        _reasoningContent.value = ""
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (chatHistory.length() == 0) {
                    val sysItem = JSONObject().apply {
                        put("role", "system")
                        put("content", getSystemInstruction())
                    }
                    chatHistory.put(sysItem)
                }

                val templatedUser = getUserTemplate()
                    .replace("{{user_request}}", prompt)
                    .replace("{{current_html}}", _workspaceHtml.value)

                val userItem = JSONObject().apply {
                    put("role", "user")
                    put("content", templatedUser)
                }
                chatHistory.put(userItem)

                log(LogType.INFO, "Initializing connection to Nvidia Build API...")
                
                var accumulatedHtml = ""
                var isFirstReasoning = true
                var isFirstContent = true
                
                apiService.streamChatCompletion(apiKey, settingsRepository.getModelName(), chatHistory).collect { event ->
                    when (event) {
                        is StreamEvent.Reasoning -> {
                            if (isFirstReasoning) {
                                log(LogType.OK, "Connection established.")
                                log(LogType.BRAIN, "DeepSeek is thinking... Please wait.")
                                isFirstReasoning = false
                            }
                            _reasoningContent.update { it + event.text }
                        }
                        is StreamEvent.Content -> {
                            if (isFirstContent) {
                                log(LogType.COMPILING, "Applying HTML/Tailwind changes...")
                                isFirstContent = false
                            }
                            accumulatedHtml += event.text
                            // Strip ```html if present on the fly when saving/previewing, but since we asked it not to, hopefully it won't.
                            _workspaceHtml.value = accumulatedHtml.replace("```html\\n".toRegex(), "").replace("```".toRegex(), "")
                        }
                        is StreamEvent.Error -> {
                            log(LogType.ERROR, "API Error: ${event.message}")
                        }
                        is StreamEvent.Done -> {
                            log(LogType.OK, "Generation complete.")
                            saveHtmlLocally(_workspaceHtml.value)
                            
                            val assistantItem = JSONObject().apply {
                                put("role", "assistant")
                                put("content", accumulatedHtml)
                            }
                            chatHistory.put(assistantItem)
                            
                            _isGenerating.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                log(LogType.ERROR, "Exception: ${e.message}")
                _isGenerating.value = false
            }
        }
    }

    private fun saveHtmlLocally(html: String) {
        try {
            val file = File(context.filesDir, "index.html")
            file.writeText(html)
            log(LogType.INFO, "Workspace saved to storage.")
        } catch (e: Exception) {
            log(LogType.ERROR, "Failed to write workspace: ${e.message}")
        }
    }
    
    fun resetSession() {
        // Retains latest html as starting point, but wipes conversation
        while (chatHistory.length() > 0) {
            chatHistory.remove(0)
        }
        log(LogType.INFO, "Session reset. Memory cleared, retaining workspace structure.")
        _reasoningContent.value = ""
    }

    suspend fun exportWorkspaceToZip(): File? = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(context.cacheDir, "aura_workspace.zip")
            val htmlFile = File(context.filesDir, "index.html")
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                if (htmlFile.exists()) {
                    zos.putNextEntry(ZipEntry("index.html"))
                    htmlFile.inputStream().copyTo(zos)
                    zos.closeEntry()
                }
            }
            zipFile
        } catch (e: Exception) {
            null
        }
    }
}
