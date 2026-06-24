# ── Crash report readability ───────────────────────────────────────────────────
# Keep file names and line numbers so stack traces remain useful after release
# builds. Renaming the SourceFile attribute to "SourceFile" strips the original
# .kt file names from the binary while still mapping them in the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Reflection metadata ────────────────────────────────────────────────────────
# Generic type signatures are read at runtime by Gson and by Kotlin reflection.
# Annotation retention is needed by Hilt, Room, WorkManager, and Compose.
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ── Room ───────────────────────────────────────────────────────────────────────
# Room KSP generates all DAO implementations and SQL at compile time; entity
# field names are accessed by cursor-column index in the generated code, not by
# reflection, so they are safe to rename.  The only runtime requirement is that
# the abstract RoomDatabase subclass name survives (used during open/upgrade).
-keep class * extends androidx.room.RoomDatabase

# ── WorkManager ────────────────────────────────────────────────────────────────
# WorkManager persists the worker's fully-qualified class name in its own SQLite
# store and recreates it on next launch via reflection.  If R8 renames the class
# the job is silently lost.  Keep every ListenableWorker concrete class together
# with the two-argument constructor that WorkManager calls via reflection.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── OkHttp3 / Okio ────────────────────────────────────────────────────────────
# Modern OkHttp ships its own consumer rules in the AAR, but the logging
# interceptor does not; suppress the handful of warnings that come from optional
# platform classes (Conscrypt, BouncyCastle, etc.) that may not be present.
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.publicsuffix.** { *; }

# ── Retrofit 2 ────────────────────────────────────────────────────────────────
# Retrofit ships its own consumer rules.  This additional rule covers any future
# interface methods annotated with Retrofit HTTP annotations.
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── Gson ──────────────────────────────────────────────────────────────────────
# Pulled in transitively via retrofit-converter-gson.  Gson uses reflection to
# serialize/deserialize @SerializedName-annotated fields; those fields must not
# be renamed.  Custom adapters and (de)serializers are looked up by type token.
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── kotlinx.serialization ─────────────────────────────────────────────────────
# The compiler plugin generates serializers at compile time, but the companion
# object descriptor and the generated serializer classes are still looked up by
# the runtime via reflection.
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class ** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
# The main-thread dispatcher factory and the coroutine exception handler are
# discovered at runtime via ServiceLoader, which uses class names as strings.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.debug.**

# ── Google Maps / Play Services ───────────────────────────────────────────────
-dontwarn com.google.android.gms.**

# ── Vico charts ───────────────────────────────────────────────────────────────
-dontwarn com.patrykandpatrick.vico.**
