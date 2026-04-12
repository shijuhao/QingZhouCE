package com.example.toolbox

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

object TokenManager {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_TOKEN = "safeToken"
    private const val KEY_LANZOU_COOKIE = "safeLanzouCookie"
    private const val KEY_ALIAS = "app_token_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)  // 强制使用随机 IV
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedData: String): String? {
        return try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveSecure(context: Context, key: String, value: String) {
        try {
            val encrypted = encrypt(value)
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit { putString(key, encrypted) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecure(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val encrypted = prefs.getString(key, null) ?: return null
        return decrypt(encrypted)
    }

    fun save(context: Context, token: String) {
        saveSecure(context, KEY_TOKEN, token)
    }

    fun get(context: Context): String? {
        return getSecure(context, KEY_TOKEN)
    }

    fun getOld(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString("token", null)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_TOKEN)
            apply()
        }
    }

    fun saveLanzouCookie(context: Context, cookie: String) {
        if (cookie.isBlank()) return
        saveSecure(context, KEY_LANZOU_COOKIE, cookie)
    }

    fun getLanzouCookie(context: Context): String? {
        val secureCookie = getSecure(context, KEY_LANZOU_COOKIE)
        if (!secureCookie.isNullOrBlank()) return secureCookie

        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val oldKeys = listOf("lanzou_cookie", "lanzouCookie", "lzy_cookie", "cookie_lanzou")
        oldKeys.forEach { key ->
            val oldValue = prefs.getString(key, null)
            if (!oldValue.isNullOrBlank()) {
                saveLanzouCookie(context, oldValue)
                prefs.edit { remove(key) }
                return oldValue
            }
        }
        return null
    }

    fun clearLanzouCookie(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit {
            remove(KEY_LANZOU_COOKIE)
            remove("lanzou_cookie")
            remove("lanzouCookie")
            remove("lzy_cookie")
            remove("cookie_lanzou")
        }
    }

    fun saveTagStatus(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt("tagStatus", value)
            apply()
        }
    }

    fun getTagStatus(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getInt("tagStatus", 0)
    }

    fun saveUserID(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt("userid", value)
            apply()
        }
    }

    fun getUserID(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getInt("userid", 0)
    }
}
