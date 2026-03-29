package com.example.connect

import com.example.connect.util.ProtobufUtils
import com.example.connect.proto.AppState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtobufSerializationTest {

    @Test
    fun testSessionStateSerialization() {
        val deviceId = "test-device-001"
        val state = AppState.MIRRORING
        val width = 1920
        val height = 1080
        val battery = 85

        val session = ProtobufUtils.createSessionState(
            deviceId, state, width, height, battery
        )
        val serialized = session.toByteArray()

        assertNotNull(serialized)
        assertTrue(serialized.isNotEmpty())

        val deserialized = ProtobufUtils.parseSessionState(serialized)
        assertNotNull(deserialized)
        assertEquals(deviceId, deserialized?.deviceId)
        assertEquals(state, deserialized?.appState)
        assertEquals(width, deserialized?.deviceInfo?.screenWidth)
        assertEquals(battery, deserialized?.deviceInfo?.batteryLevel)
        
        println("Protobuf Serialization Test Passed: Size=${serialized.size} bytes")
    }
}
