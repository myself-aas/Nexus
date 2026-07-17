package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive radial gradient background representing a premium glowing look.
 * Adapts to dark/light themes.
 */
@Composable
fun GlassGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        val bgGradient = if (isDark) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1B233F), // Indigo glow
                    Color(0xFF0C0F1A), // Near-black navy
                    Color(0xFF05070D)  // Deep midnight black
                ),
                center = Offset(width / 2f, height * 0.3f),
                radius = maxOf(width, height) * 1.2f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFEFF6FF), // Soft light blue highlight
                    Color(0xFFF8FAFC), // Off-white slate
                    Color(0xFFE2E8F0)  // Gentle gray border
                ),
                center = Offset(width / 2f, height * 0.3f),
                radius = maxOf(width, height) * 1.2f
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        ) {
            content()
        }
    }
}

/**
 * A frosted translucent card with smooth rounded corners, light borders, and fallback for older APIs.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 16.dp,
    tintAlpha: Float = 0.08f,
    tintColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    
    Box(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier)
            .background(tintColor.copy(alpha = tintAlpha))
            .border(borderWidth, borderColor, shape)
    ) {
        // Blur background only on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(blurRadius)
                    .background(tintColor.copy(alpha = 0.05f))
            )
        }
        content()
    }
}

/**
 * Pill-shaped glass button with low-opacity fill and thin light border.
 * Follows Material 3 standard 48dp minimum interactive size.
 */
@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val alpha = if (enabled) 0.15f else 0.05f
    val borderColor = if (isDark) {
        if (enabled) tintColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
    } else {
        if (enabled) tintColor.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f)
    }
    
    val shape = RoundedCornerShape(50)
    
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .background(tintColor.copy(alpha = alpha))
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/**
 * Reusable pill-shaped active/inactive chip with sleek glow.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surface
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (selected) activeColor else inactiveColor
    val tintAlpha = if (selected) 0.25f else 0.06f
    val borderColor = if (selected) {
        activeColor.copy(alpha = 0.5f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    }
    
    val shape = RoundedCornerShape(50)
    
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(tintColor.copy(alpha = tintAlpha))
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    if (isDark) Color.White else activeColor
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
        }
    }
}

/**
 * Floating pill-shaped bottom navigation bar that sits above screen content.
 */
@Composable
fun GlassBottomNav(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth(),
        cornerRadius = 28.dp,
        blurRadius = 20.dp,
        tintAlpha = 0.12f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Item in the GlassBottomNav.
 */
@Composable
fun RowScope.GlassBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val shape = RoundedCornerShape(24.dp)
    
    Box(
        modifier = modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .padding(4.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(if (selected) tintColor.copy(alpha = 0.15f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

/**
 * Reusable frosted ModalBottomSheet wrapper with glassmorphism style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isDark) Color(0xFF0F172A) else Color.White
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = tintColor.copy(alpha = 0.85f),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            content = content
        )
    }
}
