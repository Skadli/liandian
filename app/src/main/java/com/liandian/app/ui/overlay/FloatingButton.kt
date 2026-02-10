package com.liandian.app.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.liandian.app.R

@Composable
fun FloatingButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_floating_button),
            contentDescription = "floating icon",
            tint = Color.Unspecified,
            modifier = Modifier.size(50.dp)
        )
    }
}
