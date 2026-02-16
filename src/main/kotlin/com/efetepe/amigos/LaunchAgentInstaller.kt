package com.efetepe.amigos

import java.io.File

object LaunchAgentInstaller {
    private const val LABEL = "com.efetepe.amigos"
    private val plistFile = File(
        System.getProperty("user.home"),
        "Library/LaunchAgents/$LABEL.plist"
    )

    fun installIfNeeded() {
        if (plistFile.exists()) return

        val appPath = resolveAppPath() ?: return
        val plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
              "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$LABEL</string>
                <key>ProgramArguments</key>
                <array>
                    <string>open</string>
                    <string>-a</string>
                    <string>$appPath</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>KeepAlive</key>
                <false/>
            </dict>
            </plist>
        """.trimIndent()

        plistFile.parentFile.mkdirs()
        plistFile.writeText(plistContent)
    }

    /**
     * Resolve the path to the .app bundle.
     * When running from a packaged app, java.home will be inside
     * /Applications/Amigos.app/Contents/runtime/Contents/Home.
     * Returns null if not running from a packaged .app (e.g. during development).
     */
    private fun resolveAppPath(): String? {
        val javaHome = System.getProperty("java.home") ?: return null
        val appSuffix = ".app"
        val idx = javaHome.indexOf(appSuffix)
        if (idx == -1) return null
        return javaHome.substring(0, idx + appSuffix.length)
    }
}
