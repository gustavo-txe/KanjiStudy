package com.app.kanjistudy.scan.camerascan

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.kanjistudy.R

@Composable
fun StatusToggleIcon(
    isLearned: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "learnedIconColorAnimation")
    val animatedColor = infiniteTransition.animateColor(
        initialValue = Color(0xFF28D0A1),
        targetValue = Color(0xFF156B18),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "learnedIconColorCycle"
    )

    Icon(
        painter = if (isLearned) painterResource(id = R.drawable.checkicon)
        else painterResource(id = R.drawable.baseline_add_24),
        contentDescription = if (isLearned) "Remove learned kanji" else "Mark kanji as learned",
        modifier = modifier
            .size(36.dp)
            .clickable(onClick = onToggle),
        tint = if (isLearned) animatedColor.value else Color.Gray
    )
}