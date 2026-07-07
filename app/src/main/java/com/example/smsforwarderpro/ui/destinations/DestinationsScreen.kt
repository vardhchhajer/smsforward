package com.example.smsforwarderpro.ui.destinations

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarderpro.theme.ColorSuccess
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsScreen(
    viewModel: DestinationsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf<ChannelType?>(null) }
    
    // Refresh parameters dynamically from preferences
    var isTelegramEnabled by remember { mutableStateOf(viewModel.prefs.isTelegramEnabled) }
    var isWhatsappEnabled by remember { mutableStateOf(viewModel.prefs.isWhatsappEnabled) }
    var isWebhookEnabled by remember { mutableStateOf(viewModel.prefs.isWebhookEnabled) }
    var isGmailEnabled by remember { mutableStateOf(viewModel.prefs.isGmailEnabled) }
    var isSmsEnabled by remember { mutableStateOf(viewModel.prefs.isSmsEnabled) }

    val channels = listOf(
        ChannelItem(ChannelType.TELEGRAM, "Telegram Bot", Icons.Default.Send, listOf(Color(0xFF0088CC), Color(0xFF00A2ED)), isTelegramEnabled),
        ChannelItem(ChannelType.WHATSAPP, "WhatsApp Cloud", Icons.Default.PhoneAndroid, listOf(Color(0xFF25D366), Color(0xFF128C7E)), isWhatsappEnabled),
        ChannelItem(ChannelType.WEBHOOK, "Custom Webhook", Icons.Default.Language, listOf(Color(0xFF6C5DD3), Color(0xFF8B5CF6)), isWebhookEnabled),
        ChannelItem(ChannelType.GMAIL, "Gmail API", Icons.Default.Email, listOf(Color(0xFFEA4335), Color(0xFFC5221F)), isGmailEnabled),
        ChannelItem(ChannelType.SMS, "Secondary SMS", Icons.Default.Sms, listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)), isSmsEnabled)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Destinations",
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
        ) {
            Text(
                text = "Select a pipeline channel below to configure its API keys and delivery status.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Channels Horizontal List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(channels) { channel ->
                    ChannelCard(
                        item = channel,
                        onClick = { selectedChannel = channel.type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Show selected editor or summary helper
            if (selectedChannel == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "← Click a pipeline card above to edit credentials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    when (selectedChannel) {
                        ChannelType.TELEGRAM -> TelegramEditor(
                            viewModel = viewModel,
                            onSaved = {
                                isTelegramEnabled = viewModel.prefs.isTelegramEnabled
                                selectedChannel = null
                            },
                            onClose = { selectedChannel = null }
                        )
                        ChannelType.WHATSAPP -> WhatsAppEditor(
                            viewModel = viewModel,
                            onSaved = {
                                isWhatsappEnabled = viewModel.prefs.isWhatsappEnabled
                                selectedChannel = null
                            },
                            onClose = { selectedChannel = null }
                        )
                        ChannelType.WEBHOOK -> WebhookEditor(
                            viewModel = viewModel,
                            onSaved = {
                                isWebhookEnabled = viewModel.prefs.isWebhookEnabled
                                selectedChannel = null
                            },
                            onClose = { selectedChannel = null }
                        )
                        ChannelType.GMAIL -> GmailEditor(
                            viewModel = viewModel,
                            onSaved = {
                                isGmailEnabled = viewModel.prefs.isGmailEnabled
                                selectedChannel = null
                            },
                            onClose = { selectedChannel = null }
                        )
                        ChannelType.SMS -> SmsEditor(
                            viewModel = viewModel,
                            onSaved = {
                                isSmsEnabled = viewModel.prefs.isSmsEnabled
                                selectedChannel = null
                            },
                            onClose = { selectedChannel = null }
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

enum class ChannelType {
    TELEGRAM, WHATSAPP, WEBHOOK, GMAIL, SMS
}

data class ChannelItem(
    val type: ChannelType,
    val name: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val isEnabled: Boolean
)

@Composable
fun ChannelCard(
    item: ChannelItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(item.gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (item.isEnabled) ColorSuccess else Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.isEnabled) "ACTIVE" else "DISABLED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun TelegramEditor(
    viewModel: DestinationsViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    val defaultBotToken = "8615658203:AAGnV97L9V9fiCVHGuPkuyUp6tUS1zxebkg"
    var chatId by remember { mutableStateOf(viewModel.prefs.telegramChatId ?: "") }
    var enabled by remember { mutableStateOf(viewModel.prefs.isTelegramEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        EditorHeader("Configure Telegram Bot", onClose)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Pre-configured to send via Telegram Bot @smsforwarderbyvcbot. Please start a chat with the bot first, then enter your Telegram User ID / Chat ID below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = it },
                label = { Text("Telegram User ID / Chat ID") },
                placeholder = { Text("e.g. 987654321 or group ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enable Telegram Pipeline", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateTelegram(defaultBotToken, chatId, enabled)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
fun WhatsAppEditor(
    viewModel: DestinationsViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    var phoneId by remember { mutableStateOf(viewModel.prefs.whatsappPhoneNumberId ?: "") }
    var token by remember { mutableStateOf(viewModel.prefs.whatsappAccessToken ?: "") }
    var recipient by remember { mutableStateOf(viewModel.prefs.whatsappRecipientPhone ?: "") }
    var enabled by remember { mutableStateOf(viewModel.prefs.isWhatsappEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        EditorHeader("Configure WhatsApp Cloud API", onClose)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = phoneId,
                onValueChange = { phoneId = it },
                label = { Text("Phone Number ID") },
                placeholder = { Text("e.g. 1092837482910") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("System User Access Token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient Phone Number") },
                placeholder = { Text("e.g. +1234567890") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enable WhatsApp Pipeline", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateWhatsApp(phoneId, token, recipient, enabled)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
fun WebhookEditor(
    viewModel: DestinationsViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(viewModel.prefs.webhookUrl ?: "") }
    var method by remember { mutableStateOf(viewModel.prefs.webhookMethod) }
    var headers by remember { mutableStateOf(viewModel.prefs.webhookHeaders ?: "") }
    var template by remember { mutableStateOf(viewModel.prefs.webhookTemplate ?: "") }
    var authType by remember { mutableStateOf(viewModel.prefs.webhookAuthType) }
    var authValue by remember { mutableStateOf(viewModel.prefs.webhookAuthValue ?: "") }
    var enabled by remember { mutableStateOf(viewModel.prefs.isWebhookEnabled) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        EditorHeader("Configure Custom Webhook", onClose)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Webhook URL") },
                placeholder = { Text("e.g. https://api.myserver.com/sms") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // HTTP Method selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("HTTP Method:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == "POST", onClick = { method = "POST" })
                    Text("POST")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == "PUT", onClick = { method = "PUT" })
                    Text("PUT")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = headers,
                onValueChange = { headers = it },
                label = { Text("Headers (JSON Format)") },
                placeholder = { Text("e.g. {\"X-Api-Key\": \"secret\"}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text("Payload Template") },
                placeholder = { Text("e.g. {\"text\": \"{{body}}\"}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Webhook auth selection
            Text("Authentication Scheme:", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("NONE", "BEARER", "BASIC", "API_KEY").forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == type, onClick = { authType = type })
                        Text(type, fontSize = 11.sp)
                    }
                }
            }
            if (authType != "NONE") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = authValue,
                    onValueChange = { authValue = it },
                    label = { Text("Auth Key / Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            validationError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enable Webhook Pipeline", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val error = viewModel.validateWebhookConfig(
                        url = url,
                        headers = headers.ifBlank { null },
                        authType = authType,
                        authValue = authValue.ifBlank { null },
                        enabled = enabled
                    )
                    if (error != null) {
                        validationError = error
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    validationError = null
                    viewModel.updateWebhook(url, method, headers.ifBlank { null }, template.ifBlank { null }, authType, authValue.ifBlank { null }, enabled)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
fun GmailEditor(
    viewModel: DestinationsViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    var recipient by remember { mutableStateOf(viewModel.prefs.gmailRecipient ?: "") }
    var enabled by remember { mutableStateOf(viewModel.prefs.isGmailEnabled) }
    val isConnected = viewModel.prefs.gmailRefreshToken != null

    val context = LocalContext.current
    val authService = remember { AuthorizationService(context) }
    
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val authResponse = AuthorizationResponse.fromIntent(result.data!!)
            if (authResponse != null) {
                authService.performTokenRequest(authResponse.createTokenExchangeRequest()) { tokenResponse, _ ->
                    if (tokenResponse != null) {
                        viewModel.saveGmailTokens(
                            tokenResponse.accessToken ?: "",
                            tokenResponse.refreshToken,
                            tokenResponse.accessTokenExpirationTime ?: 0L
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        EditorHeader("Configure Email Destination", onClose)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (isConnected) {
                Text(
                    text = "✓ Google Account Connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorSuccess,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = { viewModel.disconnectGmail() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect Account")
                }
            } else {
                Text(
                    text = "Sign in with Google to allow the app to send emails on your behalf via the secure Gmail API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = {
                        val serviceConfig = AuthorizationServiceConfiguration(
                            Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                            Uri.parse("https://oauth2.googleapis.com/token")
                        )
                        val authRequest = AuthorizationRequest.Builder(
                            serviceConfig,
                            "527309217132-oipqgtoqcnrr7naldtq2gtuginei8avb.apps.googleusercontent.com",
                            ResponseTypeValues.CODE,
                            Uri.parse("com.example.smsforwarderpro:/oauth2redirect")
                        ).setScope("https://www.googleapis.com/auth/gmail.send")
                         .build()

                        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                        authLauncher.launch(authIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Connect with Google")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient Email Address") },
                placeholder = { Text("e.g. receiver@gmail.com") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enable Email Pipeline", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateGmail(
                        recipient = recipient,
                        enabled = enabled
                    )
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
fun SmsEditor(
    viewModel: DestinationsViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    var recipient by remember { mutableStateOf(viewModel.prefs.smsRecipient ?: "") }
    var simSlot by remember { mutableIntStateOf(viewModel.prefs.smsSimSlot) }
    var enabled by remember { mutableStateOf(viewModel.prefs.isSmsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        EditorHeader("Configure SMS Forwarding", onClose)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient Phone Number") },
                placeholder = { Text("e.g. +1234567890") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val activeSims = remember { viewModel.getActiveSimCards() }
            
            Text("Select Outgoing SIM / Number:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (activeSims.isNotEmpty()) {
                activeSims.forEach { sim ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { simSlot = sim.slotIndex }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = simSlot == sim.slotIndex,
                            onClick = { simSlot = sim.slotIndex }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${sim.carrierName} (SIM Slot ${sim.slotIndex + 1})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!sim.phoneNumber.isNullOrBlank()) {
                                Text(
                                    text = sim.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = simSlot == 0, onClick = { simSlot = 0 })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIM Slot 1")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = simSlot == 1, onClick = { simSlot = 1 })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIM Slot 2")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enable SMS Forwarding", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.updateSms(recipient, simSlot, enabled)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
        }
    }
}
