package com.liandian.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GestureBridge {

    @Volatile
    var accessibilityService: AccessibilityService? = null

    fun isAvailable(): Boolean = accessibilityService != null

    suspend fun dispatchGesture(gesture: GestureDescription): Boolean {
        val service = accessibilityService ?: return false
        return suspendCoroutine { cont ->
            val dispatched = service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        cont.resume(true)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        cont.resume(false)
                    }
                },
                null
            )
            if (!dispatched) {
                cont.resume(false)
            }
        }
    }

    fun buildTap(x: Float, y: Float): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun buildLongPress(x: Float, y: Float, duration: Long): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun buildSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long
    ): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }
}
