package com.liandian.app.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liandian.app.engine.EngineState

enum class PanelMode { TAP, RECORD }

@Composable
fun OverlayPanel(
    engineState: EngineState,
    tapPointCount: Int,
    intervalMs: Long,
    onIntervalChange: (Long) -> Unit,
    onAddTapPoint: () -> Unit,
    onStartTap: () -> Unit,
    onStartRecord: () -> Unit,
    onStartPlayback: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panelMode by remember { mutableStateOf(PanelMode.TAP) }
    var loopPlayback by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab 切换
            TabRow(
                selectedTabIndex = if (panelMode == PanelMode.TAP) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = panelMode == PanelMode.TAP,
                    onClick = { panelMode = PanelMode.TAP },
                    text = { Text("连点") }
                )
                Tab(
                    selected = panelMode == PanelMode.RECORD,
                    onClick = { panelMode = PanelMode.RECORD },
                    text = { Text("录制") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (panelMode) {
                PanelMode.TAP -> TapModeContent(
                    tapPointCount = tapPointCount,
                    intervalMs = intervalMs,
                    onIntervalChange = onIntervalChange,
                    onAddTapPoint = onAddTapPoint
                )
                PanelMode.RECORD -> RecordModeContent(
                    loopPlayback = loopPlayback,
                    onLoopChange = { loopPlayback = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (engineState) {
                    EngineState.IDLE -> {
                        when (panelMode) {
                            PanelMode.TAP -> {
                                FilledIconButton(onClick = onStartTap) {
                                    Icon(Icons.Default.PlayArrow, "开始连点")
                                }
                            }
                            PanelMode.RECORD -> {
                                FilledIconButton(
                                    onClick = onStartRecord,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Create, "开始录制")
                                }
                            }
                        }
                    }
                    EngineState.RECORDING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Clear, "停止录制")
                        }
                        FilledIconButton(onClick = {}) {
                            // 录制完后播放按钮（停止录制后自动切到播放）
                            Icon(Icons.Default.PlayArrow, "播放")
                        }
                    }
                    EngineState.PLAYING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Clear, "停止")
                        }
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "收起")
                }
            }
        }
    }
}

@Composable
private fun TapModeContent(
    tapPointCount: Int,
    intervalMs: Long,
    onIntervalChange: (Long) -> Unit,
    onAddTapPoint: () -> Unit
) {
    Text("点数: ${tapPointCount}个", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val intervalDisplay = if (intervalMs >= 1000) {
        "${"%.1f".format(intervalMs / 1000f)}s"
    } else {
        "${intervalMs}ms"
    }

    if (isEditing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("间隔: ", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    val value = editText.toLongOrNull()
                    if (value != null && value in 1..600000) {
                        onIntervalChange(value)
                    }
                    isEditing = false
                    focusManager.clearFocus()
                }),
                singleLine = true,
                suffix = { Text("ms") }
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("间隔: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = intervalDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    editText = intervalMs.toString()
                    isEditing = true
                }
            )
        }
    }
    Slider(
        value = intervalMs.toFloat(),
        onValueChange = { onIntervalChange(it.toLong()) },
        valueRange = 1f..5000f,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(4.dp))

    OutlinedButton(
        onClick = onAddTapPoint,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("添加点")
    }
}

@Composable
private fun RecordModeContent(
    loopPlayback: Boolean,
    onLoopChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("循环播放", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = loopPlayback, onCheckedChange = onLoopChange)
    }
}
