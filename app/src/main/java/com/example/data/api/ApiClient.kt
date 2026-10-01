package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.security.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // Do not attach token for public auth / register endpoints
        val isPublicEndpoint = path.contains("/api/v1/auth/login") ||
                path.contains("/api/v1/auth/register") ||
                path.contains("/api/v1/auth/refresh")

        val builder = original.newBuilder()

        if (!isPublicEndpoint) {
            SessionManager.accessToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }

        // Add user agent
        builder.header("User-Agent", "Android-FinanceManager/1.0")

        // Attach Idempotency-Key for mutating requests (POST, PUT, DELETE) if not already present
        if ((original.method == "POST" || original.method == "PUT" || original.method == "DELETE") &&
            original.header("Idempotency-Key") == null
        ) {
            builder.header("Idempotency-Key", UUID.randomUUID().toString())
        }

        return chain.proceed(builder.build())
    }
}

class TokenAuthenticator(private val moshi: Moshi) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.contains("/api/v1/auth/refresh") || path.contains("/api/v1/auth/login")) {
            return null
        }

        // Avoid infinite 401 retry loops (retry at most once)
        if (responseCount(response) >= 2) {
            Log.w("TokenAuthenticator", "Exceeded maximum retry attempts on 401 challenge")
            return null
        }

        val currentRefreshToken = SessionManager.refreshToken ?: run {
            Log.w("TokenAuthenticator", "No refresh token available, session expired")
            SessionManager.onSessionExpired()
            return null
        }

        synchronized(lock) {
            // Check if another thread already refreshed the token
            val currentAccessToken = SessionManager.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentAccessToken != null && currentAccessToken != requestToken) {
                // Token was already refreshed by another thread
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            Log.i("TokenAuthenticator", "Attempting token refresh with backend...")
            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val jsonPayload = "{\"refreshToken\":\"$currentRefreshToken\"}"
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val refreshRequest = Request.Builder()
                .url(SessionManager.baseUrl + "api/v1/auth/refresh")
                .post(jsonPayload.toRequestBody(mediaType))
                .header("User-Agent", "Android-FinanceManager/1.0")
                .build()

            try {
                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val bodyString = refreshResponse.body?.string()
                    if (bodyString != null) {
                        val responseType = com.squareup.moshi.Types.newParameterizedType(
                            NetworkApiResponse::class.java,
                            NetworkAuthResponse::class.java
                        )
                        val adapter = moshi.adapter<NetworkApiResponse<NetworkAuthResponse>>(responseType)
                        val parsed = adapter.fromJson(bodyString)

                        if (parsed?.success == true && parsed.data != null) {
                            val newAuth = parsed.data
                            SessionManager.onTokensRefreshed(
                                newAccessToken = newAuth.accessToken,
                                newRefreshToken = newAuth.refreshToken,
                                expiresInMs = newAuth.expiresInMs
                            )
                            Log.i("TokenAuthenticator", "Token refreshed successfully")

                            return response.request.newBuilder()
                                .header("Authorization", "Bearer ${newAuth.accessToken}")
                                .build()
                        }
                    }
                } else {
                    Log.w("TokenAuthenticator", "Token refresh failed with HTTP code: ${refreshResponse.code}")
                    SessionManager.onSessionExpired()
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Exception during token refresh", e)
            }
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

object ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .authenticator(TokenAuthenticator(moshi))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedRetrofit: Retrofit? = null
    private var lastBaseUrl: String? = null

    private fun getRetrofit(): Retrofit {
        val currentBaseUrl = SessionManager.baseUrl
        val existing = cachedRetrofit
        if (existing != null && lastBaseUrl == currentBaseUrl) {
            return existing
        }

        return synchronized(this) {
            val retro = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            cachedRetrofit = retro
            lastBaseUrl = currentBaseUrl
            retro
        }
    }

    val healthApi: HealthApi
        get() = getRetrofit().create(HealthApi::class.java)

    val authApi: AuthApi
        get() = getRetrofit().create(AuthApi::class.java)

    val phoneApi: PhoneApi
        get() = getRetrofit().create(PhoneApi::class.java)

    val reminderApi: ReminderApi
        get() = getRetrofit().create(ReminderApi::class.java)

    val notificationApi: NotificationApi
        get() = getRetrofit().create(NotificationApi::class.java)

    val smsApi: SmsApi
        get() = getRetrofit().create(SmsApi::class.java)
}
