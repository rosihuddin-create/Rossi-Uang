package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FamilyViewModel(application: Application, private val repository: FamilyRepository) : AndroidViewModel(application) {

    // Main local data flows
    val members: StateFlow<List<FamilyMember>> = repository.members
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investments: StateFlow<List<Investment>> = repository.investments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val retirementPlan: StateFlow<RetirementPlan?> = repository.retirementPlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Interactive States
    private val _selectedMember = MutableStateFlow<FamilyMember?>(null)
    val selectedMember: StateFlow<FamilyMember?> = _selectedMember.asStateFlow()

    private val _cloudSyncState = MutableStateFlow<SyncState>(SyncState.Synced("Sinkronisasi cloud terakhir: baru saja"))
    val cloudSyncState: StateFlow<SyncState> = _cloudSyncState.asStateFlow()

    private val _autoParseState = MutableStateFlow<AutoParseUIState>(AutoParseUIState.Idle)
    val autoParseState: StateFlow<AutoParseUIState> = _autoParseState.asStateFlow()

    private val _aiAdviceState = MutableStateFlow<AiAdviceUIState>(AiAdviceUIState.Idle)
    val aiAdviceState: StateFlow<AiAdviceUIState> = _aiAdviceState.asStateFlow()

    init {
        viewModelScope.launch {
            // First prepopulate defaults if DB is fresh
            repository.checkAndPrepopulateData()
            // Set first member as active by default or wait for flows
            _selectedMember.value = repository.members.firstOrNull()?.firstOrNull()
        }
    }

    fun selectMember(member: FamilyMember) {
        _selectedMember.value = member
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _cloudSyncState.value = SyncState.Syncing
            kotlinx.coroutines.delay(1500) // Realistic networks delays
            val lastSyncText = "Tersinkronisasi 100% antara ${members.value.size} anggota keluarga (Cloud Sejahtera)"
            _cloudSyncState.value = SyncState.Synced(lastSyncText)
        }
    }

    // --- CRUD Actions ---
    fun addMember(name: String, role: String) {
        viewModelScope.launch {
            repository.insertMember(FamilyMember(name = name, role = role))
            // Re-select if null
            if (_selectedMember.value == null) {
                _selectedMember.value = FamilyMember(name = name, role = role)
            }
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = members.value.firstOrNull { it.id != member.id }
            }
        }
    }

    fun addBudget(category: String, limit: Double) {
        viewModelScope.launch {
            // Just replace/insert
            repository.insertBudget(Budget(category = category, limitAmount = limit, spentAmount = 0.0))
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun insertManualTransaction(
        amount: Double,
        category: String,
        merchant: String,
        type: String
    ) {
        viewModelScope.launch {
            val activeMember = _selectedMember.value ?: FamilyMember(name = "Keluarga", role = "Anggota")
            val newTx = Transaction(
                familyMemberId = activeMember.id,
                memberName = activeMember.name,
                amount = amount,
                category = category,
                merchant = merchant,
                type = type,
                isAutomatic = false
            )
            repository.addTransaction(newTx)
            triggerCloudSync() // Auto-simulate synchronization instantly!
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            triggerCloudSync()
        }
    }

    fun addInvestment(name: String, type: String, invested: Double, currentValue: Double) {
        viewModelScope.launch {
            repository.insertInvestment(
                Investment(name = name, type = type, amountInvested = invested, currentValue = currentValue)
            )
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    fun saveRetirementConfig(
        currentAge: Int,
        retirementAge: Int,
        monthlyNeed: Double,
        inflation: Double,
        investmentReturn: Double,
        annualSave: Double
    ) {
        viewModelScope.launch {
            repository.saveRetirementPlan(
                RetirementPlan(
                    currentAge = currentAge,
                    retirementAge = retirementAge,
                    monthlyNeedRetirement = monthlyNeed,
                    inflationRatePercent = inflation,
                    investmentReturnPercent = investmentReturn,
                    annualSavingContribution = annualSave
                )
            )
        }
    }

    // --- Automatic Receipt Parser via Gemini API ---
    fun parseReceiptAutomatically(rawText: String) {
        if (rawText.isBlank()) return
        
        viewModelScope.launch {
            _autoParseState.value = AutoParseUIState.Loading
            val parsed = GeminiService.parseReceipt(rawText)
            
            if (parsed != null) {
                // If parsing succeeded, insert it automatically!
                val activeMember = _selectedMember.value ?: FamilyMember(name = "Keluarga", role = "Anggota")
                val autoTx = Transaction(
                    familyMemberId = activeMember.id,
                    memberName = activeMember.name,
                    amount = parsed.amount,
                    category = parsed.category,
                    merchant = parsed.merchant,
                    type = parsed.type,
                    isAutomatic = true,
                    rawTextSource = rawText
                )
                repository.addTransaction(autoTx)
                _autoParseState.value = AutoParseUIState.Success(autoTx)
                triggerCloudSync() // Instantly sync this automatic entry
            } else {
                _autoParseState.value = AutoParseUIState.Error("Gagal menganalisis otomatis. Pastikan API Key diatur atau ketik manual.")
            }
        }
    }

    fun resetAutoParseState() {
        _autoParseState.value = AutoParseUIState.Idle
    }

    // --- AI Investment & Retirement Consultation Advisor ---
    fun loadFamilyAiAdvisorAdvice() {
        viewModelScope.launch {
            _aiAdviceState.value = AiAdviceUIState.Loading
            val advice = GeminiService.getFinancialAdvice(
                familyMembers = members.value,
                budgets = budgets.value,
                transactions = transactions.value,
                investments = investments.value,
                retirementPlan = retirementPlan.value
            )
            _aiAdviceState.value = AiAdviceUIState.Content(advice)
        }
    }
}

// Sealed interfaces for UI states
sealed interface SyncState {
    object Syncing : SyncState
    data class Synced(val message: String) : SyncState
}

sealed interface AutoParseUIState {
    object Idle : AutoParseUIState
    object Loading : AutoParseUIState
    data class Success(val transaction: Transaction) : AutoParseUIState
    data class Error(val errorMsg: String) : AutoParseUIState
}

sealed interface AiAdviceUIState {
    object Idle : AiAdviceUIState
    object Loading : AiAdviceUIState
    data class Content(val markdownText: String) : AiAdviceUIState
}

// ViewModel Factory
class FamilyViewModelFactory(
    private val application: Application,
    private val repository: FamilyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
