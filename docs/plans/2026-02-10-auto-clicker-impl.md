# 连点器 App 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建一个 Android 悬浮窗连点器 App，支持快速连点和录制回放两种模式。

**Architecture:** OverlayService (前台服务) 管理悬浮窗 UI，ClickAccessibilityService 执行手势派发，GestureEngine 处理录制/回放逻辑。两个 Service 通过单例 GestureBridge 通信。

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.01.01), Material 3, Coroutines + Flow, AGP 9.0, minSdk 29

---

## Task 1: 项目脚手架 — Gradle 与 Manifest

**Files:**
- Create: `build.gradle.kts` (项目根)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/accessibility_config.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

**Step 1: 创建项目根 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "liandian"
include(":app")
```

**Step 2: 创建项目根 build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "9.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
```

**Step 3: 创建 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**Step 4: 创建 app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.liandian.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.liandian.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.01.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

**Step 5: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.LianDian">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.LianDian">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".ui.overlay.OverlayService"
            android:foregroundServiceType="specialUse" />

        <service
            android:name=".service.ClickAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>

    </application>

</manifest>
```

**Step 6: 创建 accessibility_config.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_description"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canPerformGestures="true"
    android:notificationTimeout="100" />
```

**Step 7: 创建 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">连点器</string>
    <string name="accessibility_description">连点器需要无障碍权限来执行自动点击和手势操作</string>
    <string name="overlay_notification_channel">连点器服务</string>
    <string name="overlay_notification_title">连点器运行中</string>
    <string name="overlay_notification_text">点击展开控制面板</string>
</resources>
```

**Step 8: 创建 themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.LianDian" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

**Step 9: 提交**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app/
git commit -m "chore: init project scaffold with Gradle, Manifest, and resources"
```

---

## Task 2: 数据模型层

**Files:**
- Create: `app/src/main/java/com/liandian/app/model/GestureEvent.kt`
- Create: `app/src/main/java/com/liandian/app/model/TaskMode.kt`

**Step 1: 创建 GestureEvent.kt**

```kotlin
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
```

**Step 2: 创建 TaskMode.kt**

```kotlin
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
```

**Step 3: 提交**

```bash
git add app/src/main/java/com/liandian/app/model/
git commit -m "feat: add gesture event and task mode data models"
```

---

## Task 3: GestureBridge — 服务间通信桥

**Files:**
- Create: `app/src/main/java/com/liandian/app/service/GestureBridge.kt`

**Step 1: 创建 GestureBridge.kt**

```kotlin
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
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/service/GestureBridge.kt
git commit -m "feat: add GestureBridge for service communication and gesture building"
```

---

## Task 4: ClickAccessibilityService

**Files:**
- Create: `app/src/main/java/com/liandian/app/service/ClickAccessibilityService.kt`

**Step 1: 创建 ClickAccessibilityService.kt**

```kotlin
package com.liandian.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ClickAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        GestureBridge.accessibilityService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理无障碍事件，仅用于手势派发
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        super.onDestroy()
        GestureBridge.accessibilityService = null
    }
}
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/service/ClickAccessibilityService.kt
git commit -m "feat: add ClickAccessibilityService for gesture dispatch"
```

---

## Task 5: GestureEngine — 录制器与播放器

**Files:**
- Create: `app/src/main/java/com/liandian/app/engine/GestureRecorder.kt`
- Create: `app/src/main/java/com/liandian/app/engine/GesturePlayer.kt`
- Create: `app/src/main/java/com/liandian/app/engine/GestureEngine.kt`

**Step 1: 创建 GestureRecorder.kt**

```kotlin
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
```

**Step 2: 创建 GesturePlayer.kt**

```kotlin
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
    }
}
```

**Step 3: 创建 GestureEngine.kt**

