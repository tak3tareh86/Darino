package com.example.loan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanCalculationDao {

    @Query("SELECT * FROM loan_calculations ORDER BY createdAt DESC")
    fun getAllCalculations(): Flow<List<LoanCalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(entity: LoanCalculationEntity)

    @Query("DELETE FROM loan_calculations WHERE id = :id")
    suspend fun deleteCalculation(id: String)

    @Query("DELETE FROM loan_calculations")
    suspend fun clearAllCalculations()
}
