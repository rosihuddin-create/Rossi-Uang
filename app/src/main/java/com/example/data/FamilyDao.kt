package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    // --- Family Members ---
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    // --- Budgets ---
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Query("UPDATE budgets SET spentAmount = spentAmount + :amount WHERE category = :category")
    suspend fun addSpentToBudget(category: String, amount: Double)

    @Query("UPDATE budgets SET spentAmount = spentAmount - :amount WHERE category = :category")
    suspend fun subtractSpentFromBudget(category: String, amount: Double)

    @Query("UPDATE budgets SET spentAmount = (SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category = :category AND type = 'EXPENSE') WHERE category = :category")
    suspend fun recalculateBudgetSpent(category: String)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // --- Investments ---
    @Query("SELECT * FROM investments ORDER BY purchaseDate DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    // --- Retirement Plan ---
    @Query("SELECT * FROM retirement_plans WHERE id = 1 LIMIT 1")
    fun getRetirementPlan(): Flow<RetirementPlan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRetirementPlan(plan: RetirementPlan)
}
