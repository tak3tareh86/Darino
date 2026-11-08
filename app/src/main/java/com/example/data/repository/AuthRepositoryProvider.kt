package com.example.data.repository

import android.content.Context

/**
 * Singleton provider for AuthRepository.
 * Provides device-local authentication with no remote account dependency.
 */
object AuthRepositoryProvider {
    @Volatile
    private var instance: AuthRepository? = null

    fun get(context: Context): AuthRepository {
        return instance ?: synchronized(this) {
            val created = MockAuthRepository(context.applicationContext)
            instance = created
            created
        }
    }
}
