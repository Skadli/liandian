package com.liandian.app.engine

import com.liandian.app.model.GestureEvent
import com.liandian.app.model.TapConfig
import com.liandian.app.model.TaskMode
import com.liandian.app.service.GestureBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class GesturePlayer {

    var onGestureDispatched: ((Float, Float) -> Unit)? = null

    suspend fun play(mode: TaskMode) {
        when (mode) {
            is TaskMode.QuickTap -> playQuickTap(mode.config)
            is TaskMode.Recording -> playRecording(mode.events, mode.loop)
        }
    }

    private suspend fun playQuickTap(config: TapConfig) {
        if (config.points.isEmpty()) return
        while (true) {
            for (point in config.points) {
                coroutineContext.ensureActive()
                val gesture = GestureBridge.buildTap(point.x, point.y)
                GestureBridge.dispatchGesture(gesture)
                onGestureDispatched?.invoke(point.x, point.y)
                delay(config.intervalMs)
            }
        }
    }

    private suspend fun playRecording(events: List<GestureEvent>, loop: Boolean) {
        if (events.isEmpty()) return
        do {
            var previousTimestamp = 0L
            for (event in events) {
                coroutineContext.ensureActive()
                val wait = event.timestamp - previousTimestamp
                if (wait > 0) delay(wait)
                dispatchEvent(event)
                previousTimestamp = event.timestamp
            }
        } while (loop)
    }

    private fun dispatchEvent(event: GestureEvent) {
        val gesture = when (event) {
            is GestureEvent.Tap -> GestureBridge.buildTap(event.x, event.y)
            is GestureEvent.LongPress -> GestureBridge.buildLongPress(event.x, event.y, event.duration)
            is GestureEvent.Swipe -> GestureBridge.buildSwipe(
                event.x, event.y, event.endX, event.endY, event.duration
            )
        }
        GestureBridge.dispatchGesture(gesture)
        onGestureDispatched?.invoke(event.x, event.y)
    }
}
