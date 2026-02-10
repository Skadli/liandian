package com.liandian.app.model

data class TapPoint(
    val x: Float,
    val y: Float
)

data class TapConfig(
    val points: List<TapPoint>,
    val intervalMs: Long
)

sealed class TaskMode {
    data class QuickTap(val config: TapConfig) : TaskMode()
    data class Recording(val events: List<GestureEvent>, val loop: Boolean) : TaskMode()
}
