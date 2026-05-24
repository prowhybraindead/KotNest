package com.example.feature.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalKotNestColors
import com.example.ui.viewmodel.DueMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: DueMateViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKotNestColors.current
    val state by viewModel.aiChatUiState.collectAsStateWithLifecycle()
    val backendStatus by viewModel.backendStatusUiState.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(false) }
    var providerMenuOpen by remember { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    val providerList = state.providerModels.keys.toList()
    val modelList = state.providerModels[state.selectedProvider].orEmpty()

    val transition = rememberInfiniteTransition(label = "ai_input_border_transition")
    val borderMotion by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_input_border_motion"
    )

    LaunchedEffect(Unit) {
        viewModel.refreshBackendHealth()
        viewModel.refreshAiModelConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.brandName,
                            fontWeight = FontWeight.Black,
                            color = colors.primaryText
                        )
                        Text(
                            text = "AI Finance Assistant",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.primaryText
                        )
                    }
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.backgroundGradientStart, colors.backgroundGradientEnd)
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        color = when {
                                            backendStatus.isChecking -> colors.warning
                                            backendStatus.isOnline -> colors.success
                                            else -> colors.danger
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = when {
                                    backendStatus.isChecking -> "Checking backend"
                                    backendStatus.isOnline -> "Backend connected"
                                    else -> "Backend unavailable"
                                },
                                color = colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (backendStatus.latencyMs != null && backendStatus.isOnline) {
                                Text(
                                    text = "${backendStatus.latencyMs}ms",
                                    color = colors.secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.refreshBackendHealth() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh backend",
                                    tint = colors.primaryAqua
                                )
                            }
                            IconButton(onClick = { showControls = !showControls }) {
                                Icon(
                                    imageVector = if (showControls) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle controls",
                                    tint = colors.primaryAqua
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model: ${state.selectedModel}",
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Controls",
                                color = colors.mutedText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = showControls) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                            .clickable { providerMenuOpen = true },
                                        colors = CardDefaults.cardColors(containerColor = colors.glassCard)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.selectedProvider.uppercase(),
                                                color = colors.primaryText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = colors.primaryAqua
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = providerMenuOpen,
                                        onDismissRequest = { providerMenuOpen = false }
                                    ) {
                                        providerList.forEach { provider ->
                                            DropdownMenuItem(
                                                text = { Text(provider) },
                                                onClick = {
                                                    providerMenuOpen = false
                                                    viewModel.selectAiProvider(provider)
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                            .clickable { modelMenuOpen = true },
                                        colors = CardDefaults.cardColors(containerColor = colors.glassCard)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.selectedModel,
                                                color = colors.primaryText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = colors.primaryAqua
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = modelMenuOpen,
                                        onDismissRequest = { modelMenuOpen = false }
                                    ) {
                                        modelList.forEach { model ->
                                            DropdownMenuItem(
                                                text = { Text(model) },
                                                onClick = {
                                                    modelMenuOpen = false
                                                    viewModel.selectAiModel(model)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trusted Web Search",
                                    color = colors.secondaryText,
                                    fontSize = 12.sp
                                )
                                Switch(
                                    checked = state.enableWebSearch,
                                    onCheckedChange = { viewModel.setAiWebSearchEnabled(it) }
                                )
                            }

                            if (state.rulesVersion.isNotBlank()) {
                                Text(
                                    text = "Rules: ${state.rulesVersion}",
                                    color = colors.mutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages) { turn ->
                        val isUser = turn.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) colors.glassCard else colors.elevatedCard
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .border(
                                        1.dp,
                                        if (isUser) colors.primaryAqua.copy(alpha = 0.35f) else colors.border,
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isUser) Icons.Default.Check else Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = if (isUser) colors.primaryAqua else colors.cyanAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isUser) "You" else state.brandName,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = turn.text,
                                        color = colors.primaryText,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )

                                    if (turn.filtered) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = colors.warning,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Filtered by safety policy",
                                                color = colors.warning,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    turn.citations.forEach { citation ->
                                        Text(
                                            text = "[${citation.source}] ${citation.title}\n${citation.url}",
                                            color = colors.secondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.isSending) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = colors.elevatedCard),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.primaryAqua
                                        )
                                        Text(
                                            text = "Nesty is thinking...",
                                            color = colors.secondaryText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!state.errorMessage.isNullOrBlank()) {
                Text(
                    text = state.errorMessage!!,
                    color = colors.warning,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.warning.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val animatedBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        colors.primaryAqua,
                        colors.success,
                        colors.warning,
                        colors.danger,
                        colors.primaryAqua
                    ),
                    start = Offset(0f + (920f * borderMotion), 0f),
                    end = Offset(1220f + (920f * borderMotion), 1220f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (state.isSending) 2.2.dp else 1.2.dp,
                            brush = if (state.isSending) {
                                animatedBorderBrush
                            } else {
                                Brush.linearGradient(listOf(colors.border, colors.border))
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(2.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Type in your language...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        enabled = !state.isSending,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.surface,
                            unfocusedBorderColor = colors.surface,
                            disabledBorderColor = colors.surface
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendAiChatMessage(text)
                            input = ""
                        }
                    },
                    enabled = !state.isSending
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primaryAqua
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = colors.primaryAqua
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
