package com.liandian.app.ui.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onAllGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "连点器",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "需要以下权限才能正常工作",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 悬浮窗权限
        PermissionItem(
            title = "悬浮窗权限",
            description = "在其他应用上方显示控制面板",
            granted = hasOverlayPermission,
            onRequest = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 无障碍权限
        PermissionItem(
            title = "无障碍服务",
            description = "执行自动点击和手势操作",
            granted = hasAccessibilityPermission,
            onRequest = onRequestAccessibility
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (hasOverlayPermission && hasAccessibilityPermission) {
            Button(onClick = onAllGranted, modifier = Modifier.fillMaxWidth()) {
                Text("启动连点器")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var showInstructions by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showInstructions = !showInstructions },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showInstructions) "收起说明" else "使用说明")
        }

        AnimatedVisibility(visible = showInstructions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("连点模式", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "1. 点击「添加点」在屏幕上放置点击目标\n" +
                        "2. 拖动目标可调整位置，点击右上角 × 可删除\n" +
                        "3. 滑动滑块设置点击间隔时间\n" +
                        "4. 点击播放按钮开始自动连点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("录制模式", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "1. 切换到「录制」标签页\n" +
                        "2. 点击录制按钮开始录制手势操作\n" +
                        "3. 在屏幕上执行点击、长按、滑动等操作\n" +
                        "4. 点击停止按钮结束录制\n" +
                        "5. 点击播放按钮回放录制的操作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("悬浮球操作", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• 单击悬浮球：展开控制面板\n" +
                        "• 拖动悬浮球：移动位置，松手自动贴边\n" +
                        "• 拖到屏幕底部：退出连点器服务\n" +
                        "• 控制面板中点击退出按钮：退出服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!granted) {
                TextButton(onClick = onRequest) {
                    Text("开启")
                }
            }
        }
    }
}
