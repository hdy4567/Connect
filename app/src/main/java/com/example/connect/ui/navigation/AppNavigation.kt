package com.example.connect.ui.navigation

import android.util.Log
import androidx.compose.runtime.*
import com.example.connect.ui.screens.*
import com.example.connect.ui.viewmodel.AppScreen
import com.example.connect.ui.viewmodel.ConnectViewModel
import com.example.connect.ui.viewmodel.ConnectionRole

@Composable
fun AppNavigation(
    viewModel: ConnectViewModel,
    onStartProjection: () -> Unit,
    onMinimize: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val assignedRole by viewModel.assignedRole.collectAsState()
    val foundPeers by viewModel.foundPeers.collectAsState()
    val remoteRequestMirroring by viewModel.remoteRequestMirroring.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val handshakeStatus by viewModel.handshakeStatus.collectAsState()

    // Phase 4: Auto-Projection on Remote Request
    // Phase 9: Minimize to background
    LaunchedEffect(Unit) {
        viewModel.onMinimizeRequest.collect {
            onMinimize()
        }
    }

    LaunchedEffect(remoteRequestMirroring) {
        if (remoteRequestMirroring && currentScreen == AppScreen.HOME) {
            onStartProjection()
            viewModel.resetRemoteRequest()
        }
    }

    // Lazy Discovery Start
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.DISCOVERY) {
            viewModel.startDiscovery()
        }
    }

    when (currentScreen) {
        AppScreen.DISCOVERY -> DiscoveryScreen(
            role = assignedRole,
            peers = foundPeers,
            errorMessage = errorMessage,
            handshakeStatus = handshakeStatus,
            onClearError = { viewModel.clearError() },
            onConnectClick = { peer ->
                try {
                    viewModel.connectToPeer(peer)
                } catch (e: Exception) {
                    Log.e("AppNavigation", "Connect click error: ${e.message}")
                }
            }
        )
        AppScreen.ROLE_SELECTION -> RoleSelectionScreen(
            assignedRole = assignedRole,
            onConfirm = { viewModel.assignRole(assignedRole) }
        )
        AppScreen.HOME -> ModeSelectionScreen(
            role = assignedRole,
            onHostClick = { 
                viewModel.navigateTo(AppScreen.EXPANSION)
                onStartProjection() 
            },
            onClientViewClick = { viewModel.navigateTo(AppScreen.EXPANSION) },
            onClientTouchClick = { viewModel.navigateTo(AppScreen.TOUCHPAD) }
        )
        AppScreen.TOUCHPAD -> TouchpadScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.cleanup()
                viewModel.navigateTo(AppScreen.HOME)
            }
        )
        AppScreen.EXPANSION -> ExpansionScreen(
            viewModel = viewModel
        )
    }
}
