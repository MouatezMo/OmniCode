package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.api.NvidiaApiService
import com.example.data.SettingsRepository
import com.example.viewmodel.WorkspaceViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class WorkspaceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .build()
            val apiService = NvidiaApiService(okHttpClient)
            val settingsRepo = SettingsRepository(context)
            @Suppress("UNCHECKED_CAST")
            return WorkspaceViewModel(context, apiService, settingsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
