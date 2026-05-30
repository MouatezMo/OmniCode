package com.example.api

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Reasoning(val text: String) : StreamEvent()
    data class Content(val text: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    object Done : StreamEvent()
}

class NvidiaApiService(private val client: OkHttpClient) {
    
    fun streamChatCompletion(apiKey: String, model: String, messages: JSONArray): Flow<StreamEvent> = callbackFlow {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("stream", true)
            put("temperature", 0.6)
            put("top_p", 0.7)
            put("max_tokens", 4096)
        }
        
        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(requestBody)
            .build()
            
        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done)
                    close()
                    return
                }
                try {
                    val jsonObj = JSONObject(data)
                    val choices = jsonObj.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                        if (delta != null) {
                            val reasoning = delta.optString("reasoning_content", "")
                            if (reasoning.isNotEmpty()) {
                                trySend(StreamEvent.Reasoning(reasoning))
                            }
                            val content = delta.optString("content", "")
                            if (content.isNotEmpty()) {
                                trySend(StreamEvent.Content(content))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore JSON parsing errors for partial chunks, or report them
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = t?.message ?: "Unknown API Error: ${response?.code}"
                trySend(StreamEvent.Error(errorMsg))
                close(t)
            }
            
            override fun onClosed(eventSource: EventSource) {
                trySend(StreamEvent.Done)
                close()
            }
        }

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, eventSourceListener)
        
        awaitClose {
            eventSource.cancel()
        }
    }
}
