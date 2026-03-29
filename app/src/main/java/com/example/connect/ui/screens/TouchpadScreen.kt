package com.example.connect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.connect.ui.theme.ConnectDesign

@Composable
fun TouchpadScreen(
    viewModel: com.example.connect.ui.viewmodel.ConnectViewModel,
    onBack: () -> Unit
) {
    val role by viewModel.assignedRole.collectAsState()
    val remoteTouchPoint by viewModel.remoteTouchPoint.collectAsState()
    
    androidx.activity.compose.BackHandler {
        onBack()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectDesign.BackgroundBlack)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.height(40.dp),
                color = Color(0xFF1A0505),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("END SESSION", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            
            Text(
                if (role == com.example.connect.ui.viewmodel.ConnectionRole.SERVER) "HYPER-PRECISION [CONTROLLER]" else "REMOTE CONTROL [ACTIVE]",
                modifier = Modifier.align(Alignment.Center),
                color = ConnectDesign.NeonBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            if (role == com.example.connect.ui.viewmodel.ConnectionRole.SERVER) {
                val isRelative by viewModel.isRelativeMouseMode.collectAsState()
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MOUSE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isRelative,
                        onCheckedChange = { viewModel.setRelativeMouseMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ConnectDesign.NeonBlue,
                            checkedTrackColor = ConnectDesign.NeonBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }

        if (role == com.example.connect.ui.viewmodel.ConnectionRole.SERVER) {
            // Touchpad for Server
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .background(ConnectDesign.SurfaceDark, RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = ConnectDesign.NeonBlue.copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val touches = event.changes.filter { change -> change.pressed }.map { change ->
                                    android.graphics.PointF(change.position.x, change.position.y)
                                }
                                viewModel.handleMultiTouch(touches, size.width.toFloat(), size.height.toFloat())
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SLIDE TO CONTROL",
                        color = Color.DarkGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(ConnectDesign.NeonBlue, CircleShape)
                    )
                }
            }
        } else {
            // Receiver / Controlled State - Minimal UI because app is backgrounded
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "REMOTE CONTROL ACTIVE",
                    color = ConnectDesign.NeonBlue.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
            }
        }
    }
}
