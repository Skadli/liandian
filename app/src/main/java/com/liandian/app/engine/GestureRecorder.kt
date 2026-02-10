package com.liandian.app.engine

import android.view.MotionEvent
import com.liandian.app.model.GestureEvent
import kotlin.math.hypot

class GestureRecorder {

    private val events = mutableListOf<GestureEvent>()
    private var startTime = 0L
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f

    companion object {
        private const val TAP_TIMEOUT = 300L
        private const val TOUCH_SLOP = 10f
    }

    fun start() {
        events.clear()
        startTime = System.currentTimeMillis()
    }

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                val upTime = System.currentTimeMillis()
                val upX = event.rawX
                val upY = event.rawY
                val elapsed = upTime - downTime
                val distance = hypot((upX - downX).toDouble(), (upY - downY).toDouble()).toFloat()
                val timestamp = downTime - startTime

                val gestureEvent = when {
                    distance >= TOUCH_SLOP -> GestureEvent.Swipe(
                        timestamp = timestamp,
                        x = downX,
                        y = downY,
                        endX = upX,
                        endY = upY,
                        duration = elapsed
                    )
                    elapsed >= TAP_TIMEOUT -> GestureEvent.LongPress(
                        timestamp = timestamp,
                        x = downX,
                        y = downY,
                        duration = elapsed
                    )
                    else -> GestureEvent.Tap(
                        timestamp = timestamp,
                        x = downX,
                        y = downY
                    )
                }
                events.add(gestureEvent)
            }
        }
    }

    fun stop(): List<GestureEvent> = events.toList()
}
