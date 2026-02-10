package com.liandian.app.ui.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.liandian.app.engine.EngineState
import com.liandian.app.engine.GestureEngine
import com.liandian.app.model.TapConfig
import com.liandian.app.model.TapPoint
import com.liandian.app.model.TaskMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private val engine = GestureEngine()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 悬浮窗视图
    private var floatingButtonView: View? = null
    private var panelView: View? = null
    private var recordOverlayView: View? = null
    private val targetViews = mutableListOf<TapTargetView>()

    // UI 状态
    private val isExpanded = MutableStateFlow(false)
    private val intervalMs = MutableStateFlow(500L)
    private val tapPointCount = MutableStateFlow(0)

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        showFloatingButton()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeAllViews()
        serviceScope.cancel()
        super.onDestroy()
    }

    // region 悬浮窗管理

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButton() {
        val container = FrameLayout(this)
        val composeView = ComposeView(this).also {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }
        container.addView(composeView)

        composeView.setContent {
            MaterialTheme {
                FloatingButton()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // 拖拽 + 点击逻辑
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
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
                    if (dx * dx + dy * dy > 100) isDragging = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 单击 → 展开面板
                        removeFloatingButton()
                        showPanel()
                    } else {
                        // 贴边吸附
                        val screenWidth = resources.displayMetrics.widthPixels
                        params.x = if (params.x < screenWidth / 2) 0 else screenWidth - 48
                        windowManager.updateViewLayout(container, params)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(container, params)
        floatingButtonView = container
    }

    private fun showPanel() {
        val container = FrameLayout(this)
        val composeView = ComposeView(this).also {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }
        container.addView(composeView)

        composeView.setContent {
            val engineState by engine.state.collectAsState()
            val interval by intervalMs.collectAsState()
            val pointCount by tapPointCount.collectAsState()

            MaterialTheme {
                OverlayPanel(
                    engineState = engineState,
                    tapPointCount = pointCount,
                    intervalMs = interval,
                    onIntervalChange = { intervalMs.value = it },
                    onAddTapPoint = { addTapTarget() },
                    onStartTap = { startQuickTap() },
                    onStartRecord = { startRecording() },
                    onStartPlayback = { startPlayback() },
                    onStop = { stopEngine() },
                    onClose = { closePanelAndShow() }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(container, params)
        panelView = container
    }

    private fun showWorkingIndicator() {
        // 最小化为半透明小圆点，单击停止
        val view = View(this).apply {
            setBackgroundColor(android.graphics.Color.argb(128, 244, 67, 54))
        }
        val size = (32 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        view.setOnClickListener { stopEngine() }
        windowManager.addView(view, params)
        // 暂存到 recordOverlayView 复用
        recordOverlayView = view
    }

    // endregion

    // region 引擎操作

    private fun addTapTarget() {
        val target = TapTargetView(this)
        val params = target.createLayoutParams()
        target.setupDrag(windowManager)
        target.onDelete = {
            windowManager.removeView(target)
            targetViews.remove(target)
            tapPointCount.value = targetViews.size
        }
        windowManager.addView(target, params)
        targetViews.add(target)
        tapPointCount.value = targetViews.size
    }

    private fun startQuickTap() {
        val points = targetViews.map { TapPoint(it.getCenterX(), it.getCenterY()) }
        if (points.isEmpty()) return
        val config = TapConfig(points, intervalMs.value)
        removePanel()
        showWorkingIndicator()
        engine.startPlaying(TaskMode.QuickTap(config), serviceScope)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startRecording() {
        removePanel()
        hideAllTargets()
        engine.startRecording()

        // 添加全屏半透明触摸捕获层
        val overlay = View(this)
        overlay.setBackgroundColor(android.graphics.Color.argb(30, 0, 0, 0))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        overlay.setOnTouchListener { _, event ->
            engine.getRecorder().onTouchEvent(event)
            // 长按和滑动需要消费事件，Tap 穿透
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_UP -> {
                    // 不消费，让事件穿透到下层
                    true
                }
                else -> true
            }
        }

        windowManager.addView(overlay, params)
        recordOverlayView = overlay
        showWorkingIndicator()
    }

    private fun startPlayback() {
        val events = engine.getRecordedEvents()
        if (events.isEmpty()) return
        removePanel()
        showWorkingIndicator()
        engine.startPlaying(TaskMode.Recording(events, loop = true), serviceScope)
    }

    private fun stopEngine() {
        when (engine.state.value) {
            EngineState.RECORDING -> {
                engine.stopRecording()
                removeRecordOverlay()
                showPanel()
            }
            EngineState.PLAYING -> {
                engine.stopPlaying()
                removeRecordOverlay()
                showPanel()
            }
            EngineState.IDLE -> {}
        }
    }

    // endregion

    // region 视图清理

    private fun removeFloatingButton() {
        floatingButtonView?.let { windowManager.removeView(it) }
        floatingButtonView = null
    }

    private fun removePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    private fun removeRecordOverlay() {
        recordOverlayView?.let { windowManager.removeView(it) }
        recordOverlayView = null
    }

    private fun hideAllTargets() {
        targetViews.forEach { it.visibility = View.GONE }
    }

    private fun showAllTargets() {
        targetViews.forEach { it.visibility = View.VISIBLE }
    }

    private fun closePanelAndShow() {
        removePanel()
        showFloatingButton()
    }

    private fun removeAllViews() {
        removeFloatingButton()
        removePanel()
        removeRecordOverlay()
        targetViews.forEach { windowManager.removeView(it) }
        targetViews.clear()
    }

    // endregion

    // region 通知

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.liandian.app.R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.liandian.app.R.string.overlay_notification_title))
            .setContentText(getString(com.liandian.app.R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    // endregion
}
