# Add project specific ProGuard rules here.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# JavaScriptInterface (CRITICAL: preserve bridge methods from R8 obfuscation/stripping)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.lumora.iptv.bridge.** { *; }

# Data Models & Moshi
-keep class com.lumora.iptv.data.model.** { *; }
-keepclassmembers class com.lumora.iptv.data.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Networking & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class kotlinx.coroutines.** { *; }
