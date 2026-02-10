package com.liandian.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Build
import android.graphics.Path

object GestureBridge {

    var accessibilityService: AccessibilityService? = null

    fun isAvailable(): Boolean = accessibilityService != null

    fun dispatchGesture(gesture: GestureDescription): Boolean {
        val service = accessibilityService ?: return false
        return service.dispatchGesture(gesture, null, null)
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
