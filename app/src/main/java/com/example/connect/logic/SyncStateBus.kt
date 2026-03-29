package com.example.connect.logic

import com.example.connect.proto.SessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * SyncStateBus acts as a centralized event bus for synchronized state updates.
 * This allows the MirrorService (background) to notify the ConnectViewModel (UI)
 * without competing for UDP ports.
 */
object SyncStateBus {
    private val _remoteStateFlow = MutableSharedFlow<SessionState>(extraBufferCapacity = 10)
    val remoteStateFlow = _remoteStateFlow.asSharedFlow()

    private val _remoteControlFlow = MutableSharedFlow<com.example.connect.proto.ControlVector>(extraBufferCapacity = 50)
    val remoteControlFlow = _remoteControlFlow.asSharedFlow()

    suspend fun publishState(state: SessionState) {
        _remoteStateFlow.emit(state)
    }

    suspend fun publishControl(vector: com.example.connect.proto.ControlVector) {
        _remoteControlFlow.emit(vector)
    }
}
