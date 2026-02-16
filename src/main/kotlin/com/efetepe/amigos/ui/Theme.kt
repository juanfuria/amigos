package com.efetepe.amigos.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AmigosShapes = Shapes(
    // Match button corners to input field corners — boxy, not pill
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),   // Buttons use this by default
)

@Composable
fun AmigosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        shapes = AmigosShapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
