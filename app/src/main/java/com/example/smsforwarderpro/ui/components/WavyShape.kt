package com.example.smsforwarderpro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WavyDivider(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    waveHeight: Dp = 8.dp,
    waveLength: Dp = 24.dp,
    strokeWidth: Dp = 3.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight * 2)
    ) {
        val width = size.width
        val height = size.height
        val waveHeightPx = waveHeight.toPx()
        val waveLengthPx = waveLength.toPx()
        val strokeWidthPx = strokeWidth.toPx()

        val path = Path().apply {
            val startY = height / 2f
            moveTo(0f, startY)
            var x = 0f
            while (x < width) {
                // Compute sine curve
                val relativeX = x / waveLengthPx
                val y = startY + sin(relativeX * 2 * Math.PI).toFloat() * waveHeightPx
                lineTo(x, y)
                x += 2f // small increment for smoothness
            }
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = strokeWidthPx)
        )
    }
}
