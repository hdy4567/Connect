package com.example.connect.ui.screens

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.components.ConnectHeader
import com.example.connect.ui.components.RadarAnimation
import com.example.connect.ui.theme.ConnectDesign

@Composable
fun DiscoveryScreen(
    role: com.example.connect.ui.viewmodel.ConnectionRole,
    peers: List<WifiP2pDevice>,
    errorMessage: String? = null,
    handshakeStatus: String? = null,
    onClearError: () -> Unit = {},
    onConnectClick: (WifiP2pDevice) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            ConnectHeader(
                title = "RADAR",
                subtitle = if (role == com.example.connect.ui.viewmodel.ConnectionRole.SERVER) "READY AS SERVER [SENDER]" else "READY AS RECEIVER [SINK]"
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RadarAnimation(modifier = Modifier.size(280.dp))
                    Text(
                        text = "SCANNING...",
                        color = ConnectDesign.NeonBlue.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(peers) { peer ->
                        PeerCard(peer = peer, onClick = { onConnectClick(peer) })
                    }
                }
            }
        } // End Column

        // Handshake Progress Overlay (Stage 1 Professional UX)
        if (handshakeStatus != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = ConnectDesign.NeonPurple)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = handshakeStatus,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ESTABLISHING SECURE P2P CHANNEL",
                        color = ConnectDesign.NeonPurple.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        // Error Overlay
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "OK",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearError() }
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    } // End Box
}

@Composable
fun PeerCard(peer: WifiP2pDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectDesign.SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = peer.deviceName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = peer.deviceAddress,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "CONNECT",
                color = ConnectDesign.NeonBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
