package com.example.connect.logic

import android.util.Log
import com.example.connect.proto.AppState
import com.example.connect.proto.SessionState
import com.example.connect.util.ProtobufUtils
import com.example.connect.mirror.UdpTransport
import com.example.connect.config.NetworkConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ProtocolManager centralizes the exchange of state between devices.
 * It ensures that the Master and Slave stay in sync regarding mode and status.
 */
class ProtocolManager(private val deviceId: String) {

    private val _remoteState = MutableStateFlow<SessionState?>(null)
    val remoteState: StateFlow<SessionState?> = _remoteState.asStateFlow()

    /**
     * Serializes the current local state for transmission.
     */
    fun createDiscoveryPacket(appState: AppState, width: Int, height: Int, battery: Int): ByteArray {
        return ProtobufUtils.createSessionState(
            deviceId = deviceId,
            state = appState,
            width = width,
            height = height,
            battery = battery
        ).toByteArray()
    }

    /**
     * Handles incoming state updates from the remote device.
     */
    fun handleIncomingPacket(
        data: ByteArray, 
        onStateChanged: (SessionState) -> Unit,
        onControlReceived: (com.example.connect.proto.ControlVector) -> Unit
    ) {
        // Try parsing as SessionState first
        val state = ProtobufUtils.parseSessionState(data)
        if (state != null && ProtobufUtils.verifySecurity(state)) {
            _remoteState.value = state
            onStateChanged(state)
            return
        }

        // Try parsing as ControlVector (Reverse Touch)
        try {
            val vector = com.example.connect.proto.ControlVector.parseFrom(data)
            onControlReceived(vector)
        } catch (e: Exception) {
            // Not a control vector, or malformed
        }
    }

    /**
     * Sends a one-shot UDP packet avoiding boilerplate in ViewModel.
     */
    fun sendOneShot(destinationIp: String, packet: ByteArray) {
        try {
            if (destinationIp == NetworkConfig.DEFAULT_DEST_ADDRESS) return
            val transport = UdpTransport(destinationIp, NetworkConfig.CONTROL_PORT)
            transport.open()
            transport.send(packet)
            transport.close()
        } catch (e: Exception) {
            Log.e("ProtocolManager", "One-shot send FAILED: ${e.message}")
        }
    }
}
