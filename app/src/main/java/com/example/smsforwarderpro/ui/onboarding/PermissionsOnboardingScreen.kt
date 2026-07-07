package com.example.smsforwarderpro.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smsforwarderpro.theme.MotionSpec
import com.example.smsforwarderpro.ui.components.WavyDivider
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PermissionsOnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    // Check runtime permissions
    var hasSmsPermissions by remember { mutableStateOf(checkSmsPermissions(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }
        )
    }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(checkBatteryOptimizations(context)) }

    // Launcher for SMS permissions group
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermissions = results.values.all { it }
    }

    // Launcher for Notifications permission
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Title Accent
            Text(
                text = "SMS FORWARDER PRO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            WavyDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                waveHeight = 4.dp,
                waveLength = 16.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Onboarding pages inside HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> IntroPage()
                    1 -> PermissionsPage(
                        hasSms = hasSmsPermissions,
                        hasNotifications = hasNotificationPermission,
                        onRequestSms = {
                            smsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.RECEIVE_MMS,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                            )
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    2 -> BatteryOptimizationPage(
                        isIgnoring = isIgnoringBatteryOptimizations,
                        onRequestBattery = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to settings
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        onRefresh = {
                            isIgnoringBatteryOptimizations = checkBatteryOptimizations(context)
                        }
                    )
                }
            }

            // Indicator dots
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                    val width by animateFloatAsState(
                        targetValue = if (pagerState.currentPage == iteration) 24f else 8f,
                        animationSpec = MotionSpec.BouncySpring,
                        label = "indicatorWidth"
                    )

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .width(width.dp)
                            .height(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Button
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1,
                                animationSpec = MotionSpec.SmoothSpring
                            )
                        }
                    } else {
                        // Finish onboarding if SMS permission is granted
                        if (hasSmsPermissions) {
                            onFinished()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pagerState.currentPage == 2 && !hasSmsPermissions) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val buttonText = when (pagerState.currentPage) {
                        0 -> "Get Started"
                        1 -> "Next Step"
                        else -> if (hasSmsPermissions) "Let's Go" else "SMS Permission Required"
                    }
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun IntroPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SMS",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to SMS Forwarder Pro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This app reads incoming SMS/MMS content and can forward message text, sender, SIM slot, and timestamps to destinations you configure. Continue only if you consent to that processing on this device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionsPage(
    hasSms: Boolean,
    hasNotifications: Boolean,
    onRequestSms: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    var showDisclosure by remember { mutableStateOf(false) }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Prominent Disclosure: SMS Interception") },
            text = {
                Text(
                    "SMS Forwarder Pro collects and reads your SMS and MMS messages in the background " +
                    "to forward them to your configured destinations based on your rules, even when the app is closed. " +
                    "This is the core functionality of the app. By proceeding, you consent to this data processing."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclosure = false
                        onRequestSms()
                    }
                ) {
                    Text("I Agree")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisclosure = false }
                ) {
                    Text("Decline")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Grant these permissions to receive, read, and forward messages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // 1. SMS Permissions Card
        PermissionItem(
            title = "SMS & MMS Interception",
            desc = "RECEIVE, READ, and SEND permissions are critical to forward messages.",
            isGranted = hasSms,
            onClick = { showDisclosure = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Notification Permission Card
        PermissionItem(
            title = "Post Notifications",
            desc = "Enables sticky status notifications to prevent Android service termination.",
            isGranted = hasNotifications,
            onClick = onRequestNotifications
        )
    }
}

@Composable
fun BatteryOptimizationPage(
    isIgnoring: Boolean,
    onRequestBattery: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isIgnoring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIgnoring) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Battery Status",
                    modifier = Modifier.size(48.dp),
                    tint = if (isIgnoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Background Reliability",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Incoming SMS broadcasts wake forwarding immediately. For stricter Android skins, review this app's battery settings and allow background activity if forwarding is delayed.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isIgnoring) {
            Button(
                onClick = onRequestBattery,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open App Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRefresh) {
                Text("Verify Status")
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Battery restrictions are already relaxed for this app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    desc: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun checkSmsPermissions(context: Context): Boolean {
    val sms = checkPermission(context, Manifest.permission.RECEIVE_SMS)
    val read = checkPermission(context, Manifest.permission.READ_SMS)
    val send = checkPermission(context, Manifest.permission.SEND_SMS)
    val mms = checkPermission(context, Manifest.permission.RECEIVE_MMS)
    val phoneState = checkPermission(context, Manifest.permission.READ_PHONE_STATE)
    return sms && read && send && mms && phoneState
}

private fun checkBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