```kotlin
package com.liandian.app.engine

import com.liandian.app.model.GestureEvent
import com.liandian.app.model.TaskMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EngineState {
    IDLE, RECORDING, PLAYING
}

class GestureEngine {

    private val _state = MutableStateFlow(EngineState.IDLE)
    val state: StateFlow<EngineState> = _state

    private val recorder = GestureRecorder()
    private val player = GesturePlayer()
    private var playJob: Job? = null
    private var recordedEvents: List<GestureEvent> = emptyList()

    fun startRecording() {
        if (_state.value != EngineState.IDLE) return
        recorder.start()
        _state.value = EngineState.RECORDING
    }

    fun stopRecording() {
        if (_state.value != EngineState.RECORDING) return
        recordedEvents = recorder.stop()
        _state.value = EngineState.IDLE
    }

    fun getRecorder(): GestureRecorder = recorder

    fun getRecordedEvents(): List<GestureEvent> = recordedEvents

    fun startPlaying(mode: TaskMode, scope: CoroutineScope) {
        if (_state.value != EngineState.IDLE) return
        _state.value = EngineState.PLAYING
        playJob = scope.launch(Dispatchers.Default) {
            try {
                player.play(mode)
            } finally {
                _state.value = EngineState.IDLE
            }
        }
    }

    fun stopPlaying() {
        playJob?.cancel()
        playJob = null
    }
}
```

**Step 4: 提交**

```bash
git add app/src/main/java/com/liandian/app/engine/
git commit -m "feat: add GestureEngine with recorder and player"
```

---

## Task 6: 悬浮窗 UI — 收起态 FloatingButton

**Files:**
- Create: `app/src/main/java/com/liandian/app/ui/overlay/FloatingButton.kt`

**Step 1: 创建 FloatingButton.kt**

```kotlin
package com.liandian.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatingButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = "连点器",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/ui/overlay/FloatingButton.kt
git commit -m "feat: add FloatingButton composable for collapsed overlay state"
```

---

## Task 7: 悬浮窗 UI — 展开态 OverlayPanel

**Files:**
- Create: `app/src/main/java/com/liandian/app/ui/overlay/OverlayPanel.kt`

**Step 1: 创建 OverlayPanel.kt**

```kotlin
package com.liandian.app.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    var panelMode by remember { mutableStateOf(PanelMode.TAP) }
    var loopPlayback by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    onAddTapPoint = onAddTapPoint
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
                                    Icon(Icons.Default.FiberManualRecord, "开始录制")
                                }
                            }
                        }
                    }
                    EngineState.RECORDING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, "停止录制")
                        }
                        FilledIconButton(onClick = {}) {
                            // 录制完后播放按钮（停止录制后自动切到播放）
                            Icon(Icons.Default.PlayArrow, "播放")
                        }
                    }
                    EngineState.PLAYING -> {
                        FilledIconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, "停止")
                        }
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "关闭")
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
    onAddTapPoint: () -> Unit
) {
    Text("点数: ${tapPointCount}个", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))

    Text("间隔: ${intervalMs}ms", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = intervalMs.toFloat(),
        onValueChange = { onIntervalChange(it.toLong()) },
        valueRange = 50f..5000f,
        modifier = Modifier.fillMaxWidth()
    )

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
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/ui/overlay/OverlayPanel.kt
git commit -m "feat: add OverlayPanel composable with tap and record mode UI"
```

---

## Task 8: 悬浮窗 UI — 可拖拽靶标 TapTargetView

**Files:**
- Create: `app/src/main/java/com/liandian/app/ui/overlay/TapTargetView.kt`

**Step 1: 创建 TapTargetView.kt**

靶标使用传统 View 实现（每个靶标是独立的悬浮窗 View，方便 WindowManager 管理拖拽坐标）。

```kotlin
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

    private val deletePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 244, 67, 54)
        style = Paint.Style.FILL
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val sizePx = 80
    private val radius = sizePx / 2f

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
        // 右上角删除标记
        canvas.drawText("×", sizePx - 12f, 20f, deletePaint)
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
                        // 点击右上角区域 → 删除
                        if (event.x > sizePx * 0.7f && event.y < sizePx * 0.3f) {
                            onDelete?.invoke()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }
}
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/ui/overlay/TapTargetView.kt
git commit -m "feat: add draggable TapTargetView for tap point placement"
```

---

