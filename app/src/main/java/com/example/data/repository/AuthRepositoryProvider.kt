package com.example.data.repository

import android.content.Context

/**
 * Singleton provider for AuthRepository.
 * Provides real [RemoteAuthRepository] for production authentication.
 */
object AuthRepositoryProvider {
    @Volatile
    private var instance: AuthRepository? = null

    fun get(context: Context): AuthRepository {
        return instance ?: synchronized(this) {
            val created = RemoteAuthRepository(context.applicationContext)
            instance = created
            created
        }
    }
}
