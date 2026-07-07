package com.example.smsforwarderpro.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.smsforwarderpro.ui.rules.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var retentionDays by remember { mutableFloatStateOf(viewModel.prefs.retentionDays.toFloat()) }
    var privacyMode by remember { mutableStateOf(viewModel.prefs.isPrivacyMode) }
    var biometricLock by remember { mutableStateOf(viewModel.prefs.isBiometricLockEnabled) }
    var hasPin by remember { mutableStateOf(viewModel.prefs.hasAppPin) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showOemDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Security & Encryption
            Text(
                text = "Security & App Lock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Privacy Mode Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Mode", fontWeight = FontWeight.Bold)
                            Text("Mask message bodies in home screen lists and status notifications.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = privacyMode,
                            onCheckedChange = {
                                privacyMode = it
                                viewModel.setPrivacyMode(it)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // PIN Lock Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Passcode Lock", fontWeight = FontWeight.Bold)
                            Text("Require a 4-digit PIN to open the application.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hasPin,
                            onCheckedChange = {
                                if (it) {
                                    showPinDialog = true
                                } else {
                                    viewModel.setAppPin(null)
                                    hasPin = false
                                }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    if (hasPin) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Biometric Lock Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Lock", fontWeight = FontWeight.Bold)
                                Text("Allow fingerprint/face unlock to bypass PIN code entry.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = biometricLock,
                                onCheckedChange = {
                                    biometricLock = it
                                    viewModel.setBiometricLock(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }

            // Section 2: Data & Logs Management
            Text(
                text = "History & Auto Cleanup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "History Retention: ${retentionDays.toInt()} Days",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Logs older than this interval are deleted automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = retentionDays,
                        onValueChange = { retentionDays = it },
                        onValueChangeFinished = {
                            viewModel.updateRetentionDays(retentionDays.toInt())
                        },
                        valueRange = 7f..365f,
                        steps = 50
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // CSV Export trigger row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExportDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Export Logs to CSV", fontWeight = FontWeight.Bold)
                            Text("Save a structured CSV log folder to share or back up.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Export")
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteAllDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Delete All App Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Clear logs, preferences, cache exports, and saved destination settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Delete all app data")
                    }
                }
            }

            // Section 3: OEM Autostart Guide
            Text(
                text = "OEM Background Optimization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOemDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "OEM Guide",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bypass OEM Battery Restrictions", fontWeight = FontWeight.Bold)
                        Text("Guided checklist to allow background auto-start and prevent system kills on customized brands (Samsung, Xiaomi, OnePlus, etc.).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Guide Details")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 1. PIN Setup Dialog
        if (showPinDialog) {
            var pinText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPinDialog = false; hasPin = false },
                title = { Text("Set 4-Digit Passcode") },
                text = {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { value ->
                            val digitsOnly = value.filter { it.isDigit() }
                            if (digitsOnly.length <= 4) pinText = digitsOnly
                        },
                        label = { Text("App Lock PIN") },
                        placeholder = { Text("e.g. 1234") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinText.length == 4) {
                                viewModel.setAppPin(pinText)
                                hasPin = true
                                showPinDialog = false
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false; hasPin = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. CSV Export Dialog with DateRangePicker
        if (showExportDialog) {
            val dateRangeState = rememberDateRangePickerState()
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Select Export Date Range") },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        DateRangePicker(
                            state = dateRangeState,
                            showModeToggle = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val start = dateRangeState.selectedStartDateMillis
                            val end = dateRangeState.selectedEndDateMillis
                            if (start != null && end != null) {
                                viewModel.exportLogsToCsv(start, end) { file ->
                                    showExportDialog = false
                                    if (file != null) {
                                        // Share file
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Logs CSV"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to share CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No logs found in selected range", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Export & Share")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Delete All App Data") },
                text = {
                    Text("This clears message logs, retry history, app settings, destination values, and cached CSV exports on this device.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllAppData { success ->
                                showDeleteAllDialog = false
                                if (success) {
                                    privacyMode = viewModel.prefs.isPrivacyMode
                                    biometricLock = viewModel.prefs.isBiometricLockEnabled
                                    hasPin = viewModel.prefs.hasAppPin
                                    retentionDays = viewModel.prefs.retentionDays.toFloat()
                                    Toast.makeText(context, "App data deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to delete app data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. OEM Guide Dialog
        if (showOemDialog) {
            AlertDialog(
                onDismissRequest = { showOemDialog = false },
                title = { Text("OEM Autostart & Sleep Limits") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Custom Android brands often restrict background services aggressively. Please follow these steps:")
                        
                        Text("Samsung Devices", fontWeight = FontWeight.Bold)
                        Text("1. Settings > Apps > SMS Forwarder Pro > Battery.\n2. Set to 'Unrestricted'.\n3. Settings > Device Care > Battery > Background usage limits > ensure SMS Forwarder Pro is under 'Never sleeping apps'.", fontSize = 11.sp)

                        Text("Xiaomi / Redmi Devices", fontWeight = FontWeight.Bold)
                        Text("1. Long-press SMS Forwarder Pro > App Info.\n2. Enable 'Autostart'.\n3. Set Battery Saver to 'No restrictions'.", fontSize = 11.sp)

                        Text("OnePlus / Oppo / Realme", fontWeight = FontWeight.Bold)
                        Text("1. Settings > Apps > App Management > SMS Forwarder Pro > Battery usage.\n2. Enable 'Allow background activity' and 'Allow auto-launch'.", fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "More detailed directions are available at dontkillmyapp.com",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com"))
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showOemDialog = false }) {
                        Text("Got It")
                    }
                }
            )
        }
    }
}
