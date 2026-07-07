package com.example.smsforwarderpro.ui.dashboard

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.theme.ColorDanger
import com.example.smsforwarderpro.theme.ColorSuccess
import com.example.smsforwarderpro.theme.ColorWarning
import com.example.smsforwarderpro.ui.components.SpringSwitch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentLogs by viewModel.recentLogs.collectAsState()
    val successCount by viewModel.successCount.collectAsState()
    val failedCount by viewModel.failedCount.collectAsState()
    val successToday by viewModel.successTodayCount.collectAsState()

    var isServiceActive by remember { mutableStateOf(viewModel.prefs.isServiceActive) }
    val isPrivacyMode = remember { viewModel.prefs.isPrivacyMode }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.triggerTestSms() },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Run Test") },
                text = { Text("Trigger Test SMS") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Hero Card: Mesh Gradient & Status Toggle
            item {
                HeroStatusCard(
                    isActive = isServiceActive,
                    onToggle = { active ->
                        isServiceActive = active
                        viewModel.toggleService(active)
                    }
                )
            }

            // 2. Stats Grid Section
            item {
                StatsRow(
                    today = successToday,
                    total = successCount,
                    failed = failedCount
                )
            }

            // 3. Live Logs Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Interceptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    if (recentLogs.isNotEmpty()) {
                        Text(
                            text = "Live Flow",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Live Logs List
            if (recentLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Waiting for incoming messages...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentLogs) { log ->
                    LogItemRow(log = log, isPrivacyMode = isPrivacyMode)
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun HeroStatusCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val gradientColors = if (isActive) {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isActive) "MONITORING ACTIVE" else "MONITORING PAUSED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isActive) "Listening for incoming SMS & MMS" else "Forwarding pipeline is stopped",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                SpringSwitch(
                    checked = isActive,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
fun StatsRow(
    today: Int,
    total: Int,
    failed: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today Success
        StatCard(
            title = "Today",
            value = today.toString(),
            color = ColorSuccess,
            modifier = Modifier.weight(1f)
        )

        // Total Success
        StatCard(
            title = "All Sent",
            value = total.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        // Total Failed
        StatCard(
            title = "Failed",
            value = failed.toString(),
            color = ColorDanger,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LogItemRow(
    log: Message,
    isPrivacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon slot representing SIM or SMS
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SIM${log.simSlot + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val timeStr = DateFormat.format("hh:mm a", Date(log.timestamp)).toString()
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val displayedBody = if (isPrivacyMode) {
                    "****************"
                } else {
                    log.body
                }

                Text(
                    text = displayedBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tag Badge
                val (tagColor, tagBg) = when (log.tag) {
                    MessageTag.OTP -> Color.White to MaterialTheme.colorScheme.primary
                    MessageTag.TRANSACTIONAL -> Color.White to ColorSuccess
                    MessageTag.PROMOTIONAL -> Color.Black to ColorWarning
                    MessageTag.MMS -> Color.White to MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
                }

                Surface(
                    color = tagBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = log.tag.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
