package com.example.ui.screens.check.data

import com.example.ui.screens.check.model.Check
import kotlinx.coroutines.flow.Flow

enum class CheckOperationResult {
    SUCCESS,
    VALIDATION_ERROR,
    NOT_FOUND,
    PERSISTENCE_ERROR,
    NO_AUTHENTICATED_USER
}

interface CheckRepository {
    fun getChecks(): Flow<List<Check>>
    suspend fun getCheckById(id: Int): Check?
    suspend fun addCheck(check: Check): CheckOperationResult
    suspend fun updateCheck(check: Check): CheckOperationResult
    suspend fun deleteCheck(id: Int): CheckOperationResult
}
