# 连点器 App 设计文档

## 概述

一款 Android 悬浮窗连点器工具，支持两种工作模式：快速连点和录制回放。用户可在任意 App/网页上通过悬浮控件设置点击位置和频率，实现自动化操作。

- **最低版本**: Android 10 (API 29)
- **技术栈**: Kotlin + Jetpack Compose + Material 3
- **包名**: com.liandian.app

---

## 一、核心技术架构

### 关键技术机制

- **无障碍服务 (AccessibilityService)** — 通过 `dispatchGesture()` 在其他 App 上执行点击/滑动/长按
- **悬浮窗 (SYSTEM_ALERT_WINDOW)** — 在其他 App 上层显示控制面板和靶标

### 整体架构

```
┌─────────────────────────────────────────────┐
│                   App Layer                  │
│  MainActivity (权限引导 + 简单说明页)          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              Service Layer                   │
│  ┌─────────────────┐  ┌──────────────────┐  │
│  │  OverlayService  │  │ ClickAccessibility│ │
│  │  (悬浮窗 UI)     │  │ Service(手势派发) │  │
│  └────────┬────────┘  └────────▲─────────┘  │
│           │                    │             │
│  ┌────────▼────────────────────┴─────────┐  │
│  │          GestureEngine                 │  │
│  │  (录制/回放/坐标管理的核心逻辑)          │  │
│  └────────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 职责 |
|---|---|
| **MainActivity** | 权限申请引导（悬浮窗+无障碍），App 入口 |
| **OverlayService** | 前台服务，承载悬浮窗 UI（Compose 渲染） |
| **ClickAccessibilityService** | 无障碍服务，接收手势指令并执行 dispatchGesture() |
| **GestureEngine** | 纯逻辑层，负责手势录制、存储、回放调度 |

---

## 二、双模式设计

### 两种工作模式

| 模式 | 描述 |
|---|---|
| **连点模式** | 用户在屏幕上放置一个或多个点，设置间隔时长，循环点击 |
| **录制模式** | 录制用户的完整操作序列（点击、滑动、长按），精确回放 |

### 连点模式交互流程

1. 用户点击悬浮窗「添加点」按钮
2. 屏幕上出现一个可拖拽的靶标（十字圆圈），拖到目标位置
3. 可继续添加多个靶标，每个靶标可单独删除
4. 在悬浮面板设置点击间隔（ms），所有点共享同一间隔
5. 点击「开始」，按顺序依次点击每个靶标，循环执行
6. 点击「停止」结束

### 手势数据模型

```kotlin
sealed class GestureEvent {
    abstract val timestamp: Long  // 相对于录制开始的时间偏移(ms)
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
        val duration: Long  // 长按持续时间(ms)
    ) : GestureEvent()

    data class Swipe(
        override val timestamp: Long,
        override val x: Float,
        override val y: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long  // 滑动持续时间(ms)
    ) : GestureEvent()
}

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

### 录制流程

1. 用户点击悬浮窗「录制」按钮
2. OverlayService 进入录制模式，覆盖一层半透明触摸捕获层
3. 捕获层拦截触摸事件（ACTION_DOWN / ACTION_MOVE / ACTION_UP）
4. 根据触摸时长和位移，自动判定为 Tap / LongPress / Swipe
5. 将事件写入 List<GestureEvent>，记录相对时间戳
6. 用户点击「停止录制」，录制结束

### 回放流程

1. 用户点击「播放」，GestureEngine 遍历事件列表
2. 通过 delay(event.timestamp - previousTimestamp) 还原时间间隔
3. 将每个事件转换为 GestureDescription，发送给 AccessibilityService 执行
4. 支持循环回放（可设置次数或无限循环）
5. 用户点击「停止」即终止回放协程

### 手势识别规则

| 触摸行为 | 判定条件 |
|---|---|
| **Tap** | 按下到抬起 < 300ms，位移 < 10px |
| **LongPress** | 按下到抬起 >= 300ms，位移 < 10px |
| **Swipe** | 位移 >= 10px |

---

## 三、悬浮窗 UI 设计

### 悬浮窗状态机

```
[收起态] ⇄ [展开态] → [工作态]
  ●          面板        最小化
```

| 形态 | 外观 | 说明 |
|---|---|---|
| **收起态** | 小圆形按钮（48dp），贴屏幕边缘 | 可拖拽移动，单击展开 |
| **展开态** | 控制面板（约 200x300dp） | 模式选择、参数设置、开始/停止 |
| **工作态** | 半透明小圆点（32dp） | 录制或回放进行中，不遮挡操作，单击停止 |

