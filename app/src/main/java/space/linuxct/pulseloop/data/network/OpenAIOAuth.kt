package space.linuxct.pulseloop.data.network

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

private fun d(v: IntArray) = v.map { (it xor 0x5E).toChar() }.joinToString("")

private val CLIENT_ID     = d(intArrayOf(63, 46, 46, 1, 27, 19, 49, 63, 51, 27, 27, 4, 105, 109, 56, 110, 29, 53, 6, 63, 6, 46, 105, 54, 44, 63, 48, 48))
private val AUTHORIZE_URL = d(intArrayOf(54, 42, 42, 46, 45, 100, 113, 113, 63, 43, 42, 54, 112, 49, 46, 59, 48, 63, 55, 112, 61, 49, 51, 113, 49, 63, 43, 42, 54, 113, 63, 43, 42, 54, 49, 44, 55, 36, 59))
private val TOKEN_URL     = d(intArrayOf(54, 42, 42, 46, 45, 100, 113, 113, 63, 43, 42, 54, 112, 49, 46, 59, 48, 63, 55, 112, 61, 49, 51, 113, 49, 63, 43, 42, 54, 113, 42, 49, 53, 59, 48))
private val SCOPE         = d(intArrayOf(49, 46, 59, 48, 55, 58, 126, 46, 44, 49, 56, 55, 50, 59, 126, 59, 51, 63, 55, 50, 126, 49, 56, 56, 50, 55, 48, 59, 1, 63, 61, 61, 59, 45, 45))
val OAUTH_REDIRECT_URI    = d(intArrayOf(54, 42, 42, 46, 100, 113, 113, 50, 49, 61, 63, 50, 54, 49, 45, 42, 100, 111, 106, 107, 107, 113, 63, 43, 42, 54, 113, 61, 63, 50, 50, 60, 63, 61, 53))

data class OAuthFlow(
    val url: String,
    val state: String,
    val verifier: String
)

data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

fun createOAuthFlow(): OAuthFlow {
    val verifier  = newVerifier()
    val challenge = codeChallenge(verifier)
    val state     = newState()

    val url = Uri.parse(AUTHORIZE_URL).buildUpon()
        .appendQueryParameter("response_type",              "code")
        .appendQueryParameter("client_id",                  CLIENT_ID)
        .appendQueryParameter("redirect_uri",               OAUTH_REDIRECT_URI)
        .appendQueryParameter("scope",                      SCOPE)
        .appendQueryParameter("code_challenge",             challenge)
        .appendQueryParameter("code_challenge_method",      "S256")
        .appendQueryParameter("state",                      state)
        .appendQueryParameter(d(intArrayOf(55, 58, 1, 42, 49, 53, 59, 48, 1, 63, 58, 58, 1, 49, 44, 57, 63, 48, 55, 36, 63, 42, 55, 49, 48, 45)), "true")
        .appendQueryParameter(d(intArrayOf(61, 49, 58, 59, 38, 1, 61, 50, 55, 1, 45, 55, 51, 46, 50, 55, 56, 55, 59, 58, 1, 56, 50, 49, 41)), "true")
        .appendQueryParameter(d(intArrayOf(49, 44, 55, 57, 55, 48, 63, 42, 49, 44)), d(intArrayOf(61, 49, 58, 59, 38, 1, 61, 50, 55, 1, 44, 45)))
        .build().toString()

    return OAuthFlow(url = url, state = state, verifier = verifier)
}

suspend fun exchangeAuthorizationCode(code: String, verifier: String): OAuthTokens =
    requestToken(
        "grant_type"    to "authorization_code",
        "client_id"     to CLIENT_ID,
        "code"          to code,
        "code_verifier" to verifier,
        "redirect_uri"  to OAUTH_REDIRECT_URI
    )

suspend fun refreshOAuthToken(refreshToken: String): OAuthTokens =
    requestToken(
        "grant_type"    to "refresh_token",
        "client_id"     to CLIENT_ID,
        "refresh_token" to refreshToken
    )

private val http by lazy {
    OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

private suspend fun requestToken(vararg params: Pair<String, String>): OAuthTokens =
    withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
        val req  = Request.Builder().url(TOKEN_URL).post(body).build()
        val resp = http.newCall(req).execute()
        val text = resp.body?.string() ?: error("Empty token response")
        if (!resp.isSuccessful) error("Token request failed ${resp.code}: $text")
        val j = JSONObject(text)
        OAuthTokens(
            accessToken  = j.getString("access_token"),
            refreshToken = j.getString("refresh_token"),
            expiresIn    = j.getInt("expires_in")
        )
    }

private fun newVerifier(): String {
    val b = ByteArray(32)
    SecureRandom().nextBytes(b)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
}

private fun codeChallenge(verifier: String): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}

private fun newState(): String {
    val b = ByteArray(16)
    SecureRandom().nextBytes(b)
    return b.joinToString("") { "%02x".format(it) }
}
