package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsRepository(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun getApiKey(): String {
        return sharedPreferences.getString("api_key", "") ?: ""
    }
    
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }
    
    fun getModelName(): String {
        return sharedPreferences.getString("model_name", "deepseek-ai/deepseek-v4-flash") ?: "deepseek-ai/deepseek-v4-flash"
    }

    fun saveModelName(modelName: String) {
        sharedPreferences.edit().putString("model_name", modelName).apply()
    }
}
