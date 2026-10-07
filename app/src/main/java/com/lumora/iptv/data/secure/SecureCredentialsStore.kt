package com.lumora.iptv.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lumora.iptv.data.model.Credentials
import com.lumora.iptv.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecureCredentialsStore {
    suspend fun saveCredentials(credentials: Credentials)
    suspend fun loadCredentials(): Credentials?
    suspend fun clearCredentials()
}

/**
 * Android Keystore AES-GCM implementation for API 26+ / API 30+.
 * Encrypts sensitive IPTV credentials without hardcoding keys or using deprecated methods.
 */
class KeystoreCredentialsStore(context: Context) : SecureCredentialsStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumora_secure_prefs", Context.MODE_PRIVATE)

    private val keyAlias = "LumoraIptvKeyAlias_v1"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 128

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Format: IV(Base64):CipherBytes(Base64)
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty() || !encryptedText.contains(":")) return ""
        val parts = encryptedText.split(":")
        if (parts.size != 2) return ""
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    override suspend fun saveCredentials(credentials: Credentials) = withContext(Dispatchers.IO) {
        try {
            val encPass = encrypt(credentials.password)
            prefs.edit()
                .putString("server_url", credentials.serverUrl)
                .putString("username", credentials.username)
                .putString("enc_password", encPass)
                .putString("source_type", credentials.sourceType)
                .putString("m3u_url", credentials.m3uUrl ?: "")
                .putString("expiry_date", credentials.expiryDate ?: "")
                .apply()
            AppLogger.i("SecureStore", "Credentials encrypted and saved successfully")
        } catch (e: Exception) {
            AppLogger.e("SecureStore", "Failed to save secure credentials", e)
        }
    }

    override suspend fun loadCredentials(): Credentials? = withContext(Dispatchers.IO) {
        try {
            val serverUrl = prefs.getString("server_url", null) ?: return@withContext null
            val username = prefs.getString("username", "") ?: ""
            val encPass = prefs.getString("enc_password", "") ?: ""
            val sourceType = prefs.getString("source_type", "xtream") ?: "xtream"
            val m3uUrl = prefs.getString("m3u_url", null)
            val expiryDate = prefs.getString("expiry_date", null)
            val pass = decrypt(encPass)

            Credentials(
                serverUrl = serverUrl,
                username = username,
                password = pass,
                sourceType = sourceType,
                m3uUrl = m3uUrl,
                expiryDate = expiryDate
            )
        } catch (e: Exception) {
            AppLogger.e("SecureStore", "Failed to load/decrypt credentials", e)
            null
        }
    }

    override suspend fun clearCredentials() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        AppLogger.i("SecureStore", "Credentials cleared from secure store")
    }
}
