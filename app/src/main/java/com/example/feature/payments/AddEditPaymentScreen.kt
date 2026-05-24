package com.example.feature.payments

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateUtils
import com.example.domain.model.Category
import com.example.domain.model.PaymentMethod
import com.example.feature.dashboard.parseColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import com.example.ui.theme.*
import com.example.ui.viewmodel.AddPaymentViewModel
import com.example.ui.viewmodel.DueMateViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddEditPaymentScreen(
    viewModel: DueMateViewModel,
    subscriptionId: Int, // 0 for new
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    // Create our dedicated Form State ViewModel
    val addViewModel: AddPaymentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.AddPaymentViewModelFactory(context)
    )
    val state by addViewModel.uiState.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    val isVietnamese = settingsState?.language == "vi"

    // Load initial values if in Edit Mode or setDefault settings
    LaunchedEffect(subscriptionId, categories) {
        if (categories.isNotEmpty()) {
            if (subscriptionId > 0) {
                addViewModel.loadSubscription(subscriptionId, categories)
            } else {
                addViewModel.initDefaultsFromSettings()
                // Auto choose first category if none stands
                if (state.selectedCategoryName.isEmpty()) {
                    addViewModel.onCategorySelected(categories.first())
                }
            }
        }
    }

    // Confirmation dialog when backing out with changes
    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    // Check if form has modified changes
    val hasChanges = remember(state, subscriptionId) {
        if (subscriptionId > 0) {
            // Edit mode checks (basically if user edited fields we see differences)
            state.name.isNotEmpty() || state.amountText.isNotEmpty()
        } else {
            state.name.isNotEmpty() || state.amountText.isNotEmpty()
        }
    }

    val onBackRequest = {
        if (hasChanges) {
            showExitConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        onBackRequest()
    }

    // Success Listener
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            val feedback = if (subscriptionId > 0) {
                if (isVietnamese) "Khoản mục đã cập nhật!" else "Due updated successfully"
            } else {
                if (isVietnamese) "Khoản mục đã được lưu thành công!" else "Due added successfully"
            }
            android.widget.Toast.makeText(context, feedback, android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Dropdown expanded states
    var categoryExpanded by remember { mutableStateOf(false) }
    var billingCycleExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var importanceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    // Custom creation states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddPaymentMethodDialog by remember { mutableStateOf(false) }

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("📦") }
    var newCategoryColor by remember { mutableStateOf("#00BCD4") }
    var categoryDialogError by remember { mutableStateOf<String?>(null) }

    var newPaymentMethodName by remember { mutableStateOf("") }
    var newPaymentMethodType by remember { mutableStateOf("Other") }
    var newPaymentMethodLastFour by remember { mutableStateOf("") }
    var newPaymentMethodNote by remember { mutableStateOf("") }
    var paymentMethodDialogError by remember { mutableStateOf<String?>(null) }

    val colors = LocalKotNestColors.current

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text(if (isVietnamese) "Hủy bỏ thay đổi?" else "Discard changes?") },
            text = { Text(if (isVietnamese) "Các thay đổi vẫn chưa được lưu lại. Bạn có chắc chắn muốn thoát khỏi đây?" else "You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmationDialog = false
                    onNavigateBack()
                }) {
                    Text(if (isVietnamese) "Thoát" else "Discard", color = colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmationDialog = false }) {
                    Text(if (isVietnamese) "Tiếp tục" else "Keep writing", color = colors.primaryText)
                }
            },
            containerColor = colors.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (subscriptionId > 0) {
                            if (isVietnamese) "Chỉnh sửa khoản mục" else "Edit Dues Connection"
                        } else {
                            if (isVietnamese) "Thêm khoản mục mới" else "Add New Dues"
                        }, 
                        fontWeight = FontWeight.Bold, 
                        color = colors.primaryText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundGradientStart
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(colors.backgroundGradientEnd)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { addViewModel.saveDue(subscriptionId) },
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.deepAqua,
                        contentColor = Color.White,
                        disabledContainerColor = colors.border,
                        disabledContentColor = colors.secondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("form_submit_button")
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (subscriptionId > 0) {
                                if (isVietnamese) "Cập nhật" else "Save Changes"
                            } else {
                                if (isVietnamese) "Thêm khoản chi" else "Add Subscription"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Error Message Banner if validation failed globally
            state.errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.danger.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // GROUP 1: BASIC INFORMATION
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroupHeader(if (isVietnamese) "THÔNG TIN CƠ BẢN" else "BASIC INFORMATION")
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { addViewModel.onNameChange(it) },
                        label = { Text(if (isVietnamese) "Tên khoản mục *" else "Details / Subscription Name *") },
                        isError = state.fieldErrors.containsKey("name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText,
                            focusedLabelColor = colors.primaryAqua,
                            unfocusedLabelColor = colors.mutedText,
                            focusedBorderColor = colors.primaryAqua,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.primaryAqua,
                            errorBorderColor = colors.danger
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_name_input"),
                        singleLine = true
                    )
                    if (state.fieldErrors.containsKey("name")) {
                        Text(
                            text = state.fieldErrors["name"] ?: "",
                            color = colors.danger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    // Smart Suggestions
                    state.currentSuggestion?.let { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("smart_suggestion_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.primaryAqua.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, colors.primaryAqua.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (isVietnamese) "Gợi ý thông minh" else "Smart Suggestion",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryAqua
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${(suggestion.confidence * 100).toInt()}% match",
                                        fontSize = 11.sp,
                                        color = colors.mutedText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                Text(
                                    text = if (isVietnamese) {
                                        "Lập danh mục: ${suggestion.suggestedCategoryName} • Chu kỳ: ${suggestion.suggestedBillingCycle} • Độ quan trọng: ${suggestion.suggestedImportance}"
                                    } else {
                                        "Category: ${suggestion.suggestedCategoryName} • Cycle: ${suggestion.suggestedBillingCycle} • Importance: ${suggestion.suggestedImportance}"
                                    },
                                    fontSize = 12.sp,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = suggestion.explanationText,
                                    fontSize = 11.sp,
                                    color = colors.mutedText,
                                    lineHeight = 15.sp
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { addViewModel.dismissSuggestion() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = colors.mutedText)
                                    ) {
                                        Text(if (isVietnamese) "Bỏ qua" else "Dismiss", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { addViewModel.applySuggestion() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primaryAqua,
                                            contentColor = colors.deepAqua
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (isVietnamese) "Áp dụng" else "Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.8f)) {
                        OutlinedTextField(
                            value = state.amountText,
                            onValueChange = { addViewModel.onAmountChange(it) },
                            label = { Text(if (isVietnamese) "Số tiền *" else "Amount *") },
                            isError = state.fieldErrors.containsKey("amount"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedLabelColor = colors.primaryAqua,
                                unfocusedLabelColor = colors.mutedText,
                                focusedBorderColor = colors.primaryAqua,
                                unfocusedBorderColor = colors.border,
                                cursorColor = colors.primaryAqua,
                                errorBorderColor = colors.danger
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_amount_input"),
                            singleLine = true
                        )
                        if (state.fieldErrors.containsKey("amount")) {
                            Text(
                                text = state.fieldErrors["amount"] ?: "",
                                color = colors.danger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1.2f)) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = BorderStroke(1.dp, colors.border),
                            onClick = { currencyExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isVietnamese) "Loại tệ" else "Currency", 
                                            fontSize = 10.sp, 
                                            color = colors.secondaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = state.selectedCurrency, 
                                            fontSize = 15.sp, 
                                            fontWeight = FontWeight.Black, 
                                            color = colors.primaryText
                                        )
                                    }
                                    Text("▼", fontSize = 10.sp, color = colors.mutedText)
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            val currencies = listOf("VND", "USD", "EUR", "SGD", "AUD", "JPY", "KRW", "GBP", "CNY", "THB")
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, color = colors.primaryText, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        addViewModel.onCurrencySelected(curr)
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // GROUP 2: SCHEDULE & CYCLE
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroupHeader(if (isVietnamese) "LỊCH TRÌNH & CHU KỲ" else "SCHEDULE & CYCLE")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.border),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = state.nextDueDate }
                        val datePicker = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(year, month, dayOfMonth)
                                addViewModel.onDueDateSelected(selectedCal.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePicker.show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_date_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = if (colors.isLight) colors.deepAqua else colors.cyanAccent
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isVietnamese) "Ngày thanh toán tiếp theo *" else "Next Due Date *", 
                                fontSize = 11.sp, 
                                color = colors.secondaryText
                            )
                            Text(
                                text = DateUtils.formatEpoch(state.nextDueDate),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                        }
                    }
                }

                Box {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border),
                        onClick = { billingCycleExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isVietnamese) "Chu kỳ thanh toán *" else "Billing Cycle *", 
                                    fontSize = 11.sp, 
                                    color = colors.secondaryText
                                )
                                Text(
                                    text = if (isVietnamese) {
                                        when (state.selectedBillingCycle) {
                                            "One-time" -> "Một lần"
                                            "Weekly" -> "Hàng tuần"
                                            "Monthly" -> "Hàng tháng"
                                            "Yearly" -> "Hàng năm"
                                            "Every X days" -> "Mỗi X ngày"
                                            "Every X months" -> "Mỗi X tháng"
                                            else -> state.selectedBillingCycle
                                        }
                                    } else {
                                        state.selectedBillingCycle
                                    }, 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = colors.primaryText
                                )
                            }
                            Text("▼", fontSize = 12.sp, color = colors.mutedText)
                        }
                    }
                    DropdownMenu(
                        expanded = billingCycleExpanded,
                        onDismissRequest = { billingCycleExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        val cycles = listOf("One-time", "Weekly", "Monthly", "Yearly", "Every X days", "Every X months")
                        cycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = if (isVietnamese) {
                                            when (cycle) {
                                                "One-time" -> "Một lần"
                                                "Weekly" -> "Hàng tuần"
                                                "Monthly" -> "Hàng tháng"
                                                "Yearly" -> "Hàng năm"
                                                "Every X days" -> "Mỗi X ngày"
                                                "Every X months" -> "Mỗi X tháng"
                                                else -> cycle
                                            }
                                        } else {
                                            cycle
                                        }, 
                                        color = colors.primaryText,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    addViewModel.onBillingCycleSelected(cycle)
                                    billingCycleExpanded = false
                                }
                            )
                        }
                    }
                }

                if (state.selectedBillingCycle == "Every X days" || state.selectedBillingCycle == "Every X months") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.customCycleValueText,
                            onValueChange = { addViewModel.onCustomCycleValueChange(it) },
                            label = { 
                                Text(
                                    if (state.selectedBillingCycle == "Every X days") {
                                        if (isVietnamese) "Số ngày (X) *" else "Number of Days (X) *"
                                    } else {
                                        if (isVietnamese) "Số tháng (X) *" else "Number of Months (X) *"
                                    }
                                ) 
                            },
                            isError = state.fieldErrors.containsKey("customCycleValue"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedLabelColor = colors.primaryAqua,
                                unfocusedLabelColor = colors.mutedText,
                                focusedBorderColor = colors.primaryAqua,
                                unfocusedBorderColor = colors.border,
                                cursorColor = colors.primaryAqua,
                                errorBorderColor = colors.danger
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (state.fieldErrors.containsKey("customCycleValue")) {
                            Text(
                                text = state.fieldErrors["customCycleValue"] ?: "",
                                color = colors.danger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            // GROUP 3: CATEGORY & PAYMENT METHOD
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroupHeader(if (isVietnamese) "DANH MỤC & PHƯƠNG THỨC" else "CATEGORY & PAYMENT METHOD")

                Box {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border),
                        onClick = { categoryExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_due_category_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isVietnamese) "Danh mục *" else "Category *", 
                                    fontSize = 11.sp, 
                                    color = colors.secondaryText
                                )
                                val currentCat = categories.find { it.id == state.selectedCategoryId }
                                Text(
                                    text = currentCat?.let { "${it.icon} ${it.name}" } ?: (if (isVietnamese) "Chọn danh mục" else "Select Category"), 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = colors.primaryText
                                )
                            }
                            Text("▼", fontSize = 12.sp, color = colors.mutedText)
                        }
                    }
                }

                Box {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border),
                        onClick = { paymentMethodExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_due_payment_method_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isVietnamese) "Phương thức thanh toán (Tùy chọn)" else "Payment Method (Optional)", 
                                    fontSize = 11.sp, 
                                    color = colors.secondaryText
                                )
                                val currentMethod = paymentMethods.find { it.id == state.selectedPaymentMethodId }
                                Text(
                                    text = currentMethod?.let { 
                                        if (!it.lastFourDigits.isNullOrBlank()) "${it.name} (•••• ${it.lastFourDigits})" else it.name 
                                    } ?: (if (isVietnamese) "Không có" else "None"), 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = colors.primaryText
                                )
                            }
                            Text("▼", fontSize = 12.sp, color = colors.mutedText)
                        }
                    }
                }

            // MODAL BOTTOM SHEETS
            if (categoryExpanded) {
                ModalBottomSheet(
                    onDismissRequest = { categoryExpanded = false },
                    containerColor = colors.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = colors.mutedText) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = if (isVietnamese) "Chọn danh mục" else "Select Category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = { 
                                newCategoryName = ""
                                newCategoryIcon = "📦"
                                newCategoryColor = "#00BCD4"
                                categoryDialogError = null
                                showAddCategoryDialog = true 
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.deepAqua),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("btn_modal_add_category")
                        ) {
                            Text(if (isVietnamese) "➕ Thêm danh mục mới" else "➕ Add New Category", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = cat.id == state.selectedCategoryId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addViewModel.onCategorySelected(cat)
                                            categoryExpanded = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) colors.primaryAqua.copy(alpha = 0.15f) else colors.elevatedCard
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primaryAqua else colors.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(parseColor(cat.color).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(cat.icon, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = colors.primaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddCategoryDialog) {
                val emojis = listOf("☁️", "🎬", "🎓", "🖥️", "📶", "💼", "👤", "📦", "🍔", "🚗", "🛒", "💡", "💰", "🩺")
                val colorPalettes = listOf("#2196F3", "#E91E63", "#9C27B0", "#4CAF50", "#FF9800", "#00BCD4", "#607D8B", "#9E9E9E")

                AlertDialog(
                    onDismissRequest = { showAddCategoryDialog = false },
                    title = {
                        Text(
                            text = if (isVietnamese) "Tạo Danh Mục Mới" else "Create New Category",
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (categoryDialogError != null) {
                                Text(
                                    text = categoryDialogError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { 
                                    newCategoryName = it 
                                    categoryDialogError = null
                                },
                                label = { Text(if (isVietnamese) "Tên danh mục *" else "Category Name *") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText,
                                    focusedBorderColor = colors.primaryAqua,
                                    unfocusedBorderColor = colors.border
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_category_name")
                            )

                            Column {
                                Text(
                                    text = if (isVietnamese) "Chọn Biểu Tượng" else "Select Icon Iconography",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    emojis.forEach { emoji ->
                                        val isEmojiSelected = newCategoryIcon == emoji
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isEmojiSelected) colors.primaryAqua.copy(alpha = 0.3f) else colors.elevatedCard)
                                                .border(
                                                    width = if (isEmojiSelected) 2.dp else 1.dp,
                                                    color = if (isEmojiSelected) colors.primaryAqua else colors.border,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { newCategoryIcon = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = if (isVietnamese) "Chọn Màu Sắc" else "Select Aesthetic Color",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    colorPalettes.forEach { hexColor ->
                                        val isColorSelected = newCategoryColor == hexColor
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(parseColor(hexColor))
                                                .border(
                                                    width = if (isColorSelected) 3.dp else 0.dp,
                                                    color = colors.primaryText,
                                                    shape = CircleShape
                                                )
                                                .clickable { newCategoryColor = hexColor },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isColorSelected) {
                                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val trimmedName = newCategoryName.trim()
                                if (trimmedName.isEmpty()) {
                                    categoryDialogError = if (isVietnamese) "Vui lòng nhập tên danh mục" else "Please fill category name"
                                    return@Button
                                }
                                addViewModel.createCategory(trimmedName, newCategoryIcon, newCategoryColor) { success, errMsg ->
                                    if (success) {
                                        showAddCategoryDialog = false
                                        categoryExpanded = false
                                    } else {
                                        categoryDialogError = errMsg ?: (if (isVietnamese) "Tạo thất bại" else "Failed to create")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.deepAqua),
                            modifier = Modifier.testTag("btn_confirm_add_category")
                        ) {
                            Text(if (isVietnamese) "Lưu" else "Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text(if (isVietnamese) "Hủy" else "Cancel", color = colors.mutedText)
                        }
                    },
                    containerColor = colors.surface
                )
            }

            if (paymentMethodExpanded) {
                ModalBottomSheet(
                    onDismissRequest = { paymentMethodExpanded = false },
                    containerColor = colors.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = colors.mutedText) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = if (isVietnamese) "Chọn phương thức thanh toán" else "Select Payment Method",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                newPaymentMethodName = ""
                                newPaymentMethodType = "Other"
                                newPaymentMethodLastFour = ""
                                newPaymentMethodNote = ""
                                paymentMethodDialogError = null
                                showAddPaymentMethodDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.deepAqua),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("btn_modal_add_payment_method")
                        ) {
                            Text(if (isVietnamese) "➕ Thêm phương thức mới" else "➕ Add New Payment Method", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable {
                                    addViewModel.onPaymentMethodSelected(null)
                                    paymentMethodExpanded = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.selectedPaymentMethodId == null) colors.primaryAqua.copy(alpha = 0.15f) else colors.elevatedCard
                            ),
                            border = BorderStroke(
                                width = if (state.selectedPaymentMethodId == null) 2.dp else 1.dp,
                                color = if (state.selectedPaymentMethodId == null) colors.primaryAqua else colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.mutedText.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚫", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (isVietnamese) "Không sử dụng (Trống)" else "None (Optional)",
                                    fontSize = 16.sp,
                                    fontWeight = if (state.selectedPaymentMethodId == null) FontWeight.Bold else FontWeight.Medium,
                                    color = colors.primaryText
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                val isSelected = method.id == state.selectedPaymentMethodId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addViewModel.onPaymentMethodSelected(method)
                                            paymentMethodExpanded = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) colors.primaryAqua.copy(alpha = 0.15f) else colors.elevatedCard
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primaryAqua else colors.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.primaryAqua.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val icon = when (method.type) {
                                                "Card" -> "💳"
                                                "Cash" -> "💵"
                                                "Bank Transfer" -> "🏦"
                                                "PayPal" -> "🅿️"
                                                else -> "🪙"
                                            }
                                            Text(icon, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = method.name,
                                                fontSize = 16.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = colors.primaryText
                                            )
                                            val subtitle = buildString {
                                                append(method.type)
                                                if (!method.lastFourDigits.isNullOrBlank()) {
                                                    append(" •••• ")
                                                    append(method.lastFourDigits)
                                                }
                                            }
                                            Text(
                                                text = subtitle,
                                                fontSize = 12.sp,
                                                color = colors.secondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddPaymentMethodDialog) {
                val methodTypes = listOf("Card", "Cash", "Bank Transfer", "PayPal", "Other")

                AlertDialog(
                    onDismissRequest = { showAddPaymentMethodDialog = false },
                    title = {
                        Text(
                            text = if (isVietnamese) "Tạo Phương Thức Mới" else "Create Payment Method",
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (paymentMethodDialogError != null) {
                                Text(
                                    text = paymentMethodDialogError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = newPaymentMethodName,
                                onValueChange = { 
                                    newPaymentMethodName = it 
                                    paymentMethodDialogError = null
                                },
                                label = { Text(if (isVietnamese) "Tên phương thức *" else "Name *") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText,
                                    focusedBorderColor = colors.primaryAqua,
                                    unfocusedBorderColor = colors.border
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_payment_method_name")
                            )

                            Column {
                                Text(
                                    text = if (isVietnamese) "Loại phương thức" else "Method Type",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    methodTypes.forEach { mType ->
                                        val isSelected = newPaymentMethodType == mType
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) colors.primaryAqua.copy(alpha = 0.25f) else colors.elevatedCard)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) colors.primaryAqua else colors.border,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { newPaymentMethodType = mType }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(mType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newPaymentMethodLastFour,
                                onValueChange = { 
                                    if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                        newPaymentMethodLastFour = it
                                        paymentMethodDialogError = null
                                    }
                                },
                                label = { Text(if (isVietnamese) "4 số cuối thẻ (Tùy chọn)" else "Last 4 digits (Optional)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText,
                                    focusedBorderColor = colors.primaryAqua,
                                    unfocusedBorderColor = colors.border
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_payment_method_last_four")
                            )

                            OutlinedTextField(
                                value = newPaymentMethodNote,
                                onValueChange = { newPaymentMethodNote = it },
                                label = { Text(if (isVietnamese) "Ghi chú (Tùy chọn)" else "Note (Optional)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText,
                                    focusedLabelColor = colors.primaryAqua,
                                    unfocusedLabelColor = colors.mutedText,
                                    focusedBorderColor = colors.primaryAqua,
                                    unfocusedBorderColor = colors.border
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_payment_method_note")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val trimmedName = newPaymentMethodName.trim()
                                if (trimmedName.isEmpty()) {
                                    paymentMethodDialogError = if (isVietnamese) "Vui lòng nhập tên" else "Please fill payment method name"
                                    return@Button
                                }
                                if (newPaymentMethodLastFour.isNotEmpty() && newPaymentMethodLastFour.length != 4) {
                                    paymentMethodDialogError = if (isVietnamese) "4 số cuối thẻ phải đúng 4 chữ số" else "Last 4 digits must be exactly 4 digits"
                                    return@Button
                                }
                                addViewModel.createPaymentMethod(trimmedName, newPaymentMethodType, newPaymentMethodLastFour, newPaymentMethodNote) { success, errMsg ->
                                    if (success) {
                                        showAddPaymentMethodDialog = false
                                        paymentMethodExpanded = false
                                    } else {
                                        paymentMethodDialogError = errMsg ?: (if (isVietnamese) "Tạo thất bại" else "Failed to create")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.deepAqua),
                            modifier = Modifier.testTag("btn_confirm_add_payment_method")
                        ) {
                            Text(if (isVietnamese) "Lưu" else "Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPaymentMethodDialog = false }) {
                            Text(if (isVietnamese) "Hủy" else "Cancel", color = colors.mutedText)
                        }
                    },
                    containerColor = colors.surface
                )
            }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isVietnamese) "Tự động gia hạn" else "Auto-renew", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp, 
                            color = colors.primaryText
                        )
                        Text(
                            text = if (isVietnamese) "Tự động tính ngày thanh toán khi đến kỳ tiếp theo" else "Enable auto payment extension calculation", 
                            fontSize = 12.sp, 
                            color = colors.secondaryText
                        )
                    }
                    Switch(
                        checked = state.isAutoRenew,
                        onCheckedChange = { addViewModel.onAutoRenewChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.primaryAqua,
                            uncheckedThumbColor = colors.mutedText,
                            uncheckedTrackColor = colors.surface
                        ),
                        modifier = Modifier.testTag("form_autorenew_switch")
                    )
                }
            }

            // GROUP 4: REMINDERS & IMPORTANCE
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormGroupHeader(if (isVietnamese) "NHẮC NHỞ & ĐỘ QUAN TRỌ" else "REMINDER & IMPORTANCE")

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isVietnamese) "Gửi nhắc nhở trước ngày hạn" else "Reminder Days Before", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp, 
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Chips for selecting multiple reminder days offsets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(
                            7 to (if (isVietnamese) "7 ngày" else "7d before"),
                            3 to (if (isVietnamese) "3 ngày" else "3d before"),
                            1 to (if (isVietnamese) "1 ngày" else "1d before"),
                            0 to (if (isVietnamese) "Ngày hạn" else "Due day")
                        )
                        options.forEach { (days, label) ->
                            val isSelected = state.selectedReminderDays.contains(days)
                            FilterChip(
                                selected = isSelected,
                                onClick = { addViewModel.onReminderDayToggled(days) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = colors.surface,
                                    labelColor = colors.secondaryText,
                                    selectedContainerColor = colors.primaryAqua,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Box {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border),
                        onClick = { importanceExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isVietnamese) "Mức độ quan trọng" else "Importance Level", 
                                    fontSize = 11.sp, 
                                    color = colors.secondaryText
                                )
                                Text(
                                    text = if (isVietnamese) {
                                        when (state.selectedImportance) {
                                            "Low" -> "Thấp"
                                            "Medium" -> "Trung bình"
                                            "High" -> "Cao"
                                            else -> state.selectedImportance
                                        }
                                    } else {
                                        state.selectedImportance
                                    }, 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = colors.primaryText
                                )
                            }
                            Text("▼", fontSize = 12.sp, color = colors.mutedText)
                        }
                    }
                    DropdownMenu(
                        expanded = importanceExpanded,
                        onDismissRequest = { importanceExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        val levels = listOf("Low", "Medium", "High")
                        levels.forEach { level ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = if (isVietnamese) {
                                            when (level) {
                                                "Low" -> "Thấp"
                                                "Medium" -> "Trung bình"
                                                "High" -> "Cao"
                                                else -> level
                                            }
                                        } else {
                                            level
                                        }, 
                                        color = colors.primaryText,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    addViewModel.onImportanceSelected(level)
                                    importanceExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.managementUrl,
                    onValueChange = { addViewModel.onManagementUrlChange(it) },
                    label = { Text(if (isVietnamese) "Đường dẫn liên kết (Tùy chọn)" else "Management URL (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText,
                        focusedLabelColor = colors.primaryAqua,
                        unfocusedLabelColor = colors.mutedText,
                        focusedBorderColor = colors.primaryAqua,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.primaryAqua
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // GROUP 5: ADDITIONAL NOTES
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroupHeader(if (isVietnamese) "GHI CHÚ THÊM" else "ADDITIONAL NOTES")

                OutlinedTextField(
                    value = state.note,
                    onValueChange = { addViewModel.onNoteChange(it) },
                    label = { Text(if (isVietnamese) "Ghi chú (Tùy chọn)" else "Note (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText,
                        focusedLabelColor = colors.primaryAqua,
                        unfocusedLabelColor = colors.mutedText,
                        focusedBorderColor = colors.primaryAqua,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.primaryAqua
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FormGroupHeader(text: String) {
    val colors = LocalKotNestColors.current
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
