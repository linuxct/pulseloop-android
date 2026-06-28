package space.linuxct.pulseloop.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for OTLP export credentials (endpoint password, bearer
 * token, sensitive custom-header value). Kept separate from the plaintext DataStore so secrets
 * never land in `pulse_prefs`. All access is on [Dispatchers.IO]; the underlying
 * [EncryptedSharedPreferences] is created lazily (key generation does disk I/O).
 */
@Singleton
class SecretStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "pulse_secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    suspend fun getPassword(): String? = read(KEY_PASSWORD)
    suspend fun setPassword(value: String?) = write(KEY_PASSWORD, value)

    suspend fun getBearerToken(): String? = read(KEY_BEARER)
    suspend fun setBearerToken(value: String?) = write(KEY_BEARER, value)

    suspend fun getHeaderValue(): String? = read(KEY_HEADER_VALUE)
    suspend fun setHeaderValue(value: String?) = write(KEY_HEADER_VALUE, value)

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    private suspend fun write(key: String, value: String?) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(key) else putString(key, value)
        }.apply()
    }

    private companion object {
        const val KEY_PASSWORD = "otlp_password"
        const val KEY_BEARER = "otlp_bearer_token"
        const val KEY_HEADER_VALUE = "otlp_header_value"
    }
}
