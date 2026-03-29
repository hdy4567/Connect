package com.example.connect.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object ConnectDesign {
    val BackgroundBlack = Color(0xFF050505)
    val SurfaceDark = Color(0xFF121212)
    val NeonBlue = Color(0xFF00D1FF)
    val NeonPurple = Color(0xFFA855F7)
    val SuccessGreen = Color(0xFF10B981)
    
    val NeonGlowGradient = Brush.verticalGradient(
        colors = listOf(
            NeonBlue.copy(alpha = 0.1f),
            Color.Transparent
        )
    )
    
    val GlassOverlay = Color(0x33FFFFFF)
}
