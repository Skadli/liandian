package com.liandian.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
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
    onEditingChange: (Boolean) -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var panelMode by remember { mutableStateOf(PanelMode.TAP) }
    var loopPlayback by remember { mutableStateOf(true) }
    var isEditingInterval by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (isEditingInterval) {
                            // 先提交输入值
                            val value = editingText.toLongOrNull()
                            if (value != null && value in 1..600000) {
                                onIntervalChange(value)
                            }
                            isEditingInterval = false
                            onEditingChange(false)
                            focusManager.clearFocus()
                        }
                    }
                }
        ) {
            // 顶部拖拽手柄
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
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
                    onAddTapPoint = onAddTapPoint,
                    isEditing = isEditingInterval,
                    editText = editingText,
                    onEditTextChange = { editingText = it },
                    onEditingChange = { editing ->
                        isEditingInterval = editing
                        onEditingChange(editing)
                        if (editing) {
                            editingText = intervalMs.toString()
                        }
                    }
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
                                FilledIconButton(onClick = onStartPlayback) {
                                    Icon(Icons.Default.PlayArrow, "播放录制")
                                }
                            }
                        }
                    }
                    EngineState.RECORDING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Clear, "停止录制")
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
    onAddTapPoint: () -> Unit,
    isEditing: Boolean,
    editText: String,
    onEditTextChange: (String) -> Unit,
    onEditingChange: (Boolean) -> Unit
) {
    Text("点数: ${tapPointCount}个", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))

    val focusRequester = remember { FocusRequester() }

    val intervalDisplay = if (intervalMs >= 1000) {
        "${"%.1f".format(intervalMs / 1000f)}s"
    } else {
        "${intervalMs}ms"
    }

    fun commitEdit() {
        val value = editText.toLongOrNull()
        if (value != null && value in 1..600000) {
            onIntervalChange(value)
        }
        onEditingChange(false)
    }

    if (isEditing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("间隔: ", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = editText,
                onValueChange = onEditTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { commitEdit() }),
                singleLine = true,
                suffix = { Text("ms") }
            )
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("间隔: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = intervalDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onEditingChange(true)
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 紧凑滑块
    Slider(
        value = intervalMs.toFloat(),
        onValueChange = { onIntervalChange(it.toLong()) },
        valueRange = 1f..5000f,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    )

    // -100 / +100 微调按钮
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = { onIntervalChange((intervalMs - 100).coerceAtLeast(1)) },
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) { Text("-100", style = MaterialTheme.typography.labelSmall) }

        Text(
            text = intervalDisplay,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(
            onClick = { onIntervalChange((intervalMs + 100).coerceAtMost(600000)) },
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) { Text("+100", style = MaterialTheme.typography.labelSmall) }
    }

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
