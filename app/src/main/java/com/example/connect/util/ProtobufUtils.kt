package com.example.connect.util

import com.example.connect.proto.SessionState
import com.example.connect.proto.AppState
import com.example.connect.proto.DeviceInfo
import com.example.connect.proto.ControlVector
import android.util.Log

/**
 * Centralized Protobuf utilities to ensure consistent serialization across the app.
 */
object ProtobufUtils {

    fun createSessionState(
        deviceId: String,
        state: AppState,
        width: Int,
        height: Int,
        battery: Int,
        secret: String = "CONNECT_SHARED_SECRET"
    ): SessionState {
        val timestamp = System.currentTimeMillis()
        val salt = java.util.UUID.randomUUID().toString().take(8)
        val rawData = "$deviceId|$state|$timestamp|$salt"
        val hash = generateHash(rawData, secret)

        val deviceInfo = DeviceInfo.newBuilder()
            .setScreenWidth(width)
            .setScreenHeight(height)
            .setBatteryLevel(battery)
            .build()

        return SessionState.newBuilder()
            .setDeviceId(deviceId)
            .setTimestamp(timestamp)
            .setAppState(state)
            .setDeviceInfo(deviceInfo)
            .setSecuritySalt(salt)
            .setSecurityHash(hash)
            .build()
    }

    private fun generateHash(data: String, secret: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((data + secret).toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun verifySecurity(state: SessionState, secret: String = "CONNECT_SHARED_SECRET"): Boolean {
        val rawData = "${state.deviceId}|${state.appState}|${state.timestamp}|${state.securitySalt}"
        val expectedHash = generateHash(rawData, secret)
        return expectedHash == state.securityHash
    }

    fun parseSessionState(data: ByteArray): SessionState? {
        return try {
            SessionState.parseFrom(data)
        } catch (e: Exception) {
            Log.e("ProtobufUtils", "Failed to parse SessionState: ${e.message}")
            null
        }
    }

    fun serializeControlVector(x: Float, y: Float, clicked: Boolean): ByteArray {
        return ControlVector.newBuilder()
            .setX(x)
            .setY(y)
            .setIsClicked(clicked)
            .build()
            .toByteArray()
    }
}
