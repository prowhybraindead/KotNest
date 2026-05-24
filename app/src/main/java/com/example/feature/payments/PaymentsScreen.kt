package com.example.feature.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateUtils
import com.example.core.datastore.AppSettings
import com.example.domain.model.ExchangeRateCache
import com.example.domain.model.Category
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Subscription
import com.example.feature.dashboard.StatusChip
import com.example.feature.dashboard.parseColor
import com.example.ui.theme.*
import com.example.ui.viewmodel.DueMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: DueMateViewModel,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val selectedStatusFilter by viewModel.selectedSubFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val ratesState by viewModel.ratesUiState.collectAsState()

    val isVietnamese = settingsState?.language == "vi"
    val categoryMap = categories.associateBy { it.id }
    val paymentMethodMap = paymentMethods.associateBy { it.id }
    val colors = LocalKotNestColors.current
    val rates = ratesState.rates

    // Search and Sort local state
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("due_date") } // "due_date", "amount_desc", "amount_asc", "status", "category"

    // Derive filtered and sorted sub-list efficiently
    val finalSubscriptions = remember(subscriptions, categoryMap, paymentMethodMap, searchQuery, sortBy) {
        var list = subscriptions.filter { sub ->
            if (searchQuery.trim().isEmpty()) return@filter true
            val query = searchQuery.trim()
            val nameMatch = sub.name.contains(query, ignoreCase = true)
            val catNameMatch = categoryMap[sub.categoryId]?.name?.contains(query, ignoreCase = true) == true
            val pmNameMatch = (sub.paymentMethodId?.let { paymentMethodMap[it]?.name })?.contains(query, ignoreCase = true) == true
            nameMatch || catNameMatch || pmNameMatch
        }

        when (sortBy) {
            "amount_desc" -> list.sortedByDescending { it.amount }
            "amount_asc" -> list.sortedBy { it.amount }
            "status" -> list.sortedBy { it.status }
            "category" -> list.sortedBy { categoryMap[it.categoryId]?.name ?: "" }
            else -> list.sortedBy { it.nextDueDate } // "due_date"
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isVietnamese) "Hoá đơn & Chi tiêu" else "Dues & Payments", 
                        fontWeight = FontWeight.Black, 
                        color = colors.primaryText,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundGradientStart
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            // 1. Sleek Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        text = if (isVietnamese) "Tìm tên, danh mục, ví thanh toán" else "Search dues, category, method...",
                        fontSize = 13.sp,
                        color = colors.mutedText
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .testTag("payments_search_query"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search", 
                        tint = colors.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "Clear", 
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primaryAqua,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.primaryText,
                    unfocusedTextColor = colors.primaryText
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // 2. Horizontal Status Filters
            val statusFilters = listOf("All", "Upcoming", "Due Today", "Overdue", "Paid", "Auto-renew")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(statusFilters) { item ->
                    val isSelected = selectedStatusFilter == item
                    val localizedLabel = when (item) {
                        "All" -> if (isVietnamese) "Tất cả" else "All"
                        "Upcoming" -> if (isVietnamese) "Sắp tới" else "Upcoming"
                        "Due Today" -> if (isVietnamese) "Hôm nay" else "Due Today"
                        "Overdue" -> if (isVietnamese) "Quá hạn" else "Overdue"
                        "Paid" -> if (isVietnamese) "Đã trả" else "Paid"
                        "Auto-renew" -> if (isVietnamese) "Tự động gia hạn" else "Auto-renew"
                        else -> item
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedSubFilter.value = item },
                        label = { Text(localizedLabel, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.deepAqua,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.mutedText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.deepAqua
                        ),
                        modifier = Modifier.testTag("filter_chip_$item")
                    )
                }
            }

            // 3. Horizontal Category Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                item {
                    val isSelected = selectedCategoryFilter == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategoryFilter.value = null },
                        label = { Text(if (isVietnamese) "Tất cả Danh mục" else "All Categories", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.deepAqua,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.mutedText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.deepAqua
                        )
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategoryFilter.value = cat.id },
                        label = { Text(cat.icon + " " + cat.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.deepAqua,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.mutedText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.deepAqua
                        )
                    )
                }
            }

            // 4. Compact Sort Toggle Row
            val sortOptions = listOf(
                "due_date" to (if (isVietnamese) "Ngày hạn" else "Due Date"),
                "amount_desc" to (if (isVietnamese) "Giá cao nhất" else "Price High"),
                "amount_asc" to (if (isVietnamese) "Giá thấp nhất" else "Price Low"),
                "status" to (if (isVietnamese) "Trạng thái" else "Status"),
                "category" to (if (isVietnamese) "Danh mục" else "Category")
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp)
            ) {
                items(sortOptions) { (key, label) ->
                    val isSelected = sortBy == key
                    val activeBgColor = colors.primaryAqua.copy(alpha = 0.15f)
                    val activeTextColor = colors.primaryAqua
                    val inactiveBgColor = Color.Transparent
                    val inactiveTextColor = colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeBgColor else inactiveBgColor)
                            .border(1.dp, if (isSelected) colors.primaryAqua.copy(alpha = 0.35f) else colors.border, RoundedCornerShape(12.dp))
                            .clickable { sortBy = key }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) activeTextColor else inactiveTextColor
                        )
                    }
                }
            }

            // 5. Dynamic Dues List
            if (finalSubscriptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.glassWhite,
                            modifier = Modifier
                                .size(80.dp)
                                .border(1.dp, colors.border, CircleShape),
                            content = {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🍃", fontSize = 36.sp)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isVietnamese) "Không Có Hoá Đơn Nào" else "No Dues Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isVietnamese) 
                                "Nhấn nút + trên dock để thêm khoản thanh toán đầu tiên của bạn vào KotNest." 
                                else "Tap the + button to track your first renewal with KotNest.",
                            fontSize = 14.sp,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(finalSubscriptions, key = { it.id }) { sub ->
                        val cat = categoryMap[sub.categoryId]
                        val paymentMethod = paymentMethodMap[sub.paymentMethodId]

                        PaymentListItem(
                            subscription = sub,
                            category = cat,
                            paymentMethod = paymentMethod,
                            settings = settingsState,
                            rates = rates,
                            onClick = { onNavigateToDetail(sub.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(115.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentListItem(
    subscription: Subscription,
    category: Category?,
    paymentMethod: PaymentMethod?,
    settings: AppSettings?,
    rates: List<ExchangeRateCache>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKotNestColors.current
    val categoryColor = parseColor(category?.color ?: "#4CAF50")
    val statusColor = when (subscription.status) {
        "Overdue" -> colors.danger
        "Due Today" -> colors.warning
        "Paid" -> colors.success
        else -> colors.primaryAqua
    }
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("payment_item_card_${subscription.id}")
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant status colored left stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category circular icon block
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(categoryColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val emojiText = category?.icon ?: "💳"
                        Text(
                            text = emojiText,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = subscription.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category?.name ?: "Other",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format("%,.0f", subscription.amount) + " " + subscription.currency,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        
                        val useCase = remember { com.example.domain.usecase.ConvertAmountToVndUseCase() }
                        val conversion = useCase(subscription.amount, subscription.currency, rates)
                        val showVnd = settings?.showEstimatedVnd != false && 
                                      !subscription.currency.equals("VND", ignoreCase = true) && 
                                      conversion.estimatedVndAmount != null
                        
                        if (showVnd) {
                            Text(
                                text = String.format("≈ %,.0f VND", conversion.estimatedVndAmount),
                                fontSize = 11.sp,
                                color = colors.primaryAqua,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subscription.billingCycle,
                            fontSize = 11.sp,
                            color = colors.mutedText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val labelText = if (settings?.language == "vi") "Hạn tiếp theo" else "Next Due Date"
                        Text(
                            text = labelText,
                            fontSize = 10.sp,
                            color = colors.mutedText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatEpoch(subscription.nextDueDate, if (settings?.language == "vi") "dd/MM/yyyy" else "dd MMM yyyy"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    if (paymentMethod != null) {
                        Column {
                            val methodLabel = if (settings?.language == "vi") "Ví chi trả" else "Payment Method"
                            Text(
                                text = methodLabel,
                                fontSize = 10.sp,
                                color = colors.mutedText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = paymentMethod.name,
                                fontSize = 12.sp,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    StatusChip(subscription.status)
                }
            }
        }
    }
}

