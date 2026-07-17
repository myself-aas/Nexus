package com.example.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing and sizing constants for the app.
 * Following Material Design guidelines for consistent spacing.
 */
object Sizing {
    // Spacing scale (8dp base unit)
    object Spacing {
        val xxSmall = 2.dp
        val xSmall = 4.dp
        val small = 8.dp
        val medium = 16.dp
        val large = 24.dp
        val xLarge = 32.dp
        val xxLarge = 48.dp
    }

    // Icon sizes
    object Icons {
        val xSmall = 16.dp
        val small = 20.dp
        val medium = 24.dp
        val large = 32.dp
        val xLarge = 48.dp
    }

    // Component heights
    object ComponentHeight {
        val compact = 36.dp
        val default = 48.dp
        val expanded = 56.dp
        val large = 64.dp
    }

    // Border radius for shapes
    object CornerRadius {
        val xSmall = 4.dp
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val xLarge = 20.dp
        val full = 24.dp
    }

    // Padding for common layout elements
    object Padding {
        val screen = Spacing.medium
        val card = Spacing.medium
        val button = Spacing.small
        val listItem = Spacing.medium
    }

    // Text field sizes
    object TextField {
        val height = 56.dp
        val padding = Spacing.medium
        val cornerRadius = CornerRadius.medium
    }

    // Message bubble related
    object MessageBubble {
        val maxWidth = 0.85f // 85% of screen width
        val cornerRadius = 16.dp
        val padding = 12.dp
        val spacing = 8.dp
        val toolbarButtonSize = 28.dp
        val toolbarIconSize = 14.dp
    }

    // Bottom sheet related
    object BottomSheet {
        val cornerRadius = 28.dp
        val dragHandleHeight = 4.dp
        val dragHandleWidth = 32.dp
        val contentPadding = Spacing.large
    }

    // Cards and surfaces
    object Card {
        val cornerRadius = CornerRadius.medium
        val elevation = 4.dp
        val padding = Spacing.medium
    }

    // Chip/Badge sizes
    object Chip {
        val height = 32.dp
        val padding = Spacing.small
        val cornerRadius = CornerRadius.large
    }
}

/**
 * Extension properties for common dimension combinations
 */
val Sizing.Spacing.all get() = this.medium
val Sizing.Spacing.half get() = this.xSmall
