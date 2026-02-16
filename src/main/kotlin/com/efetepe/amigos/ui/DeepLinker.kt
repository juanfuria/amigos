package com.efetepe.amigos.ui

import com.efetepe.amigos.data.models.ChannelType
import java.awt.Desktop
import java.net.URI

object DeepLinker {
    fun open(channelType: ChannelType, address: String) {
        val uri = URI(channelType.buildDeepLink(address))
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(uri)
        }
    }
}
