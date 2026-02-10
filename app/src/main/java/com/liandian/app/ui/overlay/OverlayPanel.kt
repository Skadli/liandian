package com.liandian.app.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                                    Icon(Icons.Default.FiberManualRecord, "开始录制")
                                }
                            }
                        }
                    }
                    EngineState.RECORDING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, "停止录制")
                        }
                        FilledIconButton(onClick = {}) {
                            // 录制完后播放按钮（停止录制后自动切到播放）
                            Icon(Icons.Default.PlayArrow, "播放")
                        }
                    }
                    EngineState.PLAYING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, "停止")
                        }
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "关闭")
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

    Text("间隔: ${intervalMs}ms", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = intervalMs.toFloat(),
        onValueChange = { onIntervalChange(it.toLong()) },
        valueRange = 50f..5000f,
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
