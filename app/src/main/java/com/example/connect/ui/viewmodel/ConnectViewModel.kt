package com.example.connect.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.example.connect.logic.SyncStateBus
import com.example.connect.mirror.HidDataSender
import com.example.connect.logic.TouchSmoother
import com.example.connect.logic.SessionRepository
import com.example.connect.proto.AppState as ProtoAppState
import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import com.example.connect.logic.ProtocolManager
import com.example.connect.mirror.MediaDecoder
import com.example.connect.mirror.UdpTransport
import com.example.connect.mirror.MirrorSinkManager
import androidx.compose.runtime.LaunchedEffect
import com.example.connect.proto.SessionState as ProtoSessionState
import com.example.connect.config.NetworkConfig
import android.util.Log

enum class AppScreen {
    DISCOVERY, ROLE_SELECTION, HOME, TOUCHPAD, EXPANSION
}

enum class ConnectionRole {
    SERVER, RECEIVER
}

class ConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val appInstance = application
    private val sessionRepository = SessionRepository(application)
    val sessionState = sessionRepository.sessionState

    private val _currentScreen = MutableStateFlow(AppScreen.DISCOVERY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    init {
        // Phase 9: Unified State Sync - Observe Bus instead of local UDP port
        viewModelScope.launch {
            SyncStateBus.remoteStateFlow.collect { state ->
                handleRemoteState(state)
            }
        }
        
        // Phase 9: Update UI feedback for remote touch
        viewModelScope.launch {
            SyncStateBus.remoteControlFlow.collect { vector ->
                _remoteTouchPoint.value = android.graphics.PointF(vector.x, vector.y)
            }
        }
    }

    private val _onMinimizeRequest = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val onMinimizeRequest = _onMinimizeRequest.asSharedFlow()
    
    // Permission status is now managed by MainActivity as a pre-gate
    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
    }

    private val _isMirroringActive = MutableStateFlow(false)
    val isMirroringActive: StateFlow<Boolean> = _isMirroringActive.asStateFlow()

    fun setMirroringActive(active: Boolean) {
        _isMirroringActive.value = active
    }

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _remoteTouchPoint = MutableStateFlow<android.graphics.PointF?>(null)
    val remoteTouchPoint: StateFlow<android.graphics.PointF?> = _remoteTouchPoint.asStateFlow()

    private val _isRelativeMouseMode = MutableStateFlow(false)
    val isRelativeMouseMode: StateFlow<Boolean> = _isRelativeMouseMode.asStateFlow()

    fun setRelativeMouseMode(enabled: Boolean) {
        _isRelativeMouseMode.value = enabled
        touchSmoother.reset(0f, 0f)
    }

    // Phase 6: Connection State
    private val _foundPeers = MutableStateFlow<List<android.net.wifi.p2p.WifiP2pDevice>>(emptyList())
    val foundPeers: StateFlow<List<android.net.wifi.p2p.WifiP2pDevice>> = _foundPeers.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isHandshakeComplete = MutableStateFlow(false)
    val isHandshakeComplete: StateFlow<Boolean> = _isHandshakeComplete.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _handshakeStatus = MutableStateFlow<String?>(null)
    val handshakeStatus: StateFlow<String?> = _handshakeStatus.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _latency = MutableStateFlow(0L)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    fun updateMetrics(fps: Int, latency: Long) {
        _fps.value = fps
        _latency.value = latency
    }

    fun clearError() { _errorMessage.value = null }
    
    var destinationIp: String = com.example.connect.config.NetworkConfig.DEFAULT_DEST_ADDRESS
    
    private var wifiDirectManager: com.example.connect.util.WifiDirectManager? = null
    private val protocolManager = ProtocolManager(android.os.Build.MODEL)

    // Phase 6: Role & Device Detection
    val isTablet: Boolean = application.resources.configuration.smallestScreenWidthDp >= 600
    
    private val _assignedRole = MutableStateFlow(if (isTablet) ConnectionRole.RECEIVER else ConnectionRole.SERVER)
    val assignedRole: StateFlow<ConnectionRole> = _assignedRole.asStateFlow()

    fun setRole(role: ConnectionRole) {
        _assignedRole.value = role
    }

    fun setWifiDirectManager(manager: com.example.connect.util.WifiDirectManager) {
        this.wifiDirectManager = manager
    }

    fun startDiscovery() {
        _foundPeers.value = emptyList()
        wifiDirectManager?.startDiscovery()
    }

    fun connectToPeer(device: android.net.wifi.p2p.WifiP2pDevice) {
        // Assume GO intent 15 for simplicity here, or we can negotiate
        wifiDirectManager?.connect(device, 15)
    }

    fun onPeerFound(device: android.net.wifi.p2p.WifiP2pDevice) {
        if (!_foundPeers.value.any { it.deviceAddress == device.deviceAddress }) {
            _foundPeers.value = _foundPeers.value + device
        }
    }

    fun onConnectionReady(goAddress: String) {
        try {
            if (goAddress.isBlank() || goAddress == "0.0.0.0") {
                Log.w("ConnectViewModel", "Invalid GO address received: $goAddress")
                return
            }
            destinationIp = goAddress
            _isConnected.value = true
            
            // Phase 9: Start background sync early
            startBackgroundService(if (assignedRole.value == ConnectionRole.SERVER) "START_SOURCE" else "START_SINK")

            // Phase 10: Robust Handshake - Wait for UDP parity before navigating
            startHandshake()
        } catch (e: Exception) {
            Log.e("Connect:CAUGHT", "Failed to initialize connection: ${e.message}", e)
            onDisconnected()
        }
    }
    
    fun confirmRole() {
        navigateTo(AppScreen.HOME)
    }

    private var handshakeJob: Job? = null
    private fun startHandshake() {
        handshakeJob?.cancel()
        handshakeJob = viewModelScope.launch(Dispatchers.IO) {
            Log.i("Connect:SYNC", "Starting Robust Handshake with $destinationIp")
            _handshakeStatus.value = "기기 검증 중..."
            var attempts = 0
            while (!_isHandshakeComplete.value && attempts < 20) {
                if (attempts == 5) _handshakeStatus.value = "동기화 채널 최적화 중..."
                if (attempts == 12) _handshakeStatus.value = "보안 데이터 암호화 중..."
                
                // Send a generic IDLE state as a 'Ping'
                sendStateUpdate(com.example.connect.proto.AppState.IDLE)
                delay(500) // Ping every 500ms
                attempts++
            }
            if (!_isHandshakeComplete.value) {
                Log.e("Connect:SYNC", "Handshake TIMEOUT")
                _handshakeStatus.value = null
                onDisconnected("연결 시도 시간 초과 (UDP handshake failed)")
            } else {
                _handshakeStatus.value = "연결 성공!"
                delay(500)
                _handshakeStatus.value = null
            }
        }
    }

    private var heartbeatJob: Job? = null
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                sendStateUpdate(com.example.connect.proto.AppState.IDLE)
                delay(2000) // 2s heartbeat
            }
        }
    }

    private fun handleRemoteState(state: com.example.connect.proto.SessionState) {
        viewModelScope.launch {
            Log.i("Connect:SYNC", "Received Remote State: ${state.appState} from ${state.deviceId}")
            
            // Handshake Success Check
            if (!_isHandshakeComplete.value) {
                Log.i("Connect:SYNC", "Handshake SUCCESS with ${state.deviceId}")
                _isHandshakeComplete.value = true
                if (_currentScreen.value == AppScreen.DISCOVERY) {
                    navigateTo(AppScreen.ROLE_SELECTION)
                }
            }

            // Phase 4/9: Auto-Role Selection (Master Dictatorship)
            if (_currentScreen.value == AppScreen.ROLE_SELECTION) {
                Log.i("Connect:SYNC", "Master Presence Detected. Locking Slave to RECEIVER.")
                _assignedRole.value = ConnectionRole.RECEIVER
                navigateTo(AppScreen.HOME)
                return@launch
            }

            // Master Dictator Pattern: RECEIVER must follow Master
            if (assignedRole.value == ConnectionRole.RECEIVER) {
                when (state.appState) {
                    com.example.connect.proto.AppState.MIRRORING -> {
                        if (_currentScreen.value != AppScreen.EXPANSION) {
                            Log.i("Connect:SYNC", "Forcing Navigation to EXPANSION")
                            navigateTo(AppScreen.EXPANSION)
                        }
                    }
                    com.example.connect.proto.AppState.CONTROL_ONLY -> {
                        if (_currentScreen.value != AppScreen.TOUCHPAD) {
                            Log.i("Connect:SYNC", "Forcing Navigation to TOUCHPAD")
                            navigateTo(AppScreen.TOUCHPAD)
                            _onMinimizeRequest.emit(Unit) 
                        }
                    }
                    com.example.connect.proto.AppState.IDLE -> {
                        if (_currentScreen.value != AppScreen.HOME) {
                            Log.i("Connect:SYNC", "Forcing Navigation to HOME")
                            navigateTo(AppScreen.HOME)
                        }
                    }
                    else -> {}
                }
            } else {
                // Transmitter side: Handle MIRRORING request from Receiver
                if (state.appState == com.example.connect.proto.AppState.MIRRORING && _currentScreen.value == AppScreen.HOME) {
                    Log.i("Connect:SYNC", "Peer requested Mirroring. Triggering Projection.")
                    _remoteRequestMirroring.value = true
                }
            }
        }
    }

    private val _remoteRequestMirroring = MutableStateFlow(false)
    val remoteRequestMirroring: StateFlow<Boolean> = _remoteRequestMirroring.asStateFlow()

    fun resetRemoteRequest() { _remoteRequestMirroring.value = false }

    fun sendStateUpdate(state: com.example.connect.proto.AppState) {
        viewModelScope.launch(Dispatchers.IO) {
            val packet = protocolManager.createDiscoveryPacket(
                appState = state,
                width = 1920, 
                height = 1080,
                battery = 100
            )
            protocolManager.sendOneShot(destinationIp, packet)
        }
    }

    fun onDisconnected(reason: String? = null) {
        Log.w("Connect:SYNC", "Disconnect Triggered: $reason")
        _isConnected.value = false
        _isHandshakeComplete.value = false
        _errorMessage.value = reason
        
        destinationIp = com.example.connect.config.NetworkConfig.DEFAULT_DEST_ADDRESS
        
        // Full Cleanup
        cleanup()
        
        // Return to discovery
        if (_currentScreen.value != AppScreen.DISCOVERY) {
            navigateTo(AppScreen.DISCOVERY)
            startDiscovery()
        }
    }

    private var hidDataSender: HidDataSender? = null
    private val touchSmoother = TouchSmoother()

    fun assignRole(role: ConnectionRole) {
        _assignedRole.value = role
        if (role == ConnectionRole.SERVER) {
            // Master chooses Server -> Tell Receiver to be RECEIVER
            sendStateUpdate(com.example.connect.proto.AppState.IDLE) // This packet carries the device info and 'Master' presence
        }
        navigateTo(AppScreen.HOME)
    }
    
    fun initHidDataSender(sender: HidDataSender) {
        this.hidDataSender = sender
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        saveSession(screen)
        
        // Phase 8: Consolidated Network Sync
        if (_isConnected.value) {
            val protoState = when (screen) {
                AppScreen.EXPANSION -> com.example.connect.proto.AppState.MIRRORING
                AppScreen.TOUCHPAD -> com.example.connect.proto.AppState.CONTROL_ONLY
                else -> com.example.connect.proto.AppState.IDLE
            }
            sendStateUpdate(protoState)
        }
    }

    private fun saveSession(screen: AppScreen) {
        viewModelScope.launch {
            sessionRepository.updateSession { builder ->
                val protoState = when (screen) {
                    AppScreen.DISCOVERY -> ProtoAppState.IDLE
                    AppScreen.ROLE_SELECTION -> ProtoAppState.IDLE
                    AppScreen.HOME -> ProtoAppState.IDLE
                    AppScreen.TOUCHPAD -> ProtoAppState.CONTROL_ONLY
                    AppScreen.EXPANSION -> ProtoAppState.MIRRORING
                }
                builder.appState = protoState
            }
        }
    }

    fun setMirroringStatus(active: Boolean) {
        _isMirroringActive.value = active
    }

    private var lastScrollY = 0f
    private var touchDownTime = 0L
    private var prevTouchX = -1f
    private var prevTouchY = -1f

    fun handleMultiTouch(touches: List<android.graphics.PointF>, xMax: Float, yMax: Float): Pair<Int, Int>? {
        if (touches.isEmpty()) {
            touchDownTime = 0
            prevTouchX = -1f
            prevTouchY = -1f
            touchSmoother.reset(0f, 0f)
            hidDataSender?.sendAbsoluteTouch(0, 0, false)
            _remoteTouchPoint.value = null
            return null
        }

        if (touchDownTime == 0L) touchDownTime = System.currentTimeMillis()

        when (touches.size) {
            1 -> {
                val touch = touches[0]
                if (_isRelativeMouseMode.value) {
                    if (prevTouchX != -1f && prevTouchY != -1f) {
                        val dx = touch.x - prevTouchX
                        val dy = touch.y - prevTouchY
                        val (sx, sy) = touchSmoother.smooth(dx, dy)
                        hidDataSender?.sendMouseMovement(sx.toInt(), sy.toInt())
                        _remoteTouchPoint.value = android.graphics.PointF(sx, sy) 
                    }
                    prevTouchX = touch.x
                    prevTouchY = touch.y
                    return null
                } else {
                    val (sx, sy) = touchSmoother.smooth(touch.x, touch.y)
                    val mapped = com.example.connect.util.CoordinateMapper.toAbsolute(sx, sy, xMax, yMax)
                    
                    if (mapped != null) {
                        val (nx, ny) = mapped
                        hidDataSender?.sendAbsoluteTouch(nx, ny, true)
                        _remoteTouchPoint.value = android.graphics.PointF(nx.toFloat(), ny.toFloat())
                        
                        if (assignedRole.value == ConnectionRole.RECEIVER) {
                            sendRemoteControlPacket(nx, ny, true)
                        }
                        return mapped
                    }
                }
            }
            2 -> {
                val center = (touches[0].y + touches[1].y) / 2
                if (lastScrollY != 0f) {
                    val delta = (center - lastScrollY).toInt()
                    if (Math.abs(delta) > 5) {
                        hidDataSender?.sendMouseReport(0, (-delta).coerceIn(-127, 127).toByte())
                    }
                }
                lastScrollY = center
            }
        }
        return null
    }

    fun handleMouseClick(isRightClick: Boolean) {
        val buttons = if (isRightClick) 0x02.toByte() else 0x04.toByte()
        hidDataSender?.sendMouseReport(buttons, 0)
        // Auto-release
        viewModelScope.launch {
            delay(50)
            hidDataSender?.sendMouseReport(0, 0)
        }
    }

    fun handleMouseMove(dx: Float, dy: Float) {
        val (sx, sy) = touchSmoother.smooth(dx, dy)
        hidDataSender?.sendMouseMovement(sx.toInt(), sy.toInt())
    }

    private fun sendRemoteControlPacket(x: Int, y: Int, isClicked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val packet = com.example.connect.util.ProtobufUtils.serializeControlVector(x.toFloat(), y.toFloat(), isClicked)
            protocolManager.sendOneShot(destinationIp, packet)
        }
    }

    private val sinkManager = MirrorSinkManager(viewModelScope) { fps, latency ->
        updateMetrics(fps, latency)
    }

    fun startSink(surface: android.view.Surface) {
        sinkManager.start(surface) { 
            _isStreaming.value = true 
        }
    }

    fun stopSink() {
        sinkManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    fun stopMirroring() {
        _isStreaming.value = false
        stopSink()
        navigateTo(AppScreen.HOME)
        // Sync remote to stop as well
        sendStateUpdate(com.example.connect.proto.AppState.IDLE)
    }

    fun cleanup() {
        stopSink()
        // Phase 10: Persistent Continuity - Don't kill service, just stop functions
        startBackgroundService("STOP_FUNCTION")
        hidDataSender?.unregister()
    }

    private fun startBackgroundService(action: String) {
        val intent = android.content.Intent(appInstance, com.example.connect.mirror.MirrorService::class.java).apply {
            this.action = action
            putExtra("DESTINATION_IP", destinationIp)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appInstance.startForegroundService(intent)
        } else {
            appInstance.startService(intent)
        }
    }

    private fun stopMirrorService() {
        val intent = android.content.Intent(appInstance, com.example.connect.mirror.MirrorService::class.java)
        appInstance.stopService(intent)
    }
}
