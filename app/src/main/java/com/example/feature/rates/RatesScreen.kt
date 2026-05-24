package com.example.feature.rates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.util.DateUtils
import com.example.ui.theme.LocalKotNestColors
import com.example.ui.theme.KotNestColors
import com.example.ui.viewmodel.DueMateViewModel
import com.example.ui.viewmodel.RatesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(
    viewModel: DueMateViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.ratesUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle(null)
    val isVietnamese = settingsState?.language == "vi"

    val colors = LocalKotNestColors.current
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    // Localizations
    val titleText = if (isVietnamese) "Tỷ giá" else "Exchange Rates"
    val subtitleText = if (isVietnamese) "Công cụ chuyển đổi và lịch sử" else "Converter and historical trends"
    val updatedText = if (isVietnamese) "Cập nhật lúc" else "Updated"
    val providerTextLabel = if (isVietnamese) "Nguồn dữ liệu" else "Data Source"
    val converterTitle = if (isVietnamese) "Bộ chuyển đổi ngoại tệ" else "Quick Converter"
    val disclaimerText = if (isVietnamese) {
        "Thông tin tỷ giá mang tính tham khảo và có thể biến động nhẹ so với tỷ giá giao dịch thực tế trên thẻ hoặc ngân hàng quốc tế."
    } else {
        "Exchange rates are for reference and may differ from your bank's real-time settlement rate."
    }
    val amountPlaceholder = if (isVietnamese) "Nhập số tiền" else "Enter amount"
    val lastUpdatedFormatted = if (state.lastUpdated > 0L) {
        val pattern = if (isVietnamese) "HH:mm dd/MM/yyyy" else "MMM dd, yyyy HH:mm"
        DateUtils.formatEpoch(state.lastUpdated, pattern)
    } else {
        if (isVietnamese) "Chưa rõ" else "Never"
    }

    val positiveColor = if (colors.isLight) Color(0xFF12B76A) else Color(0xFF22C55E)
    val negativeColor = if (colors.isLight) Color(0xFFF04438) else Color(0xFFEF4444)
    val neutralColor = if (colors.isLight) Color(0xFF00AEEF) else Color(0xFF48CAE4)

    var selectedDetailCurrency by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Status drawing safe spacer
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 1. Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("rates_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.primaryText,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.primaryText,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Refresh Button with state-based loader animation
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = colors.primaryAqua.copy(alpha = 0.2f),
                                spotColor = colors.primaryAqua.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(enabled = !state.isLoading) {
                                viewModel.fetchRates(force = true)
                            }
                            .testTag("rates_refresh_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.primaryAqua
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh rates",
                                tint = colors.deepAqua,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Offline Mode Banner
            if (state.isOffline || (state.errorMessage != null && state.rates.isNotEmpty())) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.warning.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, colors.warning.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Warning",
                                tint = colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isVietnamese) "Ngoại tuyến — đang hiển thị tỷ giá được lưu gần nhất" else "Offline — showing last saved rates",
                                fontSize = 13.sp,
                                color = if (colors.isLight) colors.primaryText else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Premium Status Indicator Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0x0F000000),
                            spotColor = if (colors.isLight) colors.primaryAqua.copy(alpha = 0.15f) else colors.cyanAccent.copy(alpha = 0.15f)
                        )
                        .border(1.5.dp, colors.border, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryAqua.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🇻🇳", fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = if (isVietnamese) "Tiền tệ mục tiêu" else "Target Currency",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.mutedText,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "VND (₫)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.primaryText
                                    )
                                }
                            }
                            
                            // Dynamic status chip
                            val topStatusLabel = when {
                                state.isLoading -> if (isVietnamese) "Đang tải" else "Loading"
                                state.errorMessage != null && state.rates.isEmpty() -> if (isVietnamese) "Lỗi két nối" else "Error"
                                state.isOffline -> if (isVietnamese) "Lưu trữ" else "Cached"
                                else -> if (isVietnamese) "Hoạt động" else "Live"
                            }
                            val topStatusColor = when {
                                state.isLoading -> colors.primaryAqua
                                state.errorMessage != null && state.rates.isEmpty() -> colors.danger
                                state.isOffline -> colors.warning
                                else -> colors.success
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(topStatusColor.copy(alpha = 0.12f))
                                    .border(1.dp, topStatusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = topStatusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = topStatusColor
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isVietnamese) "Nguồn cung cấp" else "Provider",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText
                                )
                                Text(
                                    text = state.providerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = updatedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText
                                )
                                Text(
                                    text = lastUpdatedFormatted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                }
            }

            // 4. Functional Converter Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (colors.isLight) Color(0xFFF8FAFC) else colors.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .border(
                            width = 1.5.dp,
                            color = colors.primaryAqua.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = converterTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.primaryText
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.border)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FAST CONVERT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.primaryAqua
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Interactive quick-switch currencies list
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            val currencies = listOf("USD", "EUR", "SGD", "AUD", "JPY", "KRW", "GBP", "CNY", "THB")
                            items(currencies) { currency ->
                                val isSelected = state.converterSourceCurrency == currency
                                val flag = getCurrencyFlag(currency)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) colors.primaryAqua.copy(alpha = 0.15f)
                                            else colors.border.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) colors.primaryAqua else colors.border,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            viewModel.updateConverter(state.converterAmount, currency)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = flag, fontSize = 15.sp)
                                        Text(
                                            text = currency,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) (if (colors.isLight) colors.deepAqua else colors.cyanAccent) else colors.secondaryText
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Double inputs layout with clear display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Amount input
                            TextField(
                                value = state.converterAmount,
                                onValueChange = { newValue ->
                                    val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                                    viewModel.updateConverter(filteredValue, state.converterSourceCurrency)
                                },
                                placeholder = { Text(amountPlaceholder, fontSize = 14.sp, color = colors.mutedText) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        text = state.converterSourceCurrency,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.mutedText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = colors.surface,
                                    unfocusedContainerColor = colors.surface,
                                    focusedIndicatorColor = colors.primaryAqua,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .testTag("converter_amount_input")
                            )
                            
                            Text(
                                text = "≈",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryAqua
                            )
                            
                            // Target calculation box
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format("%,.0f", state.convertedValue),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.primaryAqua,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "VND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5a. UI Skeleton Loading Screen
            if (state.isLoading && state.rates.isEmpty()) {
                items(5) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.border.copy(alpha = 0.25f))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.border.copy(alpha = 0.25f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.border.copy(alpha = 0.15f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.border.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }

            // 5b. Empty State Retry Card
            else if (state.rates.isEmpty() && !state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📉",
                            fontSize = 44.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = if (isVietnamese) "Không có kết nối tỷ giá" else "Couldn't load exchange rates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVietnamese) "Vui lòng kết nối mạng và nhấp thử lại hoặc quay lại sau." else "Check your connection and try again.",
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.fetchRates(force = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAqua),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(if (isVietnamese) "Thử lại" else "Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // 5c. Beautiful Currency Snapshots Cards list
                items(state.rates) { rate ->
                    val currencyName = getCurrencyName(rate.baseCurrency, isVietnamese)
                    val flag = getCurrencyFlag(rate.baseCurrency)
                    val countryLabel = getCurrencyCountry(rate.baseCurrency, isVietnamese)

                    // Aggregate local historical snapshots: sort ascending, remove duplicates (latest per date), take last 7
                    val currencyHistory = state.history
                        .filter { h -> h.currencyCode == rate.baseCurrency }
                        .groupBy { h -> h.date }
                        .mapValues { entry -> entry.value.maxByOrNull { it.updatedAt } ?: entry.value.first() }
                        .values
                        .sortedBy { h -> h.date }
                        .takeLast(7)
                    val rateValues = currencyHistory.map { h -> h.rateToVnd }

                    val (changeText, trendColor) = if (rateValues.size >= 2) {
                        val latestRate = rateValues.last()
                        val oldestRate = rateValues.first()
                        val diff = latestRate - oldestRate
                        val percent = (diff / oldestRate) * 100
                        val formatted = String.format("%+.2f%%", percent)
                        val color = if (percent > 0.0) positiveColor else if (percent < 0.0) negativeColor else neutralColor
                        Pair(formatted, color)
                    } else {
                        Pair(if (isVietnamese) "Đang tích lũy" else "Chart builds over time", colors.mutedText)
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color(0x0A000000),
                                spotColor = Color(0x0C0077B6)
                            )
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .clickable { selectedDetailCurrency = rate.baseCurrency }
                            .testTag("rate_item_${rate.baseCurrency}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Avatar & Info
                            Row(
                                modifier = Modifier.weight(1.3f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.backgroundGradientEnd)
                                        .border(1.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = flag, fontSize = 20.sp)
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = rate.baseCurrency,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "• $countryLabel",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currencyName,
                                        fontSize = 11.sp,
                                        color = colors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Center micro Sparkline
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(35.dp)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (rateValues.size >= 2) {
                                    SparklineChart(
                                        rates = rateValues,
                                        color = trendColor,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = if (isVietnamese) "Mới..." else "Muted...",
                                        fontSize = 10.sp,
                                        color = colors.mutedText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Right values trends
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                val rateValueFormatted = if (rate.rate >= 100.0) {
                                    String.format("%,.0f", rate.rate)
                                } else {
                                    String.format("%,.2f", rate.rate)
                                }

                                Text(
                                    text = "$rateValueFormatted ₫",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (colors.isLight) colors.deepAqua else colors.primaryAqua
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (rateValues.size >= 2) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(trendColor)
                                        )
                                    }
                                    Text(
                                        text = changeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = trendColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Header Disclaimer card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = disclaimerText,
                    fontSize = 11.sp,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(115.dp))
            }
        }
    }

    // Interactive Rate Detail sheets
    if (selectedDetailCurrency != null) {
        val detailCurrency = selectedDetailCurrency!!
        val rateObj = state.rates.firstOrNull { it.baseCurrency == detailCurrency }
        if (rateObj != null) {
            RateDetailBottomSheet(
                rate = rateObj,
                history = state.history.filter { h -> h.currencyCode == detailCurrency },
                onDismiss = { selectedDetailCurrency = null },
                isVietnamese = isVietnamese,
                colors = colors,
                positiveColor = positiveColor,
                negativeColor = negativeColor,
                neutralColor = neutralColor
            )
        }
    }
}

// Helpers for currency properties mapping
fun getCurrencyFlag(code: String): String {
    return when (code) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "SGD" -> "🇸🇬"
        "AUD" -> "🇦🇺"
        "JPY" -> "🇯🇵"
        "KRW" -> "🇰🇷"
        "GBP" -> "🇬🇧"
        "CNY" -> "🇨🇳"
        "THB" -> "🇹🇭"
        else -> "🏳️"
    }
}

fun getCurrencyName(code: String, isVietnamese: Boolean): String {
    return when (code) {
        "USD" -> if (isVietnamese) "Đô-la Mỹ" else "United States Dollar"
        "EUR" -> "Euro"
        "SGD" -> if (isVietnamese) "Đô-la Singapore" else "Singapore Dollar"
        "AUD" -> if (isVietnamese) "Đô-la Úc" else "Australian Dollar"
        "JPY" -> if (isVietnamese) "Yên Nhật" else "Japanese Yen"
        "KRW" -> if (isVietnamese) "Won Hàn Quốc" else "South Korean Won"
        "GBP" -> if (isVietnamese) "Bảng Anh" else "British Pound"
        "CNY" -> if (isVietnamese) "Nhân dân tệ" else "Chinese Yuan"
        "THB" -> if (isVietnamese) "Baht Thái" else "Thai Baht"
        else -> code
    }
}

fun getCurrencyCountry(code: String, isVietnamese: Boolean): String {
    return when (code) {
        "USD" -> if (isVietnamese) "Hoa Kỳ" else "USA"
        "EUR" -> if (isVietnamese) "Châu Âu" else "Eurozone"
        "SGD" -> "Singapore"
        "AUD" -> if (isVietnamese) "Úc" else "Australia"
        "JPY" -> if (isVietnamese) "Nhật Bản" else "Japan"
        "KRW" -> if (isVietnamese) "Hàn Quốc" else "South Korea"
        "GBP" -> if (isVietnamese) "Anh Quốc" else "UK"
        "CNY" -> if (isVietnamese) "Trung Quốc" else "China"
        "THB" -> if (isVietnamese) "Thái Lan" else "Thailand"
        else -> code
    }
}

@Composable
fun SparklineChart(
    rates: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (rates.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        val minRate = rates.minOrNull() ?: 0.0
        val maxRate = rates.maxOrNull() ?: 0.0
        val range = if (maxRate == minRate) 1.0 else maxRate - minRate

        val points = rates.mapIndexed { index, rate ->
            val x = index.toFloat() / (rates.size - 1) * width
            val y = if (maxRate == minRate) {
                height / 2f
            } else {
                height - ((rate - minRate) / range * height * 0.7f).toFloat() - (height * 0.15f)
            }
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prevPoint = points[i - 1]
                val currentPoint = points[i]
                val controlX = (prevPoint.x + currentPoint.x) / 2
                cubicTo(controlX, prevPoint.y, controlX, currentPoint.y, currentPoint.x, currentPoint.y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.15f), Color.Transparent)
            )
        )
    }
}

@Composable
fun DetailedRatesChart(
    rates: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    colors: KotNestColors
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (rates.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        val minRate = rates.minOrNull() ?: 0.0
        val maxRate = rates.maxOrNull() ?: 0.0
        val range = if (maxRate == minRate) 1.0 else maxRate - minRate

        // Draw horizontal dashed-looking grid lines
        val gridColor = colors.border.copy(alpha = 0.3f)
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = i.toFloat() / gridCount * height
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw data curve points
        val points = rates.mapIndexed { index, rate ->
            val x = index.toFloat() / (rates.size - 1) * width
            val y = height - ((rate - minRate) / range * height * 0.72f).toFloat() - (height * 0.14f)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prevPoint = points[i - 1]
                val currentPoint = points[i]
                val controlX = (prevPoint.x + currentPoint.x) / 2
                cubicTo(controlX, prevPoint.y, controlX, currentPoint.y, currentPoint.x, currentPoint.y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // Filled area gradient
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Highlight custom point circles
        points.forEach { pt ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = color,
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateDetailBottomSheet(
    rate: com.example.domain.model.ExchangeRateCache,
    history: List<com.example.domain.model.ExchangeRateHistory>,
    onDismiss: () -> Unit,
    isVietnamese: Boolean,
    colors: KotNestColors,
    positiveColor: Color,
    negativeColor: Color,
    neutralColor: Color
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("rate_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            val currencyName = getCurrencyName(rate.baseCurrency, isVietnamese)
            val flag = getCurrencyFlag(rate.baseCurrency)
            val countryLabel = getCurrencyCountry(rate.baseCurrency, isVietnamese)

            // Header Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.backgroundGradientEnd)
                        .border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = flag, fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${rate.baseCurrency} — $countryLabel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primaryText
                    )
                    Text(
                        text = currencyName,
                        fontSize = 11.sp,
                        color = colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Trend parsing
            val sortedHistory = history
                .groupBy { h -> h.date }
                .mapValues { entry -> entry.value.maxByOrNull { it.updatedAt } ?: entry.value.first() }
                .values
                .sortedBy { h -> h.date }
                .takeLast(7)
            val rateValues = sortedHistory.map { h -> h.rateToVnd }

            val (changeText, trendColor) = if (rateValues.size >= 2) {
                val latestRate = rateValues.last()
                val oldestRate = rateValues.first()
                val diff = latestRate - oldestRate
                val percent = (diff / oldestRate) * 100
                val formatted = String.format("%+.2f%%", percent)
                val color = if (percent > 0.0) positiveColor else if (percent < 0.0) negativeColor else neutralColor
                Pair(formatted, color)
            } else {
                Pair(if (isVietnamese) "Đang lưu lũy" else "Chart builds over time", colors.mutedText)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isVietnamese) "Tỷ giá hiện tại" else "Current Value",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "1 ${rate.baseCurrency} ≈ ${String.format("%,.0f", rate.rate)} VND",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primaryText
                    )
                }

                if (rateValues.size >= 2) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(trendColor.copy(alpha = 0.12f))
                            .border(1.dp, trendColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = changeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = trendColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Graph Title
            Text(
                text = if (isVietnamese) "Biểu đồ xu thế lịch sử 7 ngày" else "7-Day Historical Progress",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Main graph card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rateValues.size < 2) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("⚡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isVietnamese) "Đang tích lũy dữ liệu biểu đồ" else "Chart builds over time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isVietnamese) "Vui lòng làm mới tỷ giá để ghi nhận thêm các điểm lịch sử." else "Snapshots accumulate daily from your refresh actions.",
                                fontSize = 9.sp,
                                color = colors.mutedText,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        DetailedRatesChart(
                            rates = rateValues,
                            color = trendColor,
                            modifier = Modifier.fillMaxSize(),
                            colors = colors
                        )
                    }
                }
            }

            if (rateValues.size >= 2) {
                Spacer(modifier = Modifier.height(10.dp))
                // Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val minText = String.format("%,.0f", rateValues.minOrNull() ?: 0.0) + " ₫"
                    val maxText = String.format("%,.0f", rateValues.maxOrNull() ?: 0.0) + " ₫"
                    val avgText = String.format("%,.0f", rateValues.average()) + " ₫"

                    Column {
                        Text(if (isVietnamese) "Thấp nhất" else "Low (7D)", fontSize = 10.sp, color = colors.mutedText)
                        Text(minText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isVietnamese) "Trung bình" else "Average", fontSize = 10.sp, color = colors.mutedText)
                        Text(avgText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isVietnamese) "Cao nhất" else "High (7D)", fontSize = 10.sp, color = colors.mutedText)
                        Text(maxText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multipliers Reference Row
            Text(
                text = if (isVietnamese) "Quy đổi nhanh" else "Quick Multipliers",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val multipliers = listOf(1, 10, 100)
                multipliers.forEach { mult ->
                    val value = rate.rate * mult
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$mult ${rate.baseCurrency}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format("%,.0f ₫", value),
                                fontSize = 11.sp,
                                color = colors.primaryAqua,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disclaimer Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${rate.provider} • Ref Only",
                    fontSize = 10.sp,
                    color = colors.mutedText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = DateUtils.formatEpoch(rate.fetchedAt, "yyyy-MM-dd HH:mm"),
                    fontSize = 10.sp,
                    color = colors.mutedText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
