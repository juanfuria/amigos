package com.efetepe.amigos.ui

import com.efetepe.amigos.data.models.ChannelType

object DeepLinker {
    fun open(channelType: ChannelType, address: String) {
        val url = channelType.buildDeepLink(address)
        ProcessBuilder("open", url)
            .redirectErrorStream(true)
            .start()
    }
}
