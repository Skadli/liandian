package com.liandian.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.liandian.app.ui.overlay.OverlayService
import com.liandian.app.ui.permission.PermissionScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var hasOverlay by remember { mutableStateOf(checkOverlayPermission()) }
                var hasAccessibility by remember { mutableStateOf(checkAccessibilityPermission()) }

                // 从设置页返回时刷新权限状态
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            hasOverlay = checkOverlayPermission()
                            hasAccessibility = checkAccessibilityPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                PermissionScreen(
                    hasOverlayPermission = hasOverlay,
                    hasAccessibilityPermission = hasAccessibility,
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestAccessibility = { requestAccessibilityPermission() },
                    onAllGranted = { launchOverlayService() }
                )
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun checkAccessibilityPermission(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabled.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun launchOverlayService() {
        startForegroundService(Intent(this, OverlayService::class.java))
        finish()
    }
}
