package com.efetepe.amigos.notification

object MacNotifier {

    fun buildAppleScript(title: String, message: String, soundName: String = "default"): String {
        val escapedTitle = title.replace("\"", "\\\"")
        val escapedMessage = message.replace("\"", "\\\"")
        return """display notification "$escapedMessage" with title "$escapedTitle" sound name "$soundName""""
    }

    fun notify(title: String, message: String) {
        val script = buildAppleScript(title, message)
        ProcessBuilder("osascript", "-e", script)
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }
}
