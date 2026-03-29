package com.example.connect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.theme.ConnectDesign
import com.example.connect.ui.components.ConnectHeader
import com.example.connect.ui.components.NeonCard

@Composable
fun PermissionScreen(
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ConnectHeader(
            title = "PHASE 1",
            subtitle = "SYSTEM PERMISSIONS & PROTOCOL"
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        NeonCard(
            title = "초기 권한 설정",
            description = "안정적인 송출을 위해 모든 앱 위에 그리기 및 미디어 프로젝션 권한을 부여합니다. (모든 앱 송출 우선)",
            badge = "Essential",
            glowColor = ConnectDesign.NeonBlue,
            onClick = onGrantClick
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "PERMISSIONS REQUIRED FOR 60FPS SINK/SOURCE",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
