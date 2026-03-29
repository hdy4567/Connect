package com.example.connect.logic

import com.example.connect.proto.AppState
import com.example.connect.util.ProtobufUtils
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SecurityLogicTest {

    @Test
    fun testSaltedHashSecurity() {
        val deviceId = "test-device-123"
        val state = AppState.MIRRORING
        val secret = "TEST_SECRET"

        // 1. Create with security
        val sessionState = ProtobufUtils.createSessionState(
            deviceId, state, 1920, 1080, 85, secret
        )

        // 2. Verify security
        assertTrue(
            ProtobufUtils.verifySecurity(sessionState, secret),
            "Security verification failed with correct secret"
        )

        // 3. Verify failure with wrong secret
        assertFalse(
            ProtobufUtils.verifySecurity(sessionState, "WRONG_SECRET"),
            "Security verification should have failed with wrong secret"
        )
        
        println("Security Logic Test Passed: Salt=${sessionState.securitySalt}, Hash=${sessionState.securityHash}")
    }
}