## Task 9: OverlayService — 悬浮窗核心服务

**Files:**
- Create: `app/src/main/java/com/liandian/app/ui/overlay/OverlayService.kt`

**Step 1: 创建 OverlayService.kt**

```kotlin
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
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/liandian/app/ui/overlay/OverlayService.kt
git commit -m "feat: add OverlayService with floating window lifecycle management"
```

---

## Task 10: 权限引导页 — PermissionScreen + MainActivity

**Files:**
- Create: `app/src/main/java/com/liandian/app/ui/permission/PermissionScreen.kt`
- Create: `app/src/main/java/com/liandian/app/MainActivity.kt`

**Step 1: 创建 PermissionScreen.kt**

```kotlin
package com.liandian.app.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onAllGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "连点器",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "需要以下权限才能正常工作",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 悬浮窗权限
        PermissionItem(
            title = "悬浮窗权限",
            description = "在其他应用上方显示控制面板",
            granted = hasOverlayPermission,
            onRequest = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 无障碍权限
        PermissionItem(
            title = "无障碍服务",
            description = "执行自动点击和手势操作",
            granted = hasAccessibilityPermission,
            onRequest = onRequestAccessibility
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (hasOverlayPermission && hasAccessibilityPermission) {
            Button(onClick = onAllGranted, modifier = Modifier.fillMaxWidth()) {
                Text("启动连点器")
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!granted) {
                TextButton(onClick = onRequest) {
                    Text("开启")
                }
            }
        }
    }
}
```

**Step 2: 创建 MainActivity.kt**

```kotlin
package com.liandian.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.liandian.app.ui.overlay.OverlayService
import com.liandian.app.ui.permission.PermissionScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var hasOverlay by remember { mutableStateOf(checkOverlayPermission()) }
                var hasAccessibility by remember { mutableStateOf(checkAccessibilityPermission()) }

                // 从设置页返回时刷新权限状态
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            hasOverlay = checkOverlayPermission()
                            hasAccessibility = checkAccessibilityPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                PermissionScreen(
                    hasOverlayPermission = hasOverlay,
                    hasAccessibilityPermission = hasAccessibility,
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestAccessibility = { requestAccessibilityPermission() },
                    onAllGranted = { launchOverlayService() }
                )
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun checkAccessibilityPermission(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabled.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun launchOverlayService() {
        startForegroundService(Intent(this, OverlayService::class.java))
        finish()
    }
}
```

**Step 3: 提交**

```bash
git add app/src/main/java/com/liandian/app/ui/permission/ app/src/main/java/com/liandian/app/MainActivity.kt
git commit -m "feat: add permission screen and MainActivity with permission flow"
```

---

## Task 11: Gradle Wrapper 与构建验证

**Step 1: 确保 Gradle Wrapper 存在**

如果项目没有 `gradlew`，需要生成：

```bash
cd E:\project\liandian
gradle wrapper --gradle-version 8.13
```

**Step 2: 运行构建验证**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 3: 修复编译问题（如有）**

根据编译输出修复导入缺失、API 不兼容等问题。

**Step 4: 提交**

```bash
git add gradle/ gradlew gradlew.bat
git commit -m "chore: add Gradle wrapper 8.13"
```

---

## 实现顺序总结

| Task | 内容 | 依赖 |
|------|------|------|
| 1 | 项目脚手架 (Gradle + Manifest + Resources) | 无 |
| 2 | 数据模型 (GestureEvent, TaskMode) | 无 |
| 3 | GestureBridge (服务通信桥) | 无 |
| 4 | ClickAccessibilityService | Task 3 |
| 5 | GestureEngine + Recorder + Player | Task 2, 3 |
| 6 | FloatingButton (收起态 UI) | 无 |
| 7 | OverlayPanel (展开态 UI) | Task 5 |
| 8 | TapTargetView (靶标) | 无 |
| 9 | OverlayService (核心服务) | Task 5, 6, 7, 8 |
| 10 | PermissionScreen + MainActivity | Task 9 |
| 11 | Gradle Wrapper + 构建验证 | Task 1-10 |
