package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String // e.g. "Ayah", "Ibu", "Anak Sulung", "Anak Bungsu"
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // e.g. "Makanan", "Transportasi", "Pendidikan", "Hiburan", "Lainnya"
    val limitAmount: Double,
    val spentAmount: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val familyMemberId: Int,
    val memberName: String,
    val amount: Double,
    val category: String,
    val merchant: String,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "EXPENSE" or "INCOME"
    val isAutomatic: Boolean = false,
    val rawTextSource: String? = null
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g., "Reksa Dana Syariah", "Emas Logam Mulia", "Saham BBCA"
    val type: String, // "Saham", "Reksadana", "Emas", "Obligasi", "Lainnya"
    val amountInvested: Double,
    val currentValue: Double,
    val purchaseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "retirement_plans")
data class RetirementPlan(
    @PrimaryKey val id: Int = 1, // Only single retirement plan record needed
    val currentAge: Int = 30,
    val retirementAge: Int = 55,
    val monthlyNeedRetirement: Double = 10000000.0, // Expected monthly cost in today's Rupiah
    val inflationRatePercent: Double = 4.0,
    val investmentReturnPercent: Double = 8.0,
    val annualSavingContribution: Double = 12000000.0 // Annual saving for retirement
)
