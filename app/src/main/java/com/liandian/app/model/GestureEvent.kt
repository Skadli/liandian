package com.liandian.app.model

sealed class GestureEvent {
    abstract val timestamp: Long
    abstract val x: Float
    abstract val y: Float

    data class Tap(
        override val timestamp: Long,
        override val x: Float,
        override val y: Float
    ) : GestureEvent()

    data class LongPress(
        override val timestamp: Long,
        override val x: Float,
        override val y: Float,
        val duration: Long
    ) : GestureEvent()

    data class Swipe(
        override val timestamp: Long,
        override val x: Float,
        override val y: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long
    ) : GestureEvent()
}
