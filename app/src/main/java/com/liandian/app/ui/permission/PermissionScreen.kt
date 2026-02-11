package com.liandian.app.ui.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
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

        Spacer(modifier = Modifier.height(24.dp))

        var showGuide by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showGuide = !showGuide },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showGuide) "收起引导" else "使用引导")
        }

        AnimatedVisibility(visible = showGuide) {
            OnboardingGuide()
        }
    }
}

@Composable
private fun OnboardingGuide() {
    val pages = listOf(
        GuideStep(
            title = "第 1 步：启动",
            content = "授权悬浮窗和无障碍权限后，点击「启动连点器」。\n\n屏幕左侧会出现一个悬浮球，这是你的控制入口。"
        ),
        GuideStep(
            title = "第 2 步：添加点击目标",
            content = "点击悬浮球展开控制面板。\n\n在「连点」标签下，点击「添加点」在屏幕上放置红色目标点。\n\n拖动目标点可调整位置，单击目标点可删除。"
        ),
        GuideStep(
            title = "第 3 步：设置间隔并开始",
            content = "点击间隔时间数字可直接输入毫秒值，也可以用 -100 / +100 按钮微调。\n\n设置好后点击播放按钮，连点器开始自动点击。\n\n屏幕右上角会出现红色小圆点，点击它即可停止。"
        ),
        GuideStep(
            title = "第 4 步：录制手势",
            content = "切换到「录制」标签，点击录制按钮。\n\n在屏幕上执行点击、长按、滑动等操作，操作会被记录下来。\n\n点击右上角红点停止录制，然后点击播放按钮回放。"
        ),
        GuideStep(
            title = "第 5 步：悬浮球技巧",
            content = "单击悬浮球：展开/收起控制面板\n\n拖动悬浮球：移动位置，松手自动贴边\n\n拖到屏幕底部：退出连点器\n\n控制面板也可以拖动移动位置。"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        pages[page].title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        pages[page].content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(pages.size) { index ->
                    val color = if (pagerState.currentPage == index)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 导航按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Text("上一步")
                }

                Text(
                    "${pagerState.currentPage + 1} / ${pages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = pagerState.currentPage < pages.size - 1
                ) {
                    Text("下一步")
                }
            }
        }
    }
}

private data class GuideStep(val title: String, val content: String)

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
