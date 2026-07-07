package com.example.smsforwarderpro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.smsforwarderpro.theme.MotionSpec
import kotlin.math.roundToInt

@Composable
fun SpringSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val padding = 4.dp

    // Animate color based on state
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        label = "trackColor"
    )

    val activeThumbColor = Color.White
    val inactiveThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
    val thumbColor by animateColorAsState(
        targetValue = if (checked) activeThumbColor else inactiveThumbColor,
        label = "thumbColor"
    )

    // Bouncy alignment animation using spring
    val targetBias = if (checked) 1f else -1f
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = MotionSpec.BouncySpring,
        label = "thumbBias"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(48.dp)
            .semantics {
                stateDescription = if (checked) "On" else "Off"
            }
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(CircleShape)
                .background(trackColor)
            .padding(padding),
            contentAlignment = Alignment.CenterStart
        ) {
            // Calculate dynamic pixel-based offset representing position.
            val maxTravel = 24.dp
        
            val normalized = (animatedBias + 1f) / 2f
            val offsetDp = maxTravel * normalized

            Box(
                modifier = Modifier
                    .offset { IntOffset((offsetDp.toPx()).roundToInt(), 0) }
                    .width(thumbSize)
                    .fillMaxHeight()
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(thumbColor, CircleShape)
            )
        }
    }
}
