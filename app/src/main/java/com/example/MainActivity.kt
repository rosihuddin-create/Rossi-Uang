package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Formatter for Indonesian Rupiah
fun Double.toRupiah(): String {
    val localeId = Locale("id", "ID")
    val formatter = NumberFormat.getCurrencyInstance(localeId)
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core dependency initialization
        val database = FamilyDatabase.getDatabase(applicationContext)
        val repository = FamilyRepository(database.familyDao)
        val viewModel: FamilyViewModel by viewModels {
            FamilyViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { /* Implemented inside Content main container for better layout */ }
                ) { innerPadding ->
                    FamilyAppContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Dasbor", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BUDGETS("Anggaran", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    AUTOTRACK("Lacak AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    INVESTMENT("Pensiun & Investasi", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp)
}

@Composable
fun FamilyAppContent(viewModel: FamilyViewModel, modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Collect variables
    val members by viewModel.members.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val retirementPlan by viewModel.retirementPlan.collectAsStateWithLifecycle()
    
    val selectedMember by viewModel.selectedMember.collectAsStateWithLifecycle()
    val syncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showEditRetirementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. TOP HEADER & CLOUD SYNC CONTROLLER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Keluarga Wijaya",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Dashboard Keuangan",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cloud Sync Pill Indicator
                        SyncPill(
                            syncState = syncState,
                            onSyncRequest = { viewModel.triggerCloudSync() }
                        )

                        // Elegant "KW" avatar on top-right
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { /* Tap family profile */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KW",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Family Members Avatar Switching Row
                Text(
                    text = "Anggota Keluarga Aktif:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Predefined dynamic badges
                    members.forEach { m ->
                        val isActive = selectedMember?.id == m.id
                        Box(
                            modifier = Modifier
                                .testTag("member_chip_${m.id}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.selectMember(m) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val avatarIcon = when (m.role.lowercase()) {
                                    "ayah" -> Icons.Default.Face
                                    "ibu" -> Icons.Default.FaceRetouchingNatural
                                    else -> Icons.Default.ChildCare
                                }
                                Icon(
                                    imageVector = avatarIcon,
                                    contentDescription = m.role,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = m.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = m.role,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Add new family member button
                    IconButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier
                            .testTag("add_member_button")
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Anggota",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. MAIN ACTIVE WINDOW CONTENT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    members = members,
                    budgets = budgets,
                    transactions = transactions,
                    investments = investments,
                    onAddTxRequested = { showAddTxDialog = true }
                )
                AppTab.BUDGETS -> BudgetsScreen(
                    budgets = budgets,
                    transactions = transactions,
                    onAddBudget = { showAddBudgetDialog = true },
                    onDeleteBudget = { viewModel.deleteBudget(it) }
                )
                AppTab.AUTOTRACK -> AutoTrackReceiptScreen(
                    viewModel = viewModel
                )
                AppTab.INVESTMENT -> InvestmentAndRetirementScreen(
                    investments = investments,
                    retirementPlan = retirementPlan,
                    onAddInvestment = { showAddInvestmentDialog = true },
                    onDeleteInvestment = { viewModel.deleteInvestment(it) },
                    onEditRetirement = { showEditRetirementDialog = true },
                    viewModel = viewModel
                )
            }
        }

        // 3. BOTTOM TAB NAVIGATION BAR
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { currentTab = tab },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                )
            }
        }
    }

    // --- MODAL DIALOGS ---

    // A. Add Member Dialog
    if (showAddMemberDialog) {
        var nameInput by remember { mutableStateOf("") }
        var roleInput by remember { mutableStateOf("Ayah") }
        val roles = listOf("Ayah", "Ibu", "Anak", "Kakek", "Nenek")

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Tambah Anggota Keluarga") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth().testTag("add_member_name_input"),
                        singleLine = true
                    )
                    Text("Peran Keluarga:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roles.forEach { r ->
                            FilterChip(
                                selected = roleInput == r,
                                onClick = { roleInput = r },
                                label = { Text(r) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.addMember(nameInput, roleInput)
                            showAddMemberDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_member_dialog")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Batal") }
            }
        )
    }

    // B. Add Budget Dialog
    if (showAddBudgetDialog) {
        var categoryInput by remember { mutableStateOf("") }
        var limitInput by remember { mutableStateOf("") }
        val categories = listOf("Makanan", "Transportasi", "Pendidikan", "Hiburan", "Investasi", "Kesehatan", "Lainnya")

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = { Text("Buat Penganggaran Baru (Budget)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kategori Anggaran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Kategori Kustom (Bila tidak ada di atas)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Batas Anggaran Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_budget_limit_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = limitInput.toDoubleOrNull() ?: 0.0
                        if (categoryInput.isNotBlank() && limitVal > 0) {
                            viewModel.addBudget(categoryInput, limitVal)
                            showAddBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_budget_dialog")
                ) {
                    Text("Simpan Anggaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) { Text("Batal") }
            }
        )
    }

    // C. Add Manual Transaction Dialog (Pencatatan Keuangan Manual)
    if (showAddTxDialog) {
        var amountInput by remember { mutableStateOf("") }
        var merchantInput by remember { mutableStateOf("") }
        var categoryInput by remember { mutableStateOf("Makanan") }
        var typeInput by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME

        val categories = budgets.map { it.category }.ifEmpty { listOf("Makanan", "Transportasi", "Pendidikan", "Hiburan", "Lainnya") }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text("Catat Transaksi Finansial") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // EXPENSE VS INCOME Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (typeInput == "EXPENSE") MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { typeInput = "EXPENSE" }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PENGELUARAN", fontWeight = FontWeight.Bold, color = if (typeInput == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (typeInput == "INCOME") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { typeInput = "INCOME" }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PEMASUKAN", fontWeight = FontWeight.Bold, color = if (typeInput == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Nominal Transaksi (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_tx_amount_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        label = { Text(if (typeInput == "EXPENSE") "Merchant / Tempat Belanja" else "Sumber Dana") },
                        modifier = Modifier.fillMaxWidth().testTag("add_tx_merchant_input"),
                        singleLine = true
                    )

                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    if (selectedMember != null) {
                        Text(
                            text = "Dicatat oleh: ${selectedMember!!.name} (${selectedMember!!.role})",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amtVal = amountInput.toDoubleOrNull() ?: 0.0
                        if (amtVal > 0 && merchantInput.isNotBlank()) {
                            viewModel.insertManualTransaction(amtVal, categoryInput, merchantInput, typeInput)
                            showAddTxDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_tx_dialog")
                ) {
                    Text("Proses Transaksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) { Text("Batal") }
            }
        )
    }

    // D. Add Investment Asset Dialog
    if (showAddInvestmentDialog) {
        var nameInput by remember { mutableStateOf("") }
        var typeInput by remember { mutableStateOf("Saham") }
        var investedInput by remember { mutableStateOf("") }
        var currentInput by remember { mutableStateOf("") }

        val investmentTypes = listOf("Saham", "Reksadana", "Emas", "Obligasi", "Properti", "Deposito")

        AlertDialog(
            onDismissRequest = { showAddInvestmentDialog = false },
            title = { Text("Tambah Aset Investasi Baru") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Penempatan (Contoh: Obligasi Negara)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Jenis Investasi:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        investmentTypes.forEach { type ->
                            FilterChip(
                                selected = typeInput == type,
                                onClick = { typeInput = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = investedInput,
                        onValueChange = { investedInput = it },
                        label = { Text("Modal Pembelian Awal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        label = { Text("Nilai Valuasi Saat Ini (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mCapital = investedInput.toDoubleOrNull() ?: 0.0
                        val mCurrent = currentInput.toDoubleOrNull() ?: mCapital
                        if (nameInput.isNotBlank() && mCapital > 0.0) {
                            viewModel.addInvestment(nameInput, typeInput, mCapital, mCurrent)
                            showAddInvestmentDialog = false
                        }
                    }
                ) {
                    Text("Tambahkan Portfolio")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInvestmentDialog = false }) { Text("Batal") }
            }
        )
    }

    // E. Edit Retirement Goals
    if (showEditRetirementDialog) {
        val currentPlan = retirementPlan ?: RetirementPlan()
        var curAge by remember { mutableStateOf(currentPlan.currentAge.toString()) }
        var retAge by remember { mutableStateOf(currentPlan.retirementAge.toString()) }
        var monNeed by remember { mutableStateOf(currentPlan.monthlyNeedRetirement.toString()) }
        var infRate by remember { mutableStateOf(currentPlan.inflationRatePercent.toString()) }
        var yldExpected by remember { mutableStateOf(currentPlan.investmentReturnPercent.toString()) }
        var annContrib by remember { mutableStateOf(currentPlan.annualSavingContribution.toString()) }

        AlertDialog(
            onDismissRequest = { showEditRetirementDialog = false },
            title = { Text("Konfigurasi Rencana Pensiun") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = curAge,
                            onValueChange = { curAge = it },
                            label = { Text("Usia Sekarang") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = retAge,
                            onValueChange = { retAge = it },
                            label = { Text("Usia Pensiun") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = monNeed,
                        onValueChange = { monNeed = it },
                        label = { Text("Estimasi Pengeluaran Pensiun (/bulan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = infRate,
                            onValueChange = { infRate = it },
                            label = { Text("Rasio Inflasi (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = yldExpected,
                            onValueChange = { yldExpected = it },
                            label = { Text("Estimasi Return (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = annContrib,
                        onValueChange = { annContrib = it },
                        label = { Text("Kemampuan Menabung (/tahun)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveRetirementConfig(
                            currentAge = curAge.toIntOrNull() ?: 35,
                            retirementAge = retAge.toIntOrNull() ?: 55,
                            monthlyNeed = monNeed.toDoubleOrNull() ?: 10000000.0,
                            inflation = infRate.toDoubleOrNull() ?: 4.0,
                            investmentReturn = yldExpected.toDoubleOrNull() ?: 8.0,
                            annualSave = annContrib.toDoubleOrNull() ?: 12000000.0
                        )
                        showEditRetirementDialog = false
                    }
                ) {
                    Text("Update Sasaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRetirementDialog = false }) { Text("Batal") }
            }
        )
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun SyncPill(syncState: SyncState, onSyncRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .testTag("sync_controller")
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onSyncRequest() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (syncState) {
            is SyncState.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Sinkron...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            is SyncState.Synced -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )
                Text(
                    text = "Sinkron Aktif",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Donut Investment Allocation Custom Canvas Chart
@Composable
fun InvestmentDonutChart(investments: List<Investment>, modifier: Modifier = Modifier) {
    val totalInvested = investments.sumOf { it.currentValue }
    if (totalInvested == 0.0) return

    val grouped = investments.groupBy { it.type }
    val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                grouped.entries.forEachIndexed { idx, entry ->
                    val value = entry.value.sumOf { it.currentValue }
                    val sweep = (value / totalInvested * 360).toFloat()
                    val color = colors[idx % colors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Dana",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = totalInvested.toRupiah().replace("Rp", "Rp\n").trim(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.entries.forEachIndexed { idx, entry ->
                val valSum = entry.value.sumOf { it.currentValue }
                val pct = (valSum / totalInvested) * 100
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[idx % colors.size], CircleShape)
                    )
                    Text(
                        text = "${entry.key} (${String.format("%.1f", pct)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- 4 SCREENS UNDER TAB LAYOUT ---

// TAB A: FAMILY SUMMARY DASHBOARD SCREEN
@Composable
fun DashboardScreen(
    members: List<FamilyMember>,
    budgets: List<Budget>,
    transactions: List<Transaction>,
    investments: List<Investment>,
    onAddTxRequested: () -> Unit
) {
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val familyBalance = totalIncome - totalExpense

    val totalBudgetLimits = budgets.sumOf { it.limitAmount }
    val totalBudgetSpent = budgets.sumOf { it.spentAmount }

    val totalInvestments = investments.sumOf { it.currentValue }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Banner Card with Gradient Backing to avoid generic AI slop
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    // Overlapping subtle circle background decorative pattern
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = 120.dp.toPx(),
                            center = Offset(size.width + 40.dp.toPx(), -30.dp.toPx())
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Total Kekayaan Bersama",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC7D2FE) // indigo-200
                                )
                                Text(
                                    text = familyBalance.toRupiah(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Green Gain Badge (+4.2%)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4ADE80), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+4.2%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF022C22) // green-950
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row of overlapping avatar placeholders
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                val avatarColors = listOf(
                                    Color(0xFF6366F1), // Indigo 500
                                    Color(0xFF818CF8), // Indigo 400
                                    Color(0xFFA5B4FC)  // Indigo 300
                                )
                                repeat(3) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(avatarColors[i % avatarColors.size], CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Sinkronisasi terakhir: baru saja",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC7D2FE) // indigo-200
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Grid from Professional Polish design
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Pengeluaran (rose-600)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pengeluaran",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = totalExpense.toRupiah(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48), // rose-600
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress bar indicator
                        val spentRatio = if (totalBudgetLimits > 0) (totalBudgetSpent / totalBudgetLimits).toFloat().coerceIn(0f, 1f) else 0.75f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(spentRatio)
                                    .fillMaxHeight()
                                    .background(Color(0xFFF43F5E), RoundedCornerShape(100.dp)) // rose-500
                            )
                        }
                    }
                }

                // Card 2: Budget Sisa (indigo-600)
                val budgetSisa = (totalBudgetLimits - totalBudgetSpent).coerceAtLeast(0.0)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Budget Sisa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = budgetSisa.toRupiah(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, // indigo-600
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress bar indicator
                        val sisaRatio = if (totalBudgetLimits > 0) ((totalBudgetLimits - totalBudgetSpent) / totalBudgetLimits).toFloat().coerceIn(0f, 1f) else 0.33f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sisaRatio)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp)) // indigo-500
                            )
                        }
                    }
                }
            }
        }

        // Add Quick Action row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddTxRequested,
                    modifier = Modifier
                        .testTag("record_transaction_quick")
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Manual", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Investment Ring Graph Breakdown
        if (investments.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Distribusi Portfolio Investasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Alokasi aset keluarga produktif", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))
                        InvestmentDonutChart(investments = investments)
                    }
                }
            }
        }

        // Recent Family Transactions Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Aktivitas Keuangan Terbaru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada pencatatan apa pun.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(transactions.take(8)) { tx ->
                val isExpense = tx.type == "EXPENSE"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isExpense) Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f) ,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = "",
                                    tint = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.memberName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• ${tx.category}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (tx.isAutomatic) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("AI Lacak", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = if (isExpense) "- ${tx.amount.toRupiah()}" else "+ ${tx.amount.toRupiah()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

// TAB B: DETAILED BUDGETS LIST SCREEN
@Composable
fun BudgetsScreen(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    onAddBudget: () -> Unit,
    onDeleteBudget: (Budget) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Anggaran Bulanan (Satu Keluarga)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Kendalikan dan pantau batas pengeluaran kolektif", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                
                Button(
                    onClick = onAddBudget,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Buat", fontSize = 12.sp)
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada anggaran terdefinisi. Silakan tambahkan satu.")
                }
            }
        } else {
            items(budgets) { budget ->
                val ratio = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount) else 0.0
                val ratioPct = (ratio * 100).toInt()
                val progressColor = when {
                    ratio >= 1.0 -> Color(0xFFEF4444) // Overlimit Red
                    ratio >= 0.75 -> Color(0xFFF59E0B) // Warn Orange
                    else -> Color(0xFF10B981) // Safe Emerald Green
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(progressColor, CircleShape)
                                )
                                Text(
                                    text = budget.category,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$ratioPct% terpakai",
                                    fontSize = 11.sp,
                                    color = progressColor,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { onDeleteBudget(budget) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Anggaran",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = ratio.coerceAtMost(1.0).toFloat(),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terbelanja: ${budget.spentAmount.toRupiah()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Batas Limit: ${budget.limitAmount.toRupiah()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Category specific transaction links
                        val matchingTxs = transactions.filter { it.category == budget.category && it.type == "EXPENSE" }
                        if (matchingTxs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Transaksi di kategori ini:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            matchingTxs.take(3).forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${tx.memberName} (${tx.merchant})",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = tx.amount.toRupiah(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB C: AUTOMATIC EXPENSE TRACKER VIA SMS COPY-PASTE SCREEN (GEMINI DRIVEN)
@Composable
fun AutoTrackReceiptScreen(viewModel: FamilyViewModel) {
    val autoParseState by viewModel.autoParseState.collectAsStateWithLifecycle()
    var rawText by remember { mutableStateOf("") }

    // Dummy examples of Indonesian SMS Banking texts
    val examples = listOf(
        "SMS Bank BCA: Transaksi sukses di XXI senilai Rp250.000 dengan QRIS.",
        "GO-PAY: Pembayaran sukses ke GrabFood sebesar Rp125.000 atas nama keluarga Anda.",
        "Alert Pertamina: Pembayaran bensin Pertamax Rp450.000 berhasil didebit dari kartu kredit.",
        "Transfer BNI Mandiri: Uang masuk gaji Rp18.500.000 dari PT Makmur Abadi sukses."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("auto_track_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "Pelacakan Pengeluaran Otomatis (Asisten AI)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Salin & tempel teks SMS Banking, pesan notifikasi bank atau kuitansi di bawah ini. AI Gemini akan menganalisis merchant, nominal, tipe, kategori yang tepat, serta langsung mencatatnya!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Paste Board
        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            placeholder = { Text("Tempel SMS atau notifikasi transfer di sini...", fontSize = 12.sp) },
            label = { Text("Teks Transaksi Mentah") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("auto_track_input"),
            maxLines = 4
        )

        // Try Examples chips
        Text(
            text = "Ketuk contoh teks cepat untuk uji coba langsung:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            examples.forEach { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { rawText = item }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
            }
        }

        // Submit Process Button
        Button(
            onClick = {
                viewModel.parseReceiptAutomatically(rawText)
            },
            modifier = Modifier
                .testTag("submit_auto_track_button")
                .fillMaxWidth()
                .height(48.dp),
            enabled = rawText.isNotBlank() && autoParseState != AutoParseUIState.Loading
        ) {
            if (autoParseState == AutoParseUIState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Asisten AI Menganalisis...", fontSize = 14.sp)
            } else {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekstrak & Catat Otomatis", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Output Status display
        AnimatedVisibility(
            visible = autoParseState != AutoParseUIState.Idle,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            when (val state = autoParseState) {
                is AutoParseUIState.Loading -> { } // Handled inside button
                is AutoParseUIState.Success -> {
                    val tx = state.transaction
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, "Sukses", tint = Color(0xFF10B981))
                                Text("Berhasil Dicatat Otomatis!", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Merchant: ${tx.merchant}", fontSize = 13.sp)
                            Text("Nominal: ${tx.amount.toRupiah()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Kategori terpilih: ${tx.category}", fontSize = 13.sp)
                            Text("Jenis transaksi: ${tx.type}", fontSize = 13.sp)
                            Text("Diinput untuk keluarga aktif saat ini.", fontSize = 10.sp, fontStyle = FontStyle.Italic)

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    rawText = ""
                                    viewModel.resetAutoParseState()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Lacak Pesan Baru", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
                is AutoParseUIState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Error, "Sistem Gagal", tint = MaterialTheme.colorScheme.error)
                                Text("Gagal Ekstrak AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.errorMsg, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetAutoParseState() }
                            ) {
                                Text("Coba Lagi", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

// TAB D: RETIREMENT CALCULATOR & INVESTMENTS SCREEN
@Composable
fun InvestmentAndRetirementScreen(
    investments: List<Investment>,
    retirementPlan: RetirementPlan?,
    onAddInvestment: () -> Unit,
    onDeleteInvestment: (Investment) -> Unit,
    onEditRetirement: () -> Unit,
    viewModel: FamilyViewModel
) {
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()
    val plan = retirementPlan ?: RetirementPlan()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("investments_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RETIREMENT (RENCANA PENSIUN) SECTION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Kalkulator Rencana Pensiun Mandiri", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Persiapan hari tua sejahtera keluarga", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(
                        onClick = onEditRetirement,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Rencana Pensiun", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Key metrics of retirement
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usia Sekarang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("${plan.currentAge} Tahun", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Pensiun", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("${plan.retirementAge} Tahun", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Masa Kerja Sisa", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        val sisa = (plan.retirementAge - plan.currentAge).coerceAtLeast(0)
                        Text("$sisa Tahun Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sasaran Bulanan Saat Pensiun", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(plan.monthlyNeedRetirement.toRupiah(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rencana Tabungan/Investasi Setahun", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(plan.annualSavingContribution.toRupiah(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Pure Kotlin compounding formula to show target amount to accumulate inside Java
                val yearsToRetire = (plan.retirementAge - plan.currentAge).coerceAtLeast(0)
                
                // Formulate inflation effect on spending
                // inflatedMonthlyNeed = monthlyNeed * (1 + inflation/100)^yearsToRetire
                val inflationFactor = Math.pow(1 + (plan.inflationRatePercent / 100.0), yearsToRetire.toDouble())
                val inflatedMonthlyNeed = plan.monthlyNeedRetirement * inflationFactor
                
                // Formulate required retirement capital assuming saving draw-down of 4% rule (divided by 0.04) or annuity
                // capitalizationRequired = inflatedMonthlyNeed * 12 / 0.04 (conservative 4% rule)
                val capitalizationRequired = (inflatedMonthlyNeed * 12) / 0.04

                // Estimated Future Portfolio based on compounded savings
                // futureVal = annualContrib * (((1 + return/100)^years - 1) / (return/100))
                val rate = plan.investmentReturnPercent / 100.0
                val futureValValue = if (rate > 0) {
                    plan.annualSavingContribution * ((Math.pow(1 + rate, yearsToRetire.toDouble()) - 1) / rate)
                } else {
                    plan.annualSavingContribution * yearsToRetire
                }

                val gap = capitalizationRequired - futureValValue

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Proyeksi Finansial Setelah Masa Kerja:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "• Nilai Pengeluaran Efektif Inflasi: ${inflatedMonthlyNeed.toRupiah()}/bulan",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Total Kapital Kerja Hari Tua yang Dibutuhkan: ${capitalizationRequired.toRupiah()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• Proyeksi Hasil Tabungan Masa Pensiun: ${futureValValue.toRupiah()}",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (gap <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "MANTAP! Akumulasi investasi Anda diproyeksikan melebihi kapital yang dibutuhkan. Pertahankan tabungan tahunan Anda!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "MINUS TARGET: Kekurangan dana sebesar ${gap.toRupiah()}. Butuh optimasi alokasi asset atau naikkan tabungan tahunan Anda.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        // INVESTMENTS LIST SECTION CONTAINER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Daftar Aset Investasi Keluarga", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Semua instrumen pertumbuhan dana aktif", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Button(onClick = onAddInvestment) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aset", fontSize = 11.sp)
            }
        }

        if (investments.isEmpty()) {
            Text(
                "Belum ada aset investasi terdaftar.",
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            investments.forEach { inv ->
                val profit = inv.currentValue - inv.amountInvested
                val roi = if (inv.amountInvested > 0) (profit / inv.amountInvested * 100) else 0.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            val invIcon = when (inv.type.lowercase()) {
                                "saham" -> Icons.Default.TrendingUp
                                "emas" -> Icons.Default.Savings
                                else -> Icons.Default.CorporateFare
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = invIcon, contentDescription = "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(inv.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Aset: ${inv.type} • Modal: ${inv.amountInvested.toRupiah()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(inv.currentValue.toRupiah(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (profit >= 0) "+${roi.toInt()}%" else "${roi.toInt()}%",
                                color = if (profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(onClick = { onDeleteInvestment(inv) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // AI ASSISTANT ADVICE BUTTON
        Text(
            text = "Konsultasi Finansial Ahli AI",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Button(
            onClick = { viewModel.loadFamilyAiAdvisorAdvice() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .testTag("ai_advice_button")
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Analisis Strategi Finansial Keluarga (Gemini)", fontWeight = FontWeight.Bold)
        }

        // Display recommendation advice result
        AnimatedVisibility(
            visible = aiAdviceState != AiAdviceUIState.Idle,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.primary)
                        Text("Rekomendasi Ahli Keuangan AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = aiAdviceState) {
                        is AiAdviceUIState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Sedang menyusun laporan finansial komparatif...", fontSize = 12.sp, fontStyle = FontStyle.Italic)
                            }
                        }
                        is AiAdviceUIState.Content -> {
                            // Render simple custom markdown elements inside Compose to ensure beautiful readable alignment
                            val paragraphs = state.markdownText.split("\n")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                paragraphs.forEach { paragraph ->
                                    if (paragraph.startsWith("##")) {
                                        Text(
                                            text = paragraph.replace("##", "").trim(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        )
                                    } else if (paragraph.startsWith("-") || paragraph.startsWith("*")) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = paragraph.substring(1).trim(),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    } else if (paragraph.isNotBlank()) {
                                        Text(
                                            text = paragraph.trim(),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
