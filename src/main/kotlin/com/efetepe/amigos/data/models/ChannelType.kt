package com.efetepe.amigos.data.models

enum class ChannelType(val displayName: String, val deepLinkPrefix: String) {
    WHATSAPP("WhatsApp", "https://wa.me/"),
    IMESSAGE("iMessage", "imessage://"),
    EMAIL("Email", "mailto:"),
    SMS("SMS", "sms:"),
    TELEGRAM("Telegram", "https://t.me/");

    fun buildDeepLink(address: String): String = "$deepLinkPrefix$address"
}
