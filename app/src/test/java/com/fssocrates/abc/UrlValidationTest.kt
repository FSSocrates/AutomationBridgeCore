package com.fssocrates.abc

import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class UrlValidationTest {
    private val allowed = setOf("https", "http")

    private fun isUrlSafe(url: String): Boolean = try {
        val uri = URI(url.trim())
        uri.scheme?.lowercase() in allowed && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }

    @Test fun httpsOk() = assertTrue(isUrlSafe("https://example.com"))
    @Test fun httpOk() = assertTrue(isUrlSafe("http://example.com"))
    @Test fun javascriptRejected() = assertFalse(isUrlSafe("javascript:alert(1)"))
    @Test fun blankRejected() = assertFalse(isUrlSafe(""))
}
