package com.example.loan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanCalculationDao {

    @Query("SELECT * FROM loan_calculations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllCalculations(userId: String): Flow<List<LoanCalculationEntity>>

    @Query("SELECT * FROM loan_calculations WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllCalculationsList(userId: String): List<LoanCalculationEntity>

    @Query("SELECT * FROM loan_calculations WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getCalculationById(userId: String, id: String): LoanCalculationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(entity: LoanCalculationEntity)

    @Query("DELETE FROM loan_calculations WHERE userId = :userId AND id = :id")
    suspend fun deleteCalculation(userId: String, id: String): Int

    @Query("DELETE FROM loan_calculations WHERE userId = :userId")
    suspend fun clearAllCalculations(userId: String)

    @Query("UPDATE loan_calculations SET userId = :userId WHERE userId = 'legacy_unassigned'")
    suspend fun claimLegacyCalculations(userId: String)
}
