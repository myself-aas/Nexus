package com.example.presentation.util

import androidx.compose.animation.core.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Haptic feedback utilities for consistent user interaction feedback.
 */
object HapticUtils {
    /**
     * Light tap for UI confirmations and button presses
     */
    @Composable
    fun lightTap() {
        val haptic = LocalHapticFeedback.current
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Medium tick for state changes and selections
     */
    @Composable
    fun mediumClick() {
        val haptic = LocalHapticFeedback.current
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Strong pulse for important actions (send, delete)
     */
    @Composable
    fun strongPulse() {
        val haptic = LocalHapticFeedback.current
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Animation specifications for consistent motion design
 */
object AnimationSpecs {
    // Standard durations (milliseconds)
    const val QUICK = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 800

    // Common animation specifications
    val quickTween: AnimationSpec<Float> = tween(QUICK, easing = EaseInOutCubic)
    val normalTween: AnimationSpec<Float> = tween(NORMAL, easing = EaseInOutCubic)
    val slowTween: AnimationSpec<Float> = tween(SLOW, easing = EaseInOutCubic)

    // Spring animations for natural motion
    val gentleSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium
    )

    val bouncySpring: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Message entry animation: slide in from bottom + fade in
 */
@Composable
fun rememberMessageEnterAnimation(): Triple<Float, Float, Float> {
    val slideAnimation = animateFloatAsState(
        targetValue = 1f,
        animationSpec = AnimationSpecs.normalTween,
        label = "MessageSlide"
    )

    val fadeAnimation = animateFloatAsState(
        targetValue = 1f,
        animationSpec = AnimationSpecs.normalTween,
        label = "MessageFade"
    )

    val scaleAnimation = animateFloatAsState(
        targetValue = 1f,
        animationSpec = AnimationSpecs.gentleSpring,
        label = "MessageScale"
    )

    return Triple(slideAnimation.value, fadeAnimation.value, scaleAnimation.value)
}

/**
 * Expansion animation for reasoning reveal: scale + alpha
 */
@Composable
fun rememberExpansionAnimation(isExpanded: Boolean): Pair<Float, Float> {
    val scaleAnimation = animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.85f,
        animationSpec = AnimationSpecs.normalTween,
        label = "ExpansionScale"
    )

    val alphaAnimation = animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = AnimationSpecs.normalTween,
        label = "ExpansionAlpha"
    )

    return Pair(scaleAnimation.value, alphaAnimation.value)
}

/**
 * Loading spinner pulsing animation
 */
@Composable
fun rememberPulseAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseInfinite")
    
    val pulseAnimation = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    return pulseAnimation.value
}

/**
 * Button press animation with feedback
 */
@Composable
fun rememberButtonPressAnimation(isPressed: Boolean): Pair<Float, Float> {
    val scaleAnimation = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationSpecs.quickTween,
        label = "ButtonScale"
    )

    val elevationAnimation = animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = AnimationSpecs.quickTween,
        label = "ButtonElevation"
    )

    return Pair(scaleAnimation.value, elevationAnimation.value)
}

/**
 * Swipe gesture visual feedback animation
 */
@Composable
fun rememberSwipeGestureAnimation(swipeProgress: Float): Triple<Float, Float, Float> {
    // Translate animation
    val translateAnimation = animateFloatAsState(
        targetValue = swipeProgress * 100,
        animationSpec = AnimationSpecs.normalTween,
        label = "SwipeTranslate"
    )

    // Fade animation as swipe progresses
    val fadeAnimation = animateFloatAsState(
        targetValue = 1f - swipeProgress,
        animationSpec = AnimationSpecs.normalTween,
        label = "SwipeFade"
    )

    // Scale animation for swiped-away items
    val scaleAnimation = animateFloatAsState(
        targetValue = 1f - (swipeProgress * 0.1f),
        animationSpec = AnimationSpecs.normalTween,
        label = "SwipeScale"
    )

    return Triple(translateAnimation.value, fadeAnimation.value, scaleAnimation.value)
}

/**
 * Skeleton loading shimmer animation
 */
@Composable
fun rememberShimmerAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerInfinite")
    
    val shimmerAnimation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Shimmer"
    )

    return shimmerAnimation.value
}
