package com.gamebox.os.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.focus.focusProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

internal data class BlueprintScreenRoute(
    val destination: Destination,
    val selectedGameId: String?,
)

internal enum class BlueprintTransitionAxis { NONE, HORIZONTAL, VERTICAL }

internal data class BlueprintTransitionSpec(
    val axis: BlueprintTransitionAxis,
    val direction: Int = 0,
)

internal fun blueprintTransitionSpec(
    previous: BlueprintScreenRoute?,
    current: BlueprintScreenRoute,
): BlueprintTransitionSpec {
    if (previous == null) return BlueprintTransitionSpec(BlueprintTransitionAxis.NONE)

    if (previous.destination != current.destination) {
        val previousTab = primaryDestinations.indexOf(previous.destination)
        val currentTab = primaryDestinations.indexOf(current.destination)
        if (previousTab >= 0 && currentTab >= 0) {
            val tabCount = primaryDestinations.size
            val delta = currentTab - previousTab
            val direction = when {
                delta == 0 -> 0
                kotlin.math.abs(delta) == tabCount - 1 -> -delta.compareTo(0)
                else -> delta.compareTo(0)
            }
            return BlueprintTransitionSpec(
                BlueprintTransitionAxis.HORIZONTAL,
                direction = direction,
            )
        }
        return BlueprintTransitionSpec(
            BlueprintTransitionAxis.VERTICAL,
            direction = if (current.destination == Destination.DOWNLOADS) 1 else -1,
        )
    }

    if (previous.selectedGameId != current.selectedGameId) {
        return BlueprintTransitionSpec(
            BlueprintTransitionAxis.VERTICAL,
            direction = if (current.selectedGameId != null) 1 else -1,
        )
    }
    return BlueprintTransitionSpec(BlueprintTransitionAxis.NONE)
}

@Composable
internal fun BlueprintScreenTransition(
    route: BlueprintScreenRoute,
    content: @Composable (BlueprintScreenRoute) -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) {
        Box(Modifier.fillMaxSize()) { content(route) }
    } else {
        val controllerActions = LocalControllerActions.current
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                val transition = blueprintTransitionSpec(initialState, targetState)
                transition.enterTransition().togetherWith(transition.exitTransition())
            },
            label = "gamebox-route-transition",
        ) { visibleRoute ->
            val outgoingRoute = visibleRoute != route
            val routeModifier = if (outgoingRoute) {
                Modifier.focusProperties { canFocus = false }.clearAndSetSemantics { }
            } else Modifier
            CompositionLocalProvider(
                LocalControllerActions provides if (outgoingRoute) null else controllerActions,
            ) {
                Box(Modifier.fillMaxSize().then(routeModifier)) { content(visibleRoute) }
            }
        }
    }
}

private fun BlueprintTransitionSpec.enterTransition(): EnterTransition = when (axis) {
    BlueprintTransitionAxis.NONE -> EnterTransition.None
    BlueprintTransitionAxis.HORIZONTAL -> fadeIn(tween(180)) + slideInHorizontally(tween(240)) { width ->
        width / 24 * direction
    } + scaleIn(initialScale = 0.99f, animationSpec = tween(240))
    BlueprintTransitionAxis.VERTICAL -> fadeIn(tween(180)) + slideInVertically(tween(240)) { height ->
        height / 24 * direction
    } + scaleIn(initialScale = 0.99f, animationSpec = tween(240))
}

private fun BlueprintTransitionSpec.exitTransition(): ExitTransition = when (axis) {
    BlueprintTransitionAxis.NONE -> fadeOut(tween(120))
    BlueprintTransitionAxis.HORIZONTAL -> fadeOut(tween(160)) +
        slideOutHorizontally(tween(240)) { width -> -width / 24 * direction } +
        scaleOut(targetScale = 0.99f, animationSpec = tween(240))
    BlueprintTransitionAxis.VERTICAL -> fadeOut(tween(160)) +
        slideOutVertically(tween(240)) { height -> -height / 24 * direction } +
        scaleOut(targetScale = 0.99f, animationSpec = tween(240))
}
