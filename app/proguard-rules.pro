-keep class space.linuxct.pulseloop.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * { @com.google.gson.annotations.SerializedName <fields>; }
-keep @kotlinx.serialization.Serializable class **
-keepclassmembers class ** { @kotlinx.serialization.Serializable *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**


# Preserve error/warning log calls so they survive release builds for crash monitoring
-keep class android.util.Log {
    public static int w(java.lang.String, java.lang.String);
    public static int w(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int e(java.lang.String, java.lang.String);
    public static int e(java.lang.String, java.lang.String, java.lang.Throwable);
}
