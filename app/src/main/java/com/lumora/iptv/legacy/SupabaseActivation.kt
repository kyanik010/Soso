package com.lumora.iptv.legacy

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Legacy Supabase Activation Manager (Disabled as requested in Question 5, preserved for backward compatibility).
 */
data class ActivationStatus(
    val deviceId: String,
    val isActivated: Boolean,
    val isTrialActive: Boolean,
    val trialDaysRemaining: Int,
    val expiryDate: String,
    val message: String
)

class SupabaseActivationManager(private val context: Context) {

    fun getDeviceId(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(androidId.toByteArray())
                val hexString = StringBuilder()
                for (b in hash) {
                    val hex = Integer.toHexString(0xff and b.toInt())
                    if (hex.length == 1) hexString.append('0')
                    hexString.append(hex)
                }
                "LUMORA-" + hexString.toString().substring(0, 10).uppercase(Locale.ROOT)
            } else {
                "LUMORA-DEV-" + UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.ROOT)
            }
        } catch (e: Exception) {
            "LUMORA-X892-A41F"
        }
    }

    fun verifyActivationCode(code: String): ActivationStatus {
        val trimmed = code.trim().uppercase(Locale.ROOT)
        val deviceId = getDeviceId()
        return if (trimmed.startsWith("VIP-") || trimmed.startsWith("LUMORA-") || trimmed.length >= 8) {
            ActivationStatus(
                deviceId = deviceId,
                isActivated = true,
                isTrialActive = false,
                trialDaysRemaining = 365,
                expiryDate = "2027-12-31",
                message = "تم تفعيل الاشتراك بنجاح!"
            )
        } else {
            ActivationStatus(
                deviceId = deviceId,
                isActivated = true,
                isTrialActive = false,
                trialDaysRemaining = 365,
                expiryDate = "2027-12-31",
                message = "مفعل"
            )
        }
    }
}
