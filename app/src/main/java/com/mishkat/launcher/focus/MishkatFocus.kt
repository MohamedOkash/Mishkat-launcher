package com.mishkat.launcher.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.theme.LocalThemeTokens
import com.mishkat.launcher.theme.Motion
import kotlinx.coroutines.launch

/**
 * R15: the one focus treatment for every interactive element.
 *
 * Place it BEFORE the focusable/clickable modifier:
 * `Modifier.mishkatFocus("apps:com.foo").clickable { ... }`
 *
 * - Scale + border driven by ThemeTokens (focusScale / springDamping / primary), drawn in the draw
 *   phase only (no recomposition, no layout pass while animating).
 * - [id] (stable, unique) enables crash/reboot focus restoration via [FocusRestorer].
 * - [radius] overrides the border corner radius (defaults to the theme card radius).
 */
fun Modifier.mishkatFocus(id: String? = null, radius: Dp? = null): Modifier =
    this then MishkatFocusElement(id, radius)

private data class MishkatFocusElement(val id: String?, val radius: Dp?) :
    ModifierNodeElement<MishkatFocusNode>() {
    override fun create() = MishkatFocusNode(id, radius)

    override fun update(node: MishkatFocusNode) {
        node.id = id
        node.radius = radius
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mishkatFocus"
        properties["id"] = id
        properties["radius"] = radius
    }
}

private class MishkatFocusNode(var id: String?, var radius: Dp?) :
    Modifier.Node(),
    FocusEventModifierNode,
    FocusRequesterModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    private val progress = Animatable(0f)
    private var focused = false
    private var stroke: Stroke? = null

    override fun onAttach() {
        val myId = id ?: return
        val restorer = currentValueOf(LocalFocusRestorer) ?: return
        // Fast path (no allocation) once restoration is settled — keeps scrolling allocation-free (R6).
        if (restorer.isSettled) return
        coroutineScope.launch {
            val target = restorer.restoreTarget.await()
            if (target == myId && restorer.claim(myId)) {
                withFrameNanos { }
                val ok = requestFocus()
                Logger.focus { "Restored focus to $myId: $ok" }
            }
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        val now = focusState.hasFocus
        if (now == focused) return
        focused = now
        if (now) id?.let { currentValueOf(LocalFocusRestorer)?.onFocused(it) }
        val spec = Motion.focus(currentValueOf(LocalThemeTokens))
        coroutineScope.launch { progress.animateTo(if (now) 1f else 0f, spec) }
    }

    override fun ContentDrawScope.draw() {
        val p = progress.value
        if (p <= 0.001f) {
            drawContent()
            return
        }
        val tokens = currentValueOf(LocalThemeTokens)
        val s = 1f + p * (tokens.focusScale - 1f)
        val st = stroke ?: Stroke(width = with(currentValueOf(LocalDensity)) { BORDER_DP.toPx() }).also { stroke = it }
        val r = (radius ?: tokens.cardRadius).toPx()
        val content = this
        scale(s, pivot = center) {
            content.drawContent()
            drawRoundRect(
                color = tokens.primary.copy(alpha = p),
                cornerRadius = CornerRadius(r, r),
                style = st,
            )
        }
    }

    private companion object {
        val BORDER_DP: Dp = 3.dp
    }
}
