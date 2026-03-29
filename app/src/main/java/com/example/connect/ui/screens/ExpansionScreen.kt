package com.example.connect.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.theme.ConnectDesign
import com.example.connect.ui.viewmodel.ConnectViewModel

@Composable
fun ExpansionScreen(viewModel: ConnectViewModel) {
    val role by viewModel.assignedRole.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val latency by viewModel.latency.collectAsState()

    androidx.activity.compose.BackHandler {
        viewModel.stopMirroring()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Main Content Area
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isStreaming) {
                Spacer(modifier = Modifier.height(48.dp))
                com.example.connect.ui.components.ConnectHeader(
                    title = "EXPANSION",
                    subtitle = if (role == com.example.connect.ui.viewmodel.ConnectionRole.SERVER) "READY TO PROJECT" else "AWAITING SOURCE"
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Idle Visualization
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .shadow(24.dp, CircleShape, spotColor = ConnectDesign.NeonPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = ConnectDesign.SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(2.dp, ConnectDesign.NeonPurple.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(120.dp),
                                color = ConnectDesign.NeonPurple,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "M1",
                                color = ConnectDesign.NeonPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            if (role == com.example.connect.ui.viewmodel.ConnectionRole.RECEIVER) {
                // High-Performance Mirroring Surface for Receiver
                val containerModifier = if (isStreaming) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = ConnectDesign.NeonPurple)
                }

                Card(
                    modifier = containerModifier
                        .pointerInput(Unit) {
                            if (role == com.example.connect.ui.viewmodel.ConnectionRole.RECEIVER && isStreaming) {
                                val captureSize = size
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val touches = event.changes.filter { change -> change.pressed }.map { change ->
                                            android.graphics.PointF(change.position.x, change.position.y)
                                        }
                                        val mapped = viewModel.handleMultiTouch(touches, captureSize.width.toFloat(), captureSize.height.toFloat())
                                        if (mapped != null) {
                                            event.changes.forEach { change -> change.consume() }
                                        }
                                    }
                                }
                            }
                        },
                    shape = if (isStreaming) RectangleShape else RoundedCornerShape(12.dp),
                    border = if (isStreaming) null else androidx.compose.foundation.BorderStroke(1.dp, ConnectDesign.NeonPurple.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { context ->
                            com.example.connect.ui.components.MirrorSurfaceView(context).apply {
                                setListener(object : com.example.connect.ui.components.MirrorSurfaceView.Listener {
                                    override fun onSurfaceAvailable(surface: android.view.Surface) {
                                        viewModel.startSink(surface)
                                    }
                                    override fun onSurfaceDestroyed() {
                                        viewModel.stopSink()
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Server State - Sharing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCREEN MIRRORING ACTIVE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PROJECTING TO TABLET @ 60FPS",
                            color = ConnectDesign.NeonPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } // End Main Column

        // Technical Performance HUD (Stage 3 Pro UX)
        if (role == com.example.connect.ui.viewmodel.ConnectionRole.RECEIVER && isStreaming) {
            StatusHUD(fps = fps, latency = latency, modifier = Modifier.align(Alignment.TopEnd))
            
            // Visual Touch Feedback Rendering (Phases 9 & 10)
            val remoteTouch by viewModel.remoteTouchPoint.collectAsState()
            if (remoteTouch != null) {
                // We use BoxWithConstraints or just the fillMaxSize nature here
                // Note: The touch is rendered relative to the full screen since mirror is fillMaxSize
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val localPoint = com.example.connect.util.CoordinateMapper.fromAbsolute(
                        nx = remoteTouch!!.x.toInt(),
                        ny = remoteTouch!!.y.toInt(),
                        viewWidth = size.width,
                        viewHeight = size.height
                    )
                    
                    drawCircle(
                        color = ConnectDesign.NeonBlue.copy(alpha = 0.7f),
                        radius = 10.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(localPoint.x, localPoint.y),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(localPoint.x, localPoint.y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Premium Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isStreaming) {
                // Control strip
                Surface(
                    color = ConnectDesign.SurfaceDark.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.shadow(12.dp, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE SESSION",
                            color = ConnectDesign.NeonPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.stopMirroring() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text("TERMINATE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.stopMirroring() },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.Red),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("BACK TO CENTER", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    } // End Box
}

@Composable
fun StatusHUD(fps: Int, latency: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = ConnectDesign.SurfaceDark.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ConnectDesign.SuccessGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${fps}FPS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${latency}MS",
                color = ConnectDesign.NeonBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
