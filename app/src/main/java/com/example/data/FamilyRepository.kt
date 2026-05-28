package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FamilyRepository(private val familyDao: FamilyDao) {

    // Streams of data
    val members: Flow<List<FamilyMember>> = familyDao.getAllMembers()
    val budgets: Flow<List<Budget>> = familyDao.getAllBudgets()
    val transactions: Flow<List<Transaction>> = familyDao.getAllTransactions()
    val investments: Flow<List<Investment>> = familyDao.getAllInvestments()
    val retirementPlan: Flow<RetirementPlan?> = familyDao.getRetirementPlan()

    suspend fun insertMember(member: FamilyMember) = withContext(Dispatchers.IO) {
        familyDao.insertMember(member)
    }

    suspend fun deleteMember(member: FamilyMember) = withContext(Dispatchers.IO) {
        familyDao.deleteMember(member)
    }

    suspend fun insertBudget(budget: Budget) = withContext(Dispatchers.IO) {
        familyDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) = withContext(Dispatchers.IO) {
        familyDao.deleteBudget(budget)
    }

    suspend fun addTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        // Insert transaction
        familyDao.insertTransaction(transaction)
        // If it is an expense, update corresponding budget spentAmount
        if (transaction.type == "EXPENSE") {
            familyDao.addSpentToBudget(transaction.category, transaction.amount)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        familyDao.deleteTransaction(transaction)
        // Recalculate spentAmount for this category
        if (transaction.type == "EXPENSE") {
            familyDao.recalculateBudgetSpent(transaction.category)
        }
    }

    suspend fun insertInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        familyDao.insertInvestment(investment)
    }

    suspend fun deleteInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        familyDao.deleteInvestment(investment)
    }

    suspend fun saveRetirementPlan(plan: RetirementPlan) = withContext(Dispatchers.IO) {
        familyDao.saveRetirementPlan(plan)
    }

    // Initialize with helpful Indonesian default data if database is brand new
    suspend fun checkAndPrepopulateData() = withContext(Dispatchers.IO) {
        val existingMembers = members.firstOrNull() ?: emptyList()
        if (existingMembers.isEmpty()) {
            // 1. Populating family members
            val ayahId = familyDao.insertMember(FamilyMember(name = "Ayah Budi", role = "Ayah")).toInt()
            val ibuId = familyDao.insertMember(FamilyMember(name = "Ibu Linda", role = "Ibu")).toInt()
            val anakId = familyDao.insertMember(FamilyMember(name = "Kakak Siska", role = "Anak Sulung")).toInt()

            // 2. Budget categories
            familyDao.insertBudget(Budget(category = "Makanan", limitAmount = 5000000.0, spentAmount = 0.0))
            familyDao.insertBudget(Budget(category = "Transportasi", limitAmount = 2000000.0, spentAmount = 0.0))
            familyDao.insertBudget(Budget(category = "Pendidikan", limitAmount = 3000000.0, spentAmount = 0.0))
            familyDao.insertBudget(Budget(category = "Hiburan", limitAmount = 1500000.0, spentAmount = 0.0))
            familyDao.insertBudget(Budget(category = "Lainnya", limitAmount = 2500000.0, spentAmount = 0.0))

            // 3. Transactions
            // Using addTransaction to ensure budgets update
            addTransaction(Transaction(
                familyMemberId = ibuId,
                memberName = "Ibu Linda",
                amount = 450000.0,
                category = "Makanan",
                merchant = "Superindo",
                type = "EXPENSE",
                isAutomatic = false
            ))
            addTransaction(Transaction(
                familyMemberId = ayahId,
                memberName = "Ayah Budi",
                amount = 150000.0,
                category = "Transportasi",
                merchant = "Pertamina",
                type = "EXPENSE",
                isAutomatic = true,
                rawTextSource = "SMS: Transaksi Pertamina Rp 150.000 Berhasil."
            ))
            addTransaction(Transaction(
                familyMemberId = ayahId,
                memberName = "Ayah Budi",
                amount = 1200000.0,
                category = "Pendidikan",
                merchant = "Sekolah Harapan",
                type = "EXPENSE",
                isAutomatic = false
            ))
            addTransaction(Transaction(
                familyMemberId = anakId,
                memberName = "Kakak Siska",
                amount = 350000.0,
                category = "Hiburan",
                merchant = "Cinema XXI",
                type = "EXPENSE",
                isAutomatic = true,
                rawTextSource = "Notifikasi: Pembayaran QRIS ke Cinema XXI senilai Rp350.000 sukses!"
            ))

            addTransaction(Transaction(
                familyMemberId = ayahId,
                memberName = "Ayah Budi",
                amount = 15000000.0,
                category = "Gaji",
                merchant = "PT Makmur Sejahtera",
                type = "INCOME",
                isAutomatic = false
            ))
            addTransaction(Transaction(
                familyMemberId = ibuId,
                memberName = "Ibu Linda",
                amount = 4500000.0,
                category = "Freelance",
                merchant = "Desain Freelance",
                type = "INCOME",
                isAutomatic = false
            ))

            // 4. Investments
            familyDao.insertInvestment(Investment(
                name = "Emas Logam Mulia ANTAM",
                type = "Emas",
                amountInvested = 10000000.0,
                currentValue = 12500000.0
            ))
            familyDao.insertInvestment(Investment(
                name = "Saham Bank Central Asia (BBCA)",
                type = "Saham",
                amountInvested = 15000000.0,
                currentValue = 18200000.0
            ))
            familyDao.insertInvestment(Investment(
                name = "Reksadana Sucorinvest Money Market",
                type = "Reksadana",
                amountInvested = 5000000.0,
                currentValue = 5400000.0
            ))

            // 5. Retirement plan defaults
            familyDao.saveRetirementPlan(RetirementPlan(
                currentAge = 35,
                retirementAge = 55,
                monthlyNeedRetirement = 12000000.0,
                inflationRatePercent = 4.0,
                investmentReturnPercent = 8.0,
                annualSavingContribution = 24000000.0
            ))
        }
    }
}
