package com.example.financial_health.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialHealthDao {
    @Query("SELECT * FROM financial_health_profile WHERE userId = :userId AND id = :id LIMIT 1")
    fun getProfile(userId: String, id: String): Flow<FinancialHealthEntity?>

    @Query("SELECT * FROM financial_health_profile WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getProfileSync(userId: String, id: String): FinancialHealthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(entity: FinancialHealthEntity)
}
