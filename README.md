# 连点器

一款 Android 悬浮窗连点器工具，支持快速连点和录制回放两种模式。用户可在任意 App 或网页上通过悬浮控件设置点击位置和频率，实现自动化点击操作。

## 功能特性

### 连点模式
- 在屏幕上放置一个或多个可拖拽的靶标
- 自定义点击间隔（50ms ~ 5000ms）
- 按顺序循环点击所有靶标

### 录制回放模式
- 录制完整的触摸操作序列（点击、长按、滑动）
- 自动识别手势类型
- 精确回放录制的操作，支持循环播放

### 悬浮窗设计
- **收起态** — 48dp 小圆按钮，贴边吸附，不遮挡内容
- **展开态** — Material 3 风格控制面板，模式切换与参数设置
- **工作态** — 32dp 半透明指示点，单击即停

## 技术栈

| 项目 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 异步 | Kotlin Coroutines + Flow |
| 构建 | AGP 9.0 + Gradle 8.13 (Kotlin DSL) |
| 最低版本 | Android 10 (API 29) |
| 目标版本 | Android 15 (API 35) |

## 项目结构

```
app/src/main/java/com/liandian/app/
├── MainActivity.kt                  # 权限引导入口
├── model/
│   ├── GestureEvent.kt              # 手势事件数据模型
│   └── TaskMode.kt                  # 连点/录制模式定义
├── service/
│   ├── GestureBridge.kt             # 服务间通信桥
│   └── ClickAccessibilityService.kt # 无障碍服务（手势派发）
├── engine/
│   ├── GestureEngine.kt             # 引擎状态机
│   ├── GestureRecorder.kt           # 触摸事件录制
│   └── GesturePlayer.kt             # 手势回放
└── ui/
    ├── overlay/
    │   ├── OverlayService.kt        # 悬浮窗前台服务
    │   ├── OverlayPanel.kt          # 展开态控制面板
    │   ├── FloatingButton.kt        # 收起态按钮
    │   └── TapTargetView.kt         # 可拖拽靶标
    └── permission/
        └── PermissionScreen.kt      # 权限引导页
```

## 构建与运行

### 环境要求

- JDK 17+
- Android SDK（compileSdk 35）
- Android Studio Otter 3 Feature Drop 或更高版本（可选）

### 命令行构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要签名配置）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/` 目录。

### Android Studio

直接用 Android Studio 打开项目根目录，等待 Gradle Sync 完成后即可运行。

## 使用说明

1. 安装并打开 App
2. 按提示依次开启 **悬浮窗权限** 和 **无障碍服务**
3. 权限就绪后点击「启动连点器」，App 退到后台，屏幕边缘出现悬浮按钮
4. 点击悬浮按钮展开控制面板
5. 选择模式：
   - **连点模式**：添加靶标 → 调整间隔 → 开始
   - **录制模式**：点击录制 → 操作屏幕 → 停止录制 → 播放
6. 工作中点击半透明指示点即可停止

## 权限说明

| 权限 | 用途 |
|---|---|
| `SYSTEM_ALERT_WINDOW` | 在其他应用上方显示悬浮控制面板 |
| `AccessibilityService` | 执行自动点击、滑动、长按等手势操作 |
| `FOREGROUND_SERVICE` | 保持悬浮窗服务在后台运行 |

## 许可证

MIT License