### 展开态面板布局

```
┌──────────────────────┐
│  ○ 连点模式 │ ○ 录制模式 │  ← 顶部 Tab 切换
├──────────────────────┤
│                      │
│  【连点模式时】        │
│  间隔: [  500  ] ms  │  ← 输入框 + 滑杆
│  点数: 3 个           │
│  [+ 添加点]           │
│                      │
│  【录制模式时】        │
│  循环播放: [开关]      │
│                      │
├──────────────────────┤
│  [ ● 开始 ]  [ ✕ 关闭 ] │
└──────────────────────┘
```

### 悬浮窗技术要点

- 使用 WindowManager + TYPE_APPLICATION_OVERLAY 添加视图
- Compose 通过 ComposeView 嵌入到悬浮窗的 FrameLayout 中
- 拖拽：监听 ACTION_MOVE 更新 WindowManager.LayoutParams 的 x/y
- 收起态贴边：拖拽释放后自动吸附到最近的屏幕左/右边缘
- 工作态期间设置 FLAG_NOT_FOCUSABLE，避免拦截无关区域

---

## 四、权限与服务生命周期

### 权限申请流程

```
打开App
  │
  ▼
检查悬浮窗权限 ──否──→ 跳转系统设置页申请
  │是                    │
  ▼                     返回
检查无障碍服务 ──否──→ 跳转无障碍设置页开启
  │是                    │
  ▼                     返回
启动 OverlayService，显示悬浮窗
Activity finish()，退到后台
```

- 悬浮窗权限：Settings.canDrawOverlays() 检测，通过 ACTION_MANAGE_OVERLAY_PERMISSION 跳转
- 无障碍服务：通过 AccessibilityManager.getEnabledAccessibilityServiceList() 检测，跳转 ACTION_ACCESSIBILITY_SETTINGS
- 两项权限都就绪后，MainActivity 主动 finish()

### 服务生命周期

```
OverlayService (前台服务)
  ├── onCreate: 创建悬浮窗，显示收起态
  ├── 运行中: 持有 GestureEngine 实例
  ├── 通过 GestureBridge 与 AccessibilityService 通信
  └── onDestroy: 移除悬浮窗，清理资源

ClickAccessibilityService (系统管理生命周期)
  ├── onServiceConnected: 标记服务可用
  ├── 接收手势指令 → dispatchGesture()
  └── onUnbind: 标记服务不可用
```

### 服务间通信（单例桥接）

```kotlin
object GestureBridge {
    var accessibilityService: ClickAccessibilityService? = null

    fun dispatchGesture(gesture: GestureDescription): Boolean {
        return accessibilityService?.dispatchGesture(
            gesture, null, null
        ) ?: false
    }
}
```

---

## 五、项目结构

### 依赖选型

| 用途 | 选型 | 理由 |
|---|---|---|
| UI | Jetpack Compose + Material 3 | 声明式，适合状态驱动的悬浮窗 |
| 异步 | Kotlin Coroutines + Flow | 回放调度天然适合协程 |
| DI | 手动注入 | 项目小，不引入 Hilt/Koin |
| 构建 | Kotlin DSL (build.gradle.kts) | 现代标准 |

### 目录结构

```
app/src/main/
├── java/com/liandian/app/
│   ├── MainActivity.kt              // 权限引导入口
│   ├── ui/
│   │   ├── overlay/
│   │   │   ├── OverlayService.kt    // 前台服务，管理悬浮窗
│   │   │   ├── OverlayPanel.kt      // Compose: 展开态面板
│   │   │   ├── FloatingButton.kt    // Compose: 收起态小圆钮
│   │   │   └── TapTargetView.kt     // 连点模式的可拖拽靶标
│   │   └── permission/
│   │       └── PermissionScreen.kt  // Compose: 权限引导页
│   ├── service/
│   │   ├── ClickAccessibilityService.kt  // 无障碍服务
│   │   └── GestureBridge.kt              // 服务间通信桥
│   ├── engine/
│   │   ├── GestureEngine.kt         // 核心：录制与回放调度
│   │   ├── GestureRecorder.kt       // 触摸事件 → GestureEvent
│   │   └── GesturePlayer.kt         // GestureEvent → dispatchGesture
│   └── model/
│       ├── GestureEvent.kt          // 手势数据模型
│       └── TaskMode.kt              // 连点/录制模式定义
├── res/
│   ├── xml/
│   │   └── accessibility_config.xml // 无障碍服务配置
│   └── values/
│       └── strings.xml
└── AndroidManifest.xml
```

### Manifest 关键声明

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

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
```
