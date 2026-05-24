package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.datastore.SettingsManager
import com.example.core.util.DateUtils
import com.example.data.repository.DueMateRepositoryImpl
import com.example.domain.model.Category
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderRule
import com.example.domain.model.Subscription
import com.example.domain.repository.DueMateRepository
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.domain.usecase.SuggestionResult

data class AddPaymentUiState(
    val name: String = "",
    val amountText: String = "",
    val selectedCurrency: String = "VND",
    val nextDueDate: Long = System.currentTimeMillis(),
    val selectedBillingCycle: String = "Monthly",
    val customCycleValueText: String = "1",
    val selectedCategoryId: Int = 1,
    val selectedCategoryName: String = "",
    val selectedPaymentMethodId: Int? = null,
    val isAutoRenew: Boolean = true,
    val selectedReminderDays: List<Int> = listOf(7, 3, 1, 0),
    val selectedImportance: String = "Medium",
    val managementUrl: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false,
    val currentSuggestion: SuggestionResult? = null,
    val isSuggestionDismissed: Boolean = false
)

class AddPaymentViewModel(
    private val repository: DueMateRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPaymentUiState())
    val uiState: StateFlow<AddPaymentUiState> = _uiState.asStateFlow()

    val settings = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val categoriesFlow = repository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val paymentMethodsFlow = repository.getAllPaymentMethods().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun createCategory(name: String, icon: String, color: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Category name cannot be empty")
            return
        }
        viewModelScope.launch {
            try {
                val isDuplicate = categoriesFlow.value.any { it.name.equals(trimmed, ignoreCase = true) }
                if (isDuplicate) {
                    val isVietnamese = settings.value?.language == "vi"
                    onResult(false, if (isVietnamese) "Danh mục đã tồn tại" else "Category already exists")
                    return@launch
                }

                val newCategory = Category(
                    name = trimmed,
                    icon = icon.trim().ifEmpty { "📦" },
                    color = color.trim().ifEmpty { "#00BCD4" },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val newId = repository.insertCategory(newCategory)
                
                _uiState.update { state ->
                    state.copy(
                        selectedCategoryId = newId.toInt(),
                        selectedCategoryName = trimmed
                    )
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun createPaymentMethod(name: String, type: String, lastFour: String?, note: String?, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Payment method name cannot be empty")
            return
        }
        val cleanLastFour = lastFour?.trim()?.ifEmpty { null }
        if (cleanLastFour != null && (!cleanLastFour.all { it.isDigit() } || cleanLastFour.length > 4)) {
            val isVietnamese = settings.value?.language == "vi"
            onResult(false, if (isVietnamese) "4 số cuối phải là số và tối đa 4 ký tự" else "Last 4 digits must contain up to 4 digits")
            return
        }

        viewModelScope.launch {
            try {
                val isDuplicate = paymentMethodsFlow.value.any { it.name.equals(trimmed, ignoreCase = true) }
                if (isDuplicate) {
                    val isVietnamese = settings.value?.language == "vi"
                    onResult(false, if (isVietnamese) "Phương thức thanh toán đã tồn tại" else "Payment method already exists")
                    return@launch
                }

                val newMethod = PaymentMethod(
                    name = trimmed,
                    type = type.trim().ifEmpty { "Other" },
                    lastFourDigits = cleanLastFour,
                    note = note?.trim()?.ifEmpty { null },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val newId = repository.savePaymentMethod(newMethod)
                
                _uiState.update { state ->
                    state.copy(selectedPaymentMethodId = newId.toInt())
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    private var isLoaded = false

    fun loadSubscription(subscriptionId: Int, categories: List<Category>) {
        if (isLoaded || subscriptionId <= 0) return
        isLoaded = true
        
        // Ensure default selected category is set before loading
        if (categories.isNotEmpty()) {
            val defaultCat = categories.first()
            _uiState.update { 
                it.copy(
                    selectedCategoryId = defaultCat.id,
                    selectedCategoryName = defaultCat.name
                )
            }
        }

        viewModelScope.launch {
            repository.getSubscriptionByIdDirect(subscriptionId)?.let { sub ->
                val catName = categories.find { it.id == sub.categoryId }?.name ?: ""
                _uiState.update { state ->
                    state.copy(
                        name = sub.name,
                        amountText = if (sub.amount == sub.amount.toLong().toDouble()) sub.amount.toLong().toString() else sub.amount.toString(),
                        selectedCurrency = sub.currency,
                        nextDueDate = sub.nextDueDate,
                        selectedBillingCycle = sub.billingCycle,
                        customCycleValueText = sub.customCycleValue.toString(),
                        selectedCategoryId = sub.categoryId,
                        selectedCategoryName = catName,
                        selectedPaymentMethodId = sub.paymentMethodId,
                        isAutoRenew = sub.isAutoRenew,
                        selectedImportance = sub.importance,
                        managementUrl = sub.managementUrl ?: "",
                        note = sub.note ?: ""
                    )
                }
            }

            // Also load standard reminder rules for this subscription
            repository.getReminderRulesForSubscription(subscriptionId).collect { rules ->
                if (rules.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(selectedReminderDays = rules.map { it.daysBefore }.sorted())
                    }
                }
            }
        }
    }

    // Initialize state with default values from user settings
    fun initDefaultsFromSettings() {
        if (isLoaded) return
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { appSettings ->
                _uiState.update { state ->
                    state.copy(
                        selectedCurrency = appSettings.defaultCurrency
                    )
                }
            }
        }
    }

    private val suggestionEngine = com.example.domain.usecase.SmartDueSuggestionEngine()

    fun onNameChange(value: String) {
        val suggestion = if (settings.value?.smartSuggestionsEnabled != false) {
            suggestionEngine.suggest(value)
        } else {
            null
        }
        _uiState.update { 
            it.copy(
                name = value, 
                fieldErrors = it.fieldErrors - "name",
                currentSuggestion = if (it.isSuggestionDismissed && it.name.trim().lowercase() == value.trim().lowercase()) null else suggestion,
                isSuggestionDismissed = if (value.trim().isEmpty()) false else it.isSuggestionDismissed
            ) 
        }
    }

    fun applySuggestion() {
        val suggestion = _uiState.value.currentSuggestion ?: return
        val catList = categoriesFlow.value
        val matchedCategory = catList.find { it.id == suggestion.suggestedCategoryId || it.name.equals(suggestion.suggestedCategoryName, ignoreCase = true) }
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = matchedCategory?.id ?: state.selectedCategoryId,
                selectedCategoryName = matchedCategory?.name ?: state.selectedCategoryName,
                selectedBillingCycle = suggestion.suggestedBillingCycle,
                selectedImportance = suggestion.suggestedImportance,
                selectedReminderDays = suggestion.suggestedReminderDays,
                currentSuggestion = null,
                isSuggestionDismissed = true
            )
        }
    }

    fun dismissSuggestion() {
        _uiState.update { state ->
            state.copy(
                currentSuggestion = null,
                isSuggestionDismissed = true
            )
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountText = value, fieldErrors = it.fieldErrors - "amount") }
    }

    fun onCurrencySelected(value: String) {
        _uiState.update { it.copy(selectedCurrency = value) }
    }

    fun onDueDateSelected(date: Long) {
        _uiState.update { it.copy(nextDueDate = date) }
    }

    fun onBillingCycleSelected(value: String) {
        _uiState.update { 
            it.copy(
                selectedBillingCycle = value, 
                fieldErrors = it.fieldErrors - "billingCycle" - "customCycleValue"
            ) 
        }
    }

    fun onCustomCycleValueChange(value: String) {
        _uiState.update { 
            it.copy(
                customCycleValueText = value, 
                fieldErrors = it.fieldErrors - "customCycleValue"
            ) 
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { 
            it.copy(
                selectedCategoryId = category.id,
                selectedCategoryName = category.name,
                fieldErrors = it.fieldErrors - "category"
            ) 
        }
    }

    fun onPaymentMethodSelected(method: PaymentMethod?) {
        _uiState.update { it.copy(selectedPaymentMethodId = method?.id) }
    }

    fun onAutoRenewChanged(value: Boolean) {
        _uiState.update { it.copy(isAutoRenew = value) }
    }

    fun onReminderDayToggled(day: Int) {
        _uiState.update { state ->
            val newList = if (state.selectedReminderDays.contains(day)) {
                state.selectedReminderDays.filter { it != day }
            } else {
                (state.selectedReminderDays + day).sorted()
            }
            state.copy(selectedReminderDays = newList)
        }
    }

    fun onImportanceSelected(value: String) {
        _uiState.update { it.copy(selectedImportance = value) }
    }

    fun onManagementUrlChange(value: String) {
        _uiState.update { it.copy(managementUrl = value) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun saveDue(subscriptionId: Int = 0) {
        val state = _uiState.value
        val isVietnamese = settings.value?.language == "vi"
        
        // 1. Validation
        val errors = mutableMapOf<String, String>()
        
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            errors["name"] = if (isVietnamese) "Tên khoản mục không được để trống" else "Name is required"
        }
        
        val amount = state.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errors["amount"] = if (isVietnamese) "Số tiền phải lớn hơn 0" else "Amount must be greater than 0"
        }
        
        val isCustomCycle = state.selectedBillingCycle == "Every X days" || state.selectedBillingCycle == "Every X months"
        val customCycleValue = state.customCycleValueText.toIntOrNull()
        if (isCustomCycle && (customCycleValue == null || customCycleValue <= 0)) {
            errors["customCycleValue"] = if (isVietnamese) "Giá trị chu kỳ phải lớn hơn 0" else "Custom cycle value must be greater than 0"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { 
                it.copy(
                    fieldErrors = errors,
                    errorMessage = if (isVietnamese) "Vui lòng sửa các lỗi nhập liệu bên dưới." else "Please fix the visual inputs below."
                ) 
            }
            return
        }

        // 2. Perform Save
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Determine status
                val dueLocalDate = DateUtils.getLocalDateFromEpoch(state.nextDueDate)
                val todayLocalDate = LocalDate.now()
                val status = when {
                    dueLocalDate.isBefore(todayLocalDate) -> "Overdue"
                    dueLocalDate.isEqual(todayLocalDate) -> "Due Today"
                    else -> "Upcoming"
                }

                val subscription = Subscription(
                    id = subscriptionId,
                    name = trimmedName,
                    amount = amount ?: 0.0,
                    currency = state.selectedCurrency,
                    categoryId = state.selectedCategoryId,
                    billingCycle = state.selectedBillingCycle,
                    customCycleValue = if (isCustomCycle) (customCycleValue ?: 1) else 1,
                    nextDueDate = state.nextDueDate,
                    paymentMethodId = state.selectedPaymentMethodId,
                    isAutoRenew = state.isAutoRenew,
                    status = status,
                    importance = state.selectedImportance,
                    note = state.note.trim().ifEmpty { null },
                    managementUrl = state.managementUrl.trim().ifEmpty { null },
                    isPaused = false,
                    updatedAt = System.currentTimeMillis()
                )

                // Generate reminder rules to insert
                val reminderTime = settings.value?.defaultReminderTime ?: "09:00"
                val reminderRulesList = state.selectedReminderDays.map { days ->
                    ReminderRule(
                        subscriptionId = subscriptionId,
                        daysBefore = days,
                        reminderTime = reminderTime,
                        enabled = true
                    )
                }

                repository.saveSubscription(subscription, reminderRulesList)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        errorMessage = if (isVietnamese) "Có lỗi xảy ra khi lưu: ${e.message}" else "Failed to save: ${e.message}"
                    ) 
                }
            }
        }
    }
}

class AddPaymentViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddPaymentViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repository = DueMateRepositoryImpl(db)
            val settingsManager = SettingsManager(context)
            @Suppress("UNCHECKED_CAST")
            return AddPaymentViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
