package com.example.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.ui.theme.ConnectDesign

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonCard(
    title: String,
    description: String,
    badge: String,
    glowColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectDesign.SurfaceDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(
                    listOf(glowColor.copy(alpha = 0.05f), Color.Transparent),
                    radius = 500f
                ))
                .padding(28.dp)
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(glowColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = glowColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ConnectHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 8.sp
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = ConnectDesign.NeonBlue,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
