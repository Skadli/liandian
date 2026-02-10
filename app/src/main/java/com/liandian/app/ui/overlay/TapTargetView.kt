package com.liandian.app.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class TapTargetView(context: Context) : View(context) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 244, 67, 54) // 半透明红色
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 244, 67, 54)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val sizePx = 80
    private val radius = sizePx / 2f

    var index: Int = 0
    var onDelete: (() -> Unit)? = null

    fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 400
        }
    }

    fun getCenterX(): Float {
        val params = layoutParams as? WindowManager.LayoutParams ?: return 0f
        return params.x + radius
    }

    fun getCenterY(): Float {
        val params = layoutParams as? WindowManager.LayoutParams ?: return 0f
        return params.y + radius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 圆圈
        canvas.drawCircle(radius, radius, radius - 4f, circlePaint)
        // 十字线
        canvas.drawLine(radius, 8f, radius, sizePx - 8f, crossPaint)
        canvas.drawLine(8f, radius, sizePx - 8f, radius, crossPaint)
        // 序号
        canvas.drawText("${index + 1}", radius, radius + 8f, numberPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupDrag(windowManager: WindowManager) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        setOnTouchListener { _, event ->
            val params = layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 25) isDragging = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(this, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onDelete?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
