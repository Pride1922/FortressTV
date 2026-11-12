package com.fortresstv.player.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min

/** The most a focused item may grow in total width, so wide rows and buttons stay on screen. */
private val MAX_FOCUS_GROWTH = 24.dp

/**
 * The scale a focused item animates to: [scaleOnFocus], but never more than [maxGrowthPx] wider
 * in total. A poster grows by the full amount; a full-width row only by a few pixels per side.
 */
internal fun focusScale(scaleOnFocus: Float, widthPx: Int, maxGrowthPx: Float): Float {
    if (scaleOnFocus <= 1f) return 1f
    if (widthPx <= 0) return scaleOnFocus
    return min(scaleOnFocus, 1f + maxGrowthPx / widthPx)
}

/**
 * Shared focus styling for Android TV and D-pad navigation: the focused item grows (capped for
 * wide items), gets a cyan ring and a soft glow, and draws above its neighbours. It is the main
 * sign of where you are on a TV, so it has to read from the couch. Pass scaleOnFocus = 1f for a
 * ring and glow without growing.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderWidth: Dp = 3.dp,
    focusedBorderColor: Color? = null,
    scaleOnFocus: Float = 1.06f,
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier {
    if (!FormFactorDetector.isTelevisionDevice && onFocusChanged == null) {
        return this
    }
    return composed {
        val isTv = LocalFormFactor.current.isTelevision
        if (!isTv && onFocusChanged == null) {
            return@composed this
        }

        var isFocused by remember { mutableStateOf(false) }
        var widthPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val maxGrowthPx = with(density) { MAX_FOCUS_GROWTH.toPx() }
        val glowPx = with(density) { 20.dp.toPx() }

        val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
        val animatedScale by animateFloatAsState(
            targetValue = if (isFocused && isTv) focusScale(scaleOnFocus, widthPx, maxGrowthPx) else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tv_focus_scale"
        )

        val baseModifier = this
            .onSizeChanged { widthPx = it.width }
            .onFocusChanged { state ->
                isFocused = state.isFocused
                onFocusChanged?.invoke(state.isFocused)
            }

        if (!isTv) return@composed baseModifier

        baseModifier
            .zIndex(if (isFocused) 1f else 0f)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                if (isFocused) {
                    // A coloured shadow reads as a glow (Android 9+; older versions show a plain shadow).
                    shadowElevation = glowPx
                    ambientShadowColor = borderColor
                    spotShadowColor = borderColor
                    this.shape = shape
                    clip = false
                }
            }
            .border(
                BorderStroke(
                    width = if (isFocused) focusedBorderWidth else 0.dp,
                    color = if (isFocused) borderColor else Color.Transparent
                ),
                shape = shape
            )
    }
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
