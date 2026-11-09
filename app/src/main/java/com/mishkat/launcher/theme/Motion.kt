package com.mishkat.launcher.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion spec v1. All durations/springs derive from [ThemeTokens] so theme packs can tune feel.
 * - Focus: spring (interruptible, no overshoot past ~2% at damping 0.85).
 * - Enter: tween enterMs, FastOutSlowIn.
 * - Exit: 60% of enterMs (exits should be faster than entrances).
 */
object Motion {
    fun focus(tokens: ThemeTokens): AnimationSpec<Float> =
        spring(dampingRatio = tokens.springDamping, stiffness = Spring.StiffnessMediumLow)

    fun <T> enter(tokens: ThemeTokens): AnimationSpec<T> =
        tween(durationMillis = tokens.enterMs, easing = FastOutSlowInEasing)

    fun <T> exit(tokens: ThemeTokens): AnimationSpec<T> =
        tween(durationMillis = (tokens.enterMs * 0.6f).toInt(), easing = FastOutSlowInEasing)
}
