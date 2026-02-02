package com.timberglund.ledstrip

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BluetoothTesterTest {

    @Test
    fun `should convert bytes to hex string`() {
        // Simple utility test as a scaffold
        val bytes = byteArrayOf(0x01, 0x02, 0x0F, 0xFF.toByte())
        val hexString = bytes.joinToString("") { "%02x".format(it) }

        assertEquals("01020fff", hexString)
    }
}
