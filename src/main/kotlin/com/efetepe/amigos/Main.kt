package com.efetepe.amigos

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.efetepe.amigos.data.DatabaseFactory
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.scheduler.NudgeResult
import com.efetepe.amigos.scheduler.Scheduler
import com.efetepe.amigos.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    LaunchAgentInstaller.installIfNeeded()

    application {
    val db = remember { DatabaseFactory.create() }
    val friendRepo = remember { FriendRepository(db) }
    val settingsRepo = remember { SettingsRepository(db) }

    var mainWindowVisible by remember { mutableStateOf(true) }
    var showNudgePopup by remember { mutableStateOf(false) }
    var currentNudge by remember { mutableStateOf<NudgeResult?>(null) }

    val scheduler = remember {
        Scheduler(friendRepo, settingsRepo) { nudge ->
            currentNudge = nudge
            showNudgePopup = true
        }
    }

    LaunchedEffect(Unit) {
        scheduler.start(CoroutineScope(Dispatchers.Default + SupervisorJob()))
    }

    val trayIcon = remember { createTrayIcon() }

    Tray(
        icon = trayIcon,
        tooltip = "Amigos",
        onAction = { mainWindowVisible = true },
        menu = {
            Item("Open Amigos") { mainWindowVisible = true }
            Separator()
            Item("Quit") { scheduler.stop(); exitApplication() }
        }
    )

    // Main tabbed window
    if (mainWindowVisible) {
        Window(
            onCloseRequest = { mainWindowVisible = false },
            title = "Amigos",
            state = rememberWindowState(width = 800.dp, height = 600.dp)
        ) {
            MainWindowContent(friendRepo = friendRepo, settingsRepo = settingsRepo)
        }
    }

    // Nudge action popup (separate small window)
    if (showNudgePopup) {
        val nudge = currentNudge
        if (nudge != null) {
            Window(
                onCloseRequest = { showNudgePopup = false },
                title = "Amigos \u2014 Reach Out",
                state = rememberWindowState(width = 400.dp, height = 350.dp),
                resizable = false
            ) {
                val friend = friendRepo.getFriend(nudge.friendId)
                ActionWindowContent(
                    state = ActionWindowState(
                        friendName = nudge.friendName,
                        friendNotes = friend?.notes,
                        channelType = nudge.channelType,
                        address = nudge.address,
                        logId = nudge.logId
                    ),
                    onOpenApp = { DeepLinker.open(nudge.channelType, nudge.address) },
                    onDone = {
                        friendRepo.markContacted(nudge.logId)
                        showNudgePopup = false
                    },
                    onSnooze = { showNudgePopup = false },
                    onSkip = {
                        friendRepo.markSkipped(nudge.logId)
                        showNudgePopup = false
                    }
                )
            }
        }
    }
  }
}

private fun createTrayIcon(): BitmapPainter {
    val resourceStream = object {}.javaClass.getResourceAsStream("/tray-icon.png")
    if (resourceStream != null) {
        val image = javax.imageio.ImageIO.read(resourceStream)
        return BitmapPainter(image.toComposeImageBitmap())
    }

    // Fallback: purple circle
    val image = java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.color = java.awt.Color(0x6750A4)
        g.fillOval(4, 4, 24, 24)
        g.dispose()
    }
    return BitmapPainter(image.toComposeImageBitmap())
}
