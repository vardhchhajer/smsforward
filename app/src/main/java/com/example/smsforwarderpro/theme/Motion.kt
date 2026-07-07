package com.example.smsforwarderpro.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MotionSpec {
    // Spatial bouncy springs for premium interactive items
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val StiffBouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Layout transitions
    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Durations
    const val DurationShort = 200
    const val DurationMedium = 400
    const val DurationLong = 600

    val FadeTween = tween<Float>(durationMillis = DurationMedium)
}
