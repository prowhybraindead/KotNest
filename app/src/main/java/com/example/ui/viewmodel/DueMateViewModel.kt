package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.datastore.AppSettings
import com.example.core.datastore.SettingsManager
import com.example.core.util.DateUtils
import com.example.core.util.Exporter
import com.example.data.repository.DueMateRepositoryImpl
import com.example.domain.model.*
import com.example.domain.repository.DueMateRepository
import com.example.core.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DueMateViewModel(
    private val repository: DueMateRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    // 1. Core Flows from Repository
    val subscriptions: StateFlow<List<Subscription>> = repository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentMethods: StateFlow<List<PaymentMethod>> = repository.getAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<PaymentHistory>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings?> = settingsManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultLocalCurrencyList = listOf("USD", "EUR", "SGD", "AUD", "JPY", "KRW", "GBP", "CNY", "THB")

    private val _ratesUiState = MutableStateFlow(RatesUiState())
    val ratesUiState: StateFlow<RatesUiState> = _ratesUiState.asStateFlow()

    // 1b. Reactive Calendar Flows
    val selectedCalendarMonth = MutableStateFlow(YearMonth.now())
    val selectedCalendarDate = MutableStateFlow<LocalDate?>(null)

    val calendarUiState: StateFlow<CalendarUiState> = combine(
        subscriptions,
        selectedCalendarMonth,
        selectedCalendarDate
    ) { subs, month, selectedDate ->
        val paymentsByDate = subs.groupBy { sub ->
            DateUtils.getLocalDateFromEpoch(sub.nextDueDate)
        }
        val dayCells = getCalendarCells(month)
        val duesForSelectedDate = selectedDate?.let { paymentsByDate[it] } ?: emptyList()

        CalendarUiState(
            selectedMonth = month,
            dayCells = dayCells,
            subscriptions = subs,
            paymentsByDate = paymentsByDate,
            selectedDate = selectedDate,
            duesForSelectedDate = duesForSelectedDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectCalendarMonth(month: YearMonth) {
        selectedCalendarMonth.value = month
    }

    fun selectCalendarDate(date: LocalDate?) {
        selectedCalendarDate.value = date
    }

    private fun getCalendarCells(yearMonth: YearMonth): List<LocalDate?> {
        val firstDayOfMonth = yearMonth.atDay(1)
        val totalDays = yearMonth.lengthOfMonth()
        val firstDayDayOfWeek = firstDayOfMonth.dayOfWeek.value
        
        val cells = mutableListOf<LocalDate?>()
        val paddingDays = firstDayDayOfWeek - 1
        for (i in 0 until paddingDays) {
            cells.add(null)
        }
        for (day in 1..totalDays) {
            cells.add(yearMonth.atDay(day))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    init {
        viewModelScope.launch {
            repository.getAllRatesFlow().collect { cachedList ->
                val currentTarget = settings.value?.preferredTargetCurrency ?: "VND"
                val filtered = cachedList.filter { it.targetCurrency == currentTarget }
                _ratesUiState.update { state ->
                    val lastUp = filtered.maxOfOrNull { it.fetchedAt } ?: 0L
                    val providerText = filtered.firstOrNull()?.provider ?: (settings.value?.exchangeRateProviderName ?: "ExchangeRate-API Open Access")
                    val updatedState = state.copy(
                        rates = filtered,
                        targetCurrency = currentTarget,
                        lastUpdated = lastUp,
                        providerName = providerText,
                        isCached = filtered.isNotEmpty()
                    )
                    recalculateConverterValue(updatedState)
                }
            }
        }

        viewModelScope.launch {
            repository.getAllHistoryFlow("VND").collect { historyList ->
                _ratesUiState.update { state ->
                    state.copy(history = historyList)
                }
            }
        }

        viewModelScope.launch {
            settings.filterNotNull().collect { currentSettings ->
                // Once settings are retrieved, trigger automatic rates update if stale
                if (currentSettings.autoRefreshRates) {
                    val cachedList = repository.getAllRatesDirect()
                    val now = System.currentTimeMillis()
                    val threshold = currentSettings.refreshIntervalHours * 60 * 60 * 1000L
                    val filtered = cachedList.filter { it.targetCurrency == currentSettings.preferredTargetCurrency }
                    val lastFetched = filtered.maxOfOrNull { it.fetchedAt } ?: 0L
                    if (filtered.isEmpty() || (now - lastFetched) > threshold) {
                        fetchRates(force = false)
                    }
                }
            }
        }
    }

    private fun recalculateConverterValue(state: RatesUiState): RatesUiState {
        val cleanAmount = state.converterAmount.toDoubleOrNull() ?: 0.0
        val rateObj = state.rates.firstOrNull { it.baseCurrency == state.converterSourceCurrency }
        val rateValue = rateObj?.rate ?: 0.0
        val converted = cleanAmount * rateValue
        return state.copy(convertedValue = converted)
    }

    fun updateConverter(amount: String, sourceCurrency: String) {
        _ratesUiState.update { state ->
            val cleanAmount = amount.toDoubleOrNull() ?: 0.0
            val rateObj = state.rates.firstOrNull { it.baseCurrency == sourceCurrency }
            val rateValue = rateObj?.rate ?: 0.0
            val converted = cleanAmount * rateValue
            state.copy(
                converterAmount = amount,
                converterSourceCurrency = sourceCurrency,
                convertedValue = converted
            )
        }
    }

    fun fetchRates(force: Boolean = true) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: return@launch
            _ratesUiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val selectedProviderName = currentSettings.exchangeRateProviderName
            val primaryProvider = when (selectedProviderName) {
                "ExchangeRate-API Open Access" -> com.example.core.network.ExchangeRateApiOpenAccessProvider()
                "Frankfurter API" -> com.example.core.network.FrankfurterExchangeRateProvider()
                "ExchangeRate-API (Placeholder)" -> com.example.core.network.ExchangeRateApiOpenAccessProvider()
                "Custom Backend API (Placeholder)" -> com.example.core.network.CustomBackendExchangeRateProvider()
                else -> com.example.core.network.ExchangeRateApiOpenAccessProvider()
            }
            
            val fallbacks = mutableListOf<com.example.core.network.ExchangeRateProvider>()
            if (primaryProvider.name != "ExchangeRate-API Open Access") {
                fallbacks.add(com.example.core.network.ExchangeRateApiOpenAccessProvider())
            }
            if (primaryProvider.name != "Frankfurter API") {
                fallbacks.add(com.example.core.network.FrankfurterExchangeRateProvider())
            }
            
            var successList = emptyList<com.example.domain.model.ExchangeRateCache>()
            var usedProviderName = primaryProvider.name
            
            try {
                successList = primaryProvider.getExchangeRates(
                    baseCurrencies = defaultLocalCurrencyList,
                    targetCurrency = currentSettings.preferredTargetCurrency
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (successList.isEmpty()) {
                for (fallback in fallbacks) {
                    try {
                        successList = fallback.getExchangeRates(
                            baseCurrencies = defaultLocalCurrencyList,
                            targetCurrency = currentSettings.preferredTargetCurrency
                        )
                        if (successList.isNotEmpty()) {
                            usedProviderName = fallback.name
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            if (successList.isNotEmpty()) {
                repository.saveRates(successList)
                
                try {
                    val todayDateText = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val historyEntries = successList.map { rate ->
                        com.example.domain.model.ExchangeRateHistory(
                            currencyCode = rate.baseCurrency,
                            targetCurrency = rate.targetCurrency,
                            date = todayDateText,
                            rateToVnd = rate.rate,
                            provider = rate.provider
                        )
                    }
                    repository.saveHistory(historyEntries)
                } catch (he: Exception) {
                    he.printStackTrace()
                }
                
                _ratesUiState.update { state ->
                    val updated = state.copy(
                        isLoading = false,
                        isCached = false,
                        isOffline = false,
                        providerName = usedProviderName,
                        errorMessage = null
                    )
                    recalculateConverterValue(updated)
                }
            } else {
                _ratesUiState.update { state ->
                    val hasCachedData = state.rates.isNotEmpty()
                    state.copy(
                        isLoading = false,
                        errorMessage = if (hasCachedData) {
                            if (currentSettings.language == "vi") "Không thể cập nhật tỷ giá. Đang hiển thị dữ liệu lưu trữ."
                            else "Couldn't update rates. Showing last saved rates."
                        } else {
                            if (currentSettings.language == "vi") "Lỗi tải tỷ giá. Vui lòng kiểm tra kết nối mạng."
                            else "Couldn't load exchange rates. Check your connection or try again."
                        }
                    )
                }
            }
        }
    }

    fun updatePreferredTargetCurrency(currency: String) {
        viewModelScope.launch {
            settingsManager.updatePreferredTargetCurrency(currency)
        }
    }

    fun updateRefreshIntervalHours(hours: Int) {
        viewModelScope.launch {
            settingsManager.updateRefreshIntervalHours(hours)
        }
    }

    fun updateAutoRefreshRates(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateAutoRefreshRates(enabled)
        }
    }

    fun updateExchangeRateProviderName(name: String) {
        viewModelScope.launch {
            settingsManager.updateExchangeRateProviderName(name)
        }
    }

    // 2. Filter states
    val selectedSubFilter = MutableStateFlow("All") // "All", "Upcoming", "Due Today", "Overdue", "Paid", "Auto-renew"
    val selectedCategoryFilter = MutableStateFlow<Int?>(null) // categoryId, null for all

    // 3. Derived Filtered Subscriptions Flow
    val filteredSubscriptions: StateFlow<List<Subscription>> = combine(
        subscriptions,
        selectedSubFilter,
        selectedCategoryFilter
    ) { subs, statusFilter, catFilter ->
        var list = subs
        
        // Apply status filter
        list = when (statusFilter) {
            "Upcoming" -> list.filter { it.status == "Upcoming" && !it.isPaused }
            "Due Today" -> list.filter { it.status == "Due Today" && !it.isPaused }
            "Overdue" -> list.filter { it.status == "Overdue" && !it.isPaused }
            "Paid" -> list.filter { it.status == "Paid" }
            "Auto-renew" -> list.filter { it.isAutoRenew && !it.isPaused }
            else -> list // "All"
        }

        // Apply category filter
        if (catFilter != null) {
            list = list.filter { it.categoryId == catFilter }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Dashboard Metrics Derived Flows
    data class DashboardMetrics(
        val currentMonthTotal: Double,
        val upcomingCount: Int,
        val overdueCount: Int,
        val closestUpcoming: Subscription?,
        val dueNextSevenDays: List<Subscription>,
        val hasForeignCurrency: Boolean = false
    )

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        subscriptions,
        repository.getAllRatesFlow(),
        settings
    ) { subs, rates, currentSettings ->
        val summaryUseCase = com.example.domain.usecase.GetDashboardSummaryUseCase()
        val summary = summaryUseCase(subs, rates, currentSettings?.showEstimatedVnd != false)

        DashboardMetrics(
            currentMonthTotal = summary.currentMonthTotal,
            upcomingCount = summary.upcomingCount,
            overdueCount = summary.overdueCount,
            closestUpcoming = summary.closestUpcoming,
            dueNextSevenDays = summary.dueNextSevenDays,
            hasForeignCurrency = summary.hasForeignCurrency
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardMetrics(0.0, 0, 0, null, emptyList(), false)
    )

    // 5. Actions
    fun saveSubscription(
        id: Int,
        name: String,
        amount: Double,
        currency: String,
        categoryId: Int,
        billingCycle: String,
        customCycleValue: Int,
        nextDueDate: Long,
        paymentMethodId: Int?,
        isAutoRenew: Boolean,
        importance: String,
        note: String?,
        managementUrl: String?,
        isPaused: Boolean
    ) {
        viewModelScope.launch {
            // Determine initial status based on due date
            val now = System.currentTimeMillis()
            val todayStart = DateUtils.getEpochFromLocalDate(LocalDate.now())
            val dueLocalDate = DateUtils.getLocalDateFromEpoch(nextDueDate)
            val todayLocalDate = LocalDate.now()

            val status = when {
                isPaused -> "Paused"
                dueLocalDate.isBefore(todayLocalDate) -> "Overdue"
                dueLocalDate.isEqual(todayLocalDate) -> "Due Today"
                else -> "Upcoming"
            }

            val sub = Subscription(
                id = id,
                name = name,
                amount = amount,
                currency = currency,
                categoryId = categoryId,
                billingCycle = billingCycle,
                customCycleValue = customCycleValue,
                nextDueDate = nextDueDate,
                paymentMethodId = paymentMethodId,
                isAutoRenew = isAutoRenew,
                status = status,
                importance = importance,
                note = note,
                managementUrl = managementUrl,
                isPaused = isPaused,
                updatedAt = System.currentTimeMillis()
            )

            // Calculate standard reminder rules based on user settings
            val currentSettings = settings.value
            val reminderDays = currentSettings?.defaultReminderDays ?: 3
            val reminderTime = currentSettings?.defaultReminderTime ?: "09:00"

            val rules = listOf(
                ReminderRule(subscriptionId = id, daysBefore = 0, reminderTime = reminderTime, enabled = true),
                ReminderRule(subscriptionId = id, daysBefore = reminderDays, reminderTime = reminderTime, enabled = true)
            )

            repository.saveSubscription(sub, rules)
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }

    fun togglePauseSubscription(subscriptionId: Int) {
        viewModelScope.launch {
            val sub = repository.getSubscriptionByIdDirect(subscriptionId) ?: return@launch
            val newIsPaused = !sub.isPaused
            val now = System.currentTimeMillis()
            val todayLocalDate = LocalDate.now()
            val dueLocalDate = DateUtils.getLocalDateFromEpoch(sub.nextDueDate)

            val newStatus = if (newIsPaused) {
                "Paused"
            } else {
                when {
                    dueLocalDate.isBefore(todayLocalDate) -> "Overdue"
                    dueLocalDate.isEqual(todayLocalDate) -> "Due Today"
                    else -> "Upcoming"
                }
            }

            val updated = sub.copy(
                isPaused = newIsPaused,
                status = newStatus,
                updatedAt = now
            )
            repository.saveSubscription(updated, emptyList()) // retains existing rules basically
        }
    }

    fun markAsPaid(subscriptionId: Int, note: String? = null) {
        viewModelScope.launch {
            repository.markAsPaid(subscriptionId, note)
        }
    }

    // Settings actions
    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            settingsManager.updateDefaultCurrency(currency)
        }
    }

    fun setDefaultReminderTime(time: String) {
        viewModelScope.launch {
            settingsManager.updateDefaultReminderTime(time)
        }
    }

    fun setDefaultReminderDays(days: Int) {
        viewModelScope.launch {
            settingsManager.updateDefaultReminderDays(days)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.updateTheme(theme)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateNotificationsEnabled(enabled)
        }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateDailySummaryEnabled(enabled)
        }
    }

    fun setOverdueRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateOverdueRemindersEnabled(enabled)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateAppLockEnabled(enabled)
        }
    }

    fun setBackendBaseUrl(url: String) {
        viewModelScope.launch {
            settingsManager.updateBackendBaseUrl(url)
        }
    }

    fun setShowEstimatedVnd(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateShowEstimatedVnd(enabled)
        }
    }

    fun setSmartSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateSmartSuggestionsEnabled(enabled)
        }
    }

    fun saveBackupMetadata(time: Long, count: Int, fileName: String) {
        viewModelScope.launch {
            settingsManager.updateBackupMetadata(time, count, fileName)
        }
    }

    fun saveImportMetadata(time: Long) {
        viewModelScope.launch {
            settingsManager.updateImportMetadata(time)
        }
    }

    // Export & Import
    fun exportBackupJson(context: Context, database: AppDatabase, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = Exporter.exportToJSON(context, database)
                saveBackupMetadata(System.currentTimeMillis(), result.first, result.second)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun importBackupJson(inputStream: InputStream, database: AppDatabase, onResult: (com.example.core.util.ImportResult?) -> Unit) {
        viewModelScope.launch {
            val result = Exporter.importFromJSON(inputStream, database)
            if (result != null) {
                saveImportMetadata(System.currentTimeMillis())
            }
            onResult(result)
        }
    }

    fun exportBackupCsv(context: Context, database: AppDatabase) {
        viewModelScope.launch {
            Exporter.exportToCSV(context, database)
        }
    }

    fun getSubscriptionStream(id: Int): Flow<Subscription?> {
        return repository.getSubscriptionById(id)
    }

    fun getHistoryStream(id: Int): Flow<List<PaymentHistory>> {
        return repository.getHistoryForSubscription(id)
    }

    fun savePaymentMethod(name: String, type: String, lastFourDigits: String?, note: String?) {
        viewModelScope.launch {
            repository.savePaymentMethod(
                PaymentMethod(
                    name = name,
                    type = type,
                    lastFourDigits = lastFourDigits,
                    note = note
                )
            )
        }
    }

    fun saveCategory(name: String, icon: String, color: String, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Category name cannot be empty")
            return
        }
        val isDuplicate = categories.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (isDuplicate) {
            val isVietnamese = settings.value?.language == "vi"
            onResult(false, if (isVietnamese) "Danh mục đã tồn tại" else "Category already exists")
            return
        }
        viewModelScope.launch {
            try {
                repository.insertCategory(
                    Category(
                        name = trimmed,
                        icon = icon.trim().ifEmpty { "📦" },
                        color = color.trim().ifEmpty { "#00BCD4" }
                    )
                )
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun deleteCategory(id: Int, onResult: (Boolean, String?) -> Unit) {
        val isVietnamese = settings.value?.language == "vi"
        val isUsed = subscriptions.value.any { it.categoryId == id }
        if (isUsed) {
            onResult(false, if (isVietnamese) "Danh mục đang được sử dụng bởi các khoản chi khác và không thể xóa." else "This category is in use by existing dues and cannot be deleted.")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun deletePaymentMethod(id: Int, onResult: (Boolean, String?) -> Unit) {
        val isVietnamese = settings.value?.language == "vi"
        val isUsed = subscriptions.value.any { it.paymentMethodId == id }
        if (isUsed) {
            onResult(false, if (isVietnamese) "Phương thức thanh toán đang được sử dụng và không thể xóa." else "This payment method is in use by existing dues and cannot be deleted.")
            return
        }
        viewModelScope.launch {
            try {
                repository.deletePaymentMethod(id)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun savePaymentMethodCustom(name: String, type: String, lastFour: String?, note: String?, onResult: (Boolean, String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Name cannot be empty")
            return
        }
        val isDuplicate = paymentMethods.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (isDuplicate) {
            val isVietnamese = settings.value?.language == "vi"
            onResult(false, if (isVietnamese) "Phương thức thanh toán đã tồn tại" else "Payment method already exists")
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
                repository.savePaymentMethod(
                    PaymentMethod(
                        name = trimmed,
                        type = type,
                        lastFourDigits = cleanLastFour,
                        note = note?.trim()?.ifEmpty { null }
                    )
                )
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}

// Custom factory to inject database + settingsManager cleanly
class DueMateViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DueMateViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope)
            val repository = DueMateRepositoryImpl(db)
            val settingsManager = SettingsManager(context)
            @Suppress("UNCHECKED_CAST")
            return DueMateViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class RatesUiState(
    val isLoading: Boolean = false,
    val rates: List<com.example.domain.model.ExchangeRateCache> = emptyList(),
    val history: List<com.example.domain.model.ExchangeRateHistory> = emptyList(),
    val targetCurrency: String = "VND",
    val lastUpdated: Long = 0L,
    val providerName: String = "ExchangeRate-API Open Access",
    val isCached: Boolean = true,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val converterAmount: String = "1",
    val converterSourceCurrency: String = "USD",
    val convertedValue: Double = 0.0
)

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val dayCells: List<LocalDate?> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val paymentsByDate: Map<LocalDate, List<Subscription>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val duesForSelectedDate: List<Subscription> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
