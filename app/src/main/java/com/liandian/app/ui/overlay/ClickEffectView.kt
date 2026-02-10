package com.liandian.app.ui.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator

class ClickEffectView(context: Context) : View(context) {

    var windowManager: WindowManager? = null

    private val maxRadius = 40f
    private var currentRadius = 0f
    private var currentAlpha = 255

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 33, 150, 243)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 33, 150, 243)
        style = Paint.Style.FILL
    }

    fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                currentRadius = maxRadius * fraction
                currentAlpha = (255 * (1f - fraction)).toInt()
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    post {
                        try {
                            windowManager?.removeView(this@ClickEffectView)
                        } catch (_: Exception) {}
                    }
                }
            })
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        fillPaint.alpha = (currentAlpha * 0.3f).toInt()
        canvas.drawCircle(cx, cy, currentRadius, fillPaint)
        paint.alpha = currentAlpha
        canvas.drawCircle(cx, cy, currentRadius, paint)
    }
}
