package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthHeadersTest {
    @Test
    fun `adds bearer prefix when missing`() {
        val scheme = "Bearer"
        assertEquals("$scheme sample-key", bearerAuthorizationValue("sample-key"))
    }

    @Test
    fun `keeps bearer token unchanged when already prefixed`() {
        val scheme = "Bearer"
        val token = "$scheme existing-token"
        assertEquals(token, bearerAuthorizationValue(token))
    }
}
