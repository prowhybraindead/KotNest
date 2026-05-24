package com.example.feature.payments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateUtils
import com.example.domain.model.Category
import com.example.domain.model.PaymentHistory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Subscription
import com.example.feature.dashboard.StatusChip
import com.example.feature.dashboard.parseColor
import com.example.ui.theme.*
import com.example.ui.viewmodel.DueMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    viewModel: DueMateViewModel,
    subscriptionId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscriptionState = viewModel.getSubscriptionStream(subscriptionId).collectAsState(initial = null)
    val historyState = viewModel.getHistoryStream(subscriptionId).collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val ratesState by viewModel.ratesUiState.collectAsState()

    val colors = LocalKotNestColors.current

    val subscription = subscriptionState.value
    val histories = historyState.value
    val category = categories.find { it?.id == subscription?.categoryId }
    val paymentMethod = paymentMethods.find { it?.id == subscription?.paymentMethodId }
    val rates = ratesState.rates

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Dues Connection") },
            text = { Text("Are you sure you want to delete '${subscription?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscription(subscriptionId)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colors.secondaryText)
                }
            }
        )
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
                title = { Text("Dues Details", fontWeight = FontWeight.Bold, color = colors.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(subscriptionId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colors.primaryText)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.danger)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundGradientStart
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (subscription == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.deepAqua)
            }
        } else {
            val colorVal = parseColor(category?.color ?: "#4CAF50")
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Header Information Block Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(colorVal.copy(alpha = 0.12f), CircleShape)
                                    .border(1.5.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val emojiText = category?.icon ?: "💳"
                                Text(
                                    text = emojiText,
                                    fontSize = 28.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = subscription.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category?.name ?: "Other",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = String.format("%,.0f", subscription.amount) + " " + subscription.currency,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = if (colors.isLight) colors.deepAqua else colors.cyanAccent
                            )

                            val useCase = remember { com.example.domain.usecase.ConvertAmountToVndUseCase() }
                            val conversion = useCase(subscription.amount, subscription.currency, rates)
                            val showVnd = settingsState?.showEstimatedVnd != false && 
                                          !subscription.currency.equals("VND", ignoreCase = true) && 
                                          conversion.estimatedVndAmount != null
                            
                            if (showVnd) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format("≈ %,.0f VND", conversion.estimatedVndAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAqua
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            StatusChip(subscription.status)
                        }
                    }
                }

                // Quick Action buttons with maximum style consistency
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Mark as Paid Button
                        Button(
                            onClick = { viewModel.markAsPaid(subscriptionId) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("detail_mark_paid_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.deepAqua,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Mark as Paid", fontWeight = FontWeight.Bold)
                        }

                        // Pause / Resume Button
                        OutlinedButton(
                            onClick = { viewModel.togglePauseSubscription(subscriptionId) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("detail_pause_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.primaryText
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (colors.isLight) colors.deepAqua else colors.cyanAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (subscription.isPaused) "Resume" else "Pause",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Full details card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailItem(label = "Billing Cycle", value = subscription.billingCycle)
                            
                            if (subscription.billingCycle.contains("X")) {
                                DetailItem(label = "Cycle Value", value = subscription.customCycleValue.toString())
                            }
                            
                            DetailItem(label = "Next Payment Due", value = DateUtils.formatEpoch(subscription.nextDueDate))
                            
                            DetailItem(label = "Importance Level", value = subscription.importance)
                            
                            DetailItem(label = "Auto-Renewing", value = if (subscription.isAutoRenew) "Yes" else "No")

                            if (paymentMethod != null) {
                                DetailItem(label = "Saved Method", value = "${paymentMethod.name} (${paymentMethod.type})")
                            }

                            if (!subscription.managementUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border)
                                )
                                Column {
                                    Text("Management Link", fontSize = 11.sp, color = colors.mutedText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subscription.managementUrl,
                                        fontSize = 14.sp,
                                        color = colors.primaryAqua,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier
                                            .clickable {
                                                try {
                                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(subscription.managementUrl))
                                                    context.startActivity(webIntent)
                                                } catch (e: Exception) {
                                                    // fail-safe
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }

                            if (!subscription.note.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border)
                                )
                                Column {
                                    Text("Notes", fontSize = 11.sp, color = colors.mutedText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(subscription.note, fontSize = 14.sp, color = colors.primaryText)
                                }
                            }
                        }
                    }
                }

                // 3. Payment History Rows
                item {
                    Text(
                        text = "PAYMENT HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (histories.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                .padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No payment history recorded yet.", fontSize = 13.sp, color = colors.mutedText)
                            }
                        }
                    }
                } else {
                    items(histories) { history ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Paid " + DateUtils.formatEpoch(history.paidDate),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = history.note ?: "No note",
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                                Text(
                                    text = String.format("%,.0f", history.amount) + " " + history.currency,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.success
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.padding(bottom = 40.dp))
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    val colors = LocalKotNestColors.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, fontSize = 13.sp, color = colors.secondaryText)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
    }
}
