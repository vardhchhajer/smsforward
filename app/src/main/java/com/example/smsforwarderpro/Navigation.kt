package com.example.smsforwarderpro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.ui.lock.AppLockScreen
import com.example.smsforwarderpro.ui.main.MainScreen
import com.example.smsforwarderpro.ui.onboarding.PermissionsOnboardingScreen

@Composable
fun MainNavigation(prefs: EncryptedPreferencesManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLocked by remember { mutableStateOf(prefs.hasAppPin) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && prefs.hasAppPin) {
                isLocked = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isLocked && prefs.hasAppPin) {
        AppLockScreen(
            prefs = prefs,
            onUnlocked = { isLocked = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Determine initial route dynamically on startup
    val initialKey = if (!checkPermissionsGranted(context)) Onboarding else Main
    val backStack = rememberNavBackStack(initialKey)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                (context as? Activity)?.finish()
            }
        },
        entryProvider = entryProvider {
            entry<AppLock> {
                AppLockScreen(
                    prefs = prefs,
                    onUnlocked = {
                        // Clear backstack to prevent going back to Lock
                        if (checkPermissionsGranted(context)) {
                            backStack.add(Main)
                        } else {
                            backStack.add(Onboarding)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Onboarding> {
                PermissionsOnboardingScreen(
                    onFinished = {
                        backStack.add(Main)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}

private fun checkPermissionsGranted(context: Context): Boolean {
    val sms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    val send = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    val mms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) == PackageManager.PERMISSION_GRANTED
    val phoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    return sms && read && send && mms && phoneState
}
