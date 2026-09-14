package com.gamebox.os.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal val LocalFocusDebugRegistry = staticCompositionLocalOf<FocusDebugRegistry?> { null }

internal enum class FocusDirection(val label: String) {
    UP("up"), DOWN("down"), LEFT("left"), RIGHT("right")
}

@Stable
internal class FocusDebugRegistry {
    private var nextId by mutableIntStateOf(1)
    val nodes = mutableStateMapOf<Int, Rect>()
    var focusedId by mutableStateOf<Int?>(null)
        private set

    fun allocateId(): Int = nextId++

    fun update(id: Int, bounds: Rect) {
        nodes[id] = bounds
    }

    fun focus(id: Int, focused: Boolean) {
        if (focused) focusedId = id else if (focusedId == id) focusedId = null
    }

    fun remove(id: Int) {
        nodes.remove(id)
        if (focusedId == id) focusedId = null
    }

    fun neighbor(direction: FocusDirection): Int? {
        val sourceId = focusedId ?: return null
        val source = nodes[sourceId] ?: return null
        val origin = source.center
        return nodes.asSequence()
            .filter { (id, bounds) ->
                id != sourceId && when (direction) {
                    FocusDirection.UP -> bounds.center.y < origin.y
                    FocusDirection.DOWN -> bounds.center.y > origin.y
                    FocusDirection.LEFT -> bounds.center.x < origin.x
                    FocusDirection.RIGHT -> bounds.center.x > origin.x
                }
            }
            .minByOrNull { (_, bounds) ->
                val delta = bounds.center - origin
                when (direction) {
                    FocusDirection.UP, FocusDirection.DOWN -> abs(delta.y) + abs(delta.x) * 2f
                    FocusDirection.LEFT, FocusDirection.RIGHT -> abs(delta.x) + abs(delta.y) * 2f
                }
            }?.key
    }
}

internal fun Modifier.focusDebugTarget(): Modifier = composed {
    val registry = LocalFocusDebugRegistry.current
    if (registry == null) return@composed this
    val id = androidx.compose.runtime.remember(registry) { registry.allocateId() }
    DisposableEffect(registry, id) {
        onDispose { registry.remove(id) }
    }
    this
        .onGloballyPositioned { registry.update(id, it.boundsInRoot()) }
        .onFocusChanged { registry.focus(id, it.isFocused) }
}

@Composable
internal fun FocusDebugOverlay(registry: FocusDebugRegistry) {
    val focusedId = registry.focusedId
    val bounds = focusedId?.let(registry.nodes::get)
    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Focus diagnostics overlay" }
    ) {
        if (bounds != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0xFFFFC857),
                    topLeft = Offset(bounds.left, bounds.top),
                    size = bounds.size,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            color = Color(0xE6121724),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.small,
            shadowElevation = 8.dp,
        ) {
            val lines = if (focusedId == null || bounds == null) {
                "FOCUS DEBUG\nNo focused Blueprint control"
            } else {
                buildString {
                    append("FOCUS DEBUG  control-").append(focusedId)
                    append("\nBounds: ")
                    append(bounds.left.toInt()).append(", ").append(bounds.top.toInt())
                    append("  ").append(bounds.width.toInt()).append("×").append(bounds.height.toInt())
                    FocusDirection.entries.forEach { direction ->
                        append("\n").append(direction.label.uppercase()).append(": ")
                        append(registry.neighbor(direction)?.let { "control-$it" } ?: "—")
                    }
                }
            }
            Text(lines, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}
