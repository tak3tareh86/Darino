package com.example.financial_health.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialHealthDao {
    @Query("SELECT * FROM financial_health_profile WHERE id = :id LIMIT 1")
    fun getProfile(id: String = "default_user_profile"): Flow<FinancialHealthEntity?>

    @Query("SELECT * FROM financial_health_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileSync(id: String = "default_user_profile"): FinancialHealthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(entity: FinancialHealthEntity)
}
