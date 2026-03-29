package com.example.connect.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.theme.ConnectDesign
import com.example.connect.ui.viewmodel.ConnectionRole
import com.example.connect.ui.components.ConnectHeader

@Composable
fun ModeSelectionScreen(
    role: ConnectionRole,
    onHostClick: () -> Unit,
    onClientViewClick: () -> Unit,
    onClientTouchClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ConnectDesign.BackgroundBlack)) {
        // Subtle background animation
        PulseBackground(color = if (role == ConnectionRole.SERVER) ConnectDesign.NeonBlue else ConnectDesign.NeonPurple)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            ConnectHeader(
                title = "CONTROL",
                subtitle = if (role == ConnectionRole.SERVER) "MASTER NODE INTERFACE" else "SYNCED SINK STATION"
            )
            
            Spacer(modifier = Modifier.height(60.dp))
            
            if (role == ConnectionRole.SERVER) {
                GlassModeCard(
                    title = "화면 공유 시작",
                    description = "모바일 화면을 실시간 60FPS로 전송합니다",
                    badge = "ULTRA-STREAM",
                    glowColor = ConnectDesign.NeonBlue,
                    iconType = "MIRROR",
                    onClick = onHostClick
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                GlassModeCard(
                    title = "터치패드 모드",
                    description = "정밀한 원격 마우스 및 클릭 제어를 시작합니다",
                    badge = "PRECISION",
                    glowColor = ConnectDesign.SuccessGreen,
                    iconType = "TOUCH",
                    onClick = onClientTouchClick
                )
            } else {
                GlassModeCard(
                    title = "화면 수신 대기",
                    description = "마스터 노드의 화면을 확장 디스플레이로 수신",
                    badge = "DISPLAY",
                    glowColor = ConnectDesign.NeonPurple,
                    iconType = "MIRROR",
                    onClick = onClientViewClick
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                GlassModeCard(
                    title = "터치패드 모드 전송",
                    description = "이 기기를 마스터 노드의 정밀 입력 장치로 전환",
                    badge = "CONTROLLER",
                    glowColor = ConnectDesign.SuccessGreen,
                    iconType = "TOUCH",
                    onClick = onClientTouchClick
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "AWAITING MASTER COMMANDS...",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun GlassModeCard(
    title: String,
    description: String,
    badge: String,
    glowColor: Color,
    iconType: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = glowColor)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ConnectDesign.SurfaceDark.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, glowColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeIcon(type = iconType, color = glowColor)
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = glowColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, glowColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = badge,
                            color = glowColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ModeIcon(type: String, color: Color) {
    Canvas(modifier = Modifier.size(48.dp)) {
        val strokeWidth = 2.dp.toPx()
        if (type == "MIRROR") {
            // Draw Screen to Screen Projection
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 8f),
                size = androidx.compose.ui.geometry.Size(32f.dp.toPx(), 24f.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.5f),
                topLeft = androidx.compose.ui.geometry.Offset(12f.dp.toPx(), 16f.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(32f.dp.toPx(), 24f.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        } else {
            // Draw Touch Indicator
            drawCircle(
                color = color,
                radius = 16f.dp.toPx(),
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = color,
                radius = 4f.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun PulseBackground(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(120.dp)
            .background(color.copy(alpha = alpha))
    )
}
