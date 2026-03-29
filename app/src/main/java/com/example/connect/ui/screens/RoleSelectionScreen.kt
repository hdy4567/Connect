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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.theme.ConnectDesign
import com.example.connect.ui.components.ConnectHeader
import com.example.connect.ui.viewmodel.ConnectionRole

@Composable
fun RoleSelectionScreen(
    assignedRole: ConnectionRole,
    onConfirm: () -> Unit
) {
    val glowColor = if (assignedRole == ConnectionRole.SERVER) ConnectDesign.NeonBlue else ConnectDesign.NeonPurple
    
    Box(modifier = Modifier.fillMaxSize().background(ConnectDesign.BackgroundBlack)) {
        // Decorative background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .blur(100.dp)
                .background(glowColor.copy(alpha = 0.15f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            ConnectHeader(
                title = "PHASE 02",
                subtitle = "IDENTITY VERIFIED"
            )

            // Premium Glassmorphism Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = glowColor),
                    shape = RoundedCornerShape(24.dp),
                    color = ConnectDesign.SurfaceDark.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glowColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RoleIllustration(role = assignedRole, color = glowColor)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = if (assignedRole == ConnectionRole.SERVER) "MASTER NODE" else "RECEIVER NODE",
                            color = glowColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (assignedRole == ConnectionRole.SERVER) 
                                "이 기기는 제어의 중심(Master)이 되어 화면을 송출하고 명령을 하달합니다." 
                                else "이 기기는 수신측(Receiver)이 되어 화면을 출력하고 명령을 집행합니다.",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (assignedRole == ConnectionRole.SERVER) 
                                "SMARTPHONE DETECTED • HIGH-SPEED UPLINK" 
                                else "TABLET DETECTED • LOW-LATENCY DOWNLOAD",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // High-End Confirm Button
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = glowColor),
                colors = ButtonDefaults.buttonColors(containerColor = glowColor),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(
                    text = "CONFIRM IDENTITY",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RoleIllustration(role: ConnectionRole, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val strokeWidth = 3.dp.toPx()
            if (role == ConnectionRole.SERVER) {
                // Draw Phone
                val phoneW = size.width * 0.5f
                val phoneH = size.height * 0.9f
                val left = (size.width - phoneW) / 2
                val top = (size.height - phoneH) / 2
                
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(phoneW, phoneH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // Home button dot
                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, top + phoneH - 12.dp.toPx())
                )
            } else {
                // Draw Tablet
                val tabletW = size.width * 0.9f
                val tabletH = size.height * 0.6f
                val left = (size.width - tabletW) / 2
                val top = (size.height - tabletH) / 2
                
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(tabletW, tabletH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // Side button line
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(left + tabletW - 4.dp.toPx(), top + 12.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(left + tabletW - 4.dp.toPx(), top + 24.dp.toPx()),
                    strokeWidth = strokeWidth
                )
            }
        }
        
        // Pulse outer glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, color.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                .padding(8.dp * pulseScale)
        )
    }
}
