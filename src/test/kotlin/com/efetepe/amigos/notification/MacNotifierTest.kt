package com.efetepe.amigos.notification

import kotlin.test.Test
import kotlin.test.assertTrue

class MacNotifierTest {
    @Test
    fun `buildScript generates valid osascript command`() {
        val script = MacNotifier.buildAppleScript(
            title = "Amigos",
            message = "Time to reach out to Alice via WhatsApp",
            soundName = "default"
        )
        assertTrue(script.contains("display notification"))
        assertTrue(script.contains("Alice"))
        assertTrue(script.contains("Amigos"))
    }
}
