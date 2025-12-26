package com.example.rakutenmonitor.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(userId: String, password: String) {
        sharedPreferences.edit()
            .putString("user_id", userId)
            .putString("password", password)
            .apply()
    }

    fun getUserId(): String? = sharedPreferences.getString("user_id", null)
    fun getPassword(): String? = sharedPreferences.getString("password", null)

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
