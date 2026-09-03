package com.fortresstv.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared focus styling for Android TV and D-pad navigation.
 * Highlights the composable with a high-contrast border and optional subtle scale
 * when focused.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderWidth: Dp = 2.dp,
    focusedBorderColor: Color? = null,
    scaleOnFocus: Float = 1.02f,
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_focus_scale"
    )

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChanged?.invoke(state.isFocused)
        }
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .border(
            BorderStroke(
                width = if (isFocused) focusedBorderWidth else 0.dp,
                color = if (isFocused) borderColor else Color.Transparent
            ),
            shape = shape
        )
}

/**
 * Focus zones for managing navigation between the primary sidebar and content.
 */
enum class NavigationZone {
    SIDEBAR,
    CONTENT,
    MODAL,
}

/**
 * Testable coordinator managing focus transitions and back-stack focus restoration
 * between primary navigation areas.
 */
class NavigationFocusCoordinator(
    initialZone: NavigationZone = NavigationZone.SIDEBAR
) {
    var currentZone: NavigationZone = initialZone
        private set

    var previousZone: NavigationZone? = null
        private set

    /**
     * Move focus into a new navigation zone.
     */
    fun transitionTo(target: NavigationZone) {
        if (currentZone != target) {
            previousZone = currentZone
            currentZone = target
        }
    }

    /**
     * Handles the Back action.
     * Returns true if the Back action was consumed internally (e.g. retreating from content to sidebar),
     * or false if the Back action should be handled by the system/activity (e.g. exit application).
     */
    fun handleBack(): Boolean {
        return when (currentZone) {
            NavigationZone.MODAL -> {
                currentZone = previousZone ?: NavigationZone.CONTENT
                true
            }
            NavigationZone.CONTENT -> {
                currentZone = NavigationZone.SIDEBAR
                true
            }
            NavigationZone.SIDEBAR -> {
                false // Allow system back to minimize/exit
            }
        }
    }
}
