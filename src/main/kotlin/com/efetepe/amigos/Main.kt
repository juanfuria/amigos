package com.efetepe.amigos

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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

enum class AppScreen { NONE, ACTION, ADD_FRIEND, LOG, SETTINGS }

fun main() = application {
    val db = remember { DatabaseFactory.create() }
    val friendRepo = remember { FriendRepository(db) }
    val settingsRepo = remember { SettingsRepository(db) }

    var currentScreen by remember { mutableStateOf(AppScreen.NONE) }
    var currentNudge by remember { mutableStateOf<NudgeResult?>(null) }

    val scheduler = remember {
        Scheduler(friendRepo, settingsRepo) { nudge ->
            currentNudge = nudge
            currentScreen = AppScreen.ACTION
        }
    }

    LaunchedEffect(Unit) {
        scheduler.start(CoroutineScope(Dispatchers.Default + SupervisorJob()))
    }

    val trayIcon = remember { createTrayIcon() }

    Tray(
        icon = trayIcon,
        tooltip = "Amigos",
        onAction = { currentScreen = AppScreen.ADD_FRIEND },
        menu = {
            Item("Add Friend") { currentScreen = AppScreen.ADD_FRIEND }
            Item("View Log") { currentScreen = AppScreen.LOG }
            Item("Settings") { currentScreen = AppScreen.SETTINGS }
            Separator()
            Item("Quit") { scheduler.stop(); exitApplication() }
        }
    )

    when (currentScreen) {
        AppScreen.ACTION -> {
            val nudge = currentNudge
            if (nudge != null) {
                Window(
                    onCloseRequest = { currentScreen = AppScreen.NONE },
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
                            currentScreen = AppScreen.NONE
                        },
                        onSnooze = { currentScreen = AppScreen.NONE },
                        onSkip = {
                            friendRepo.markSkipped(nudge.logId)
                            currentScreen = AppScreen.NONE
                        }
                    )
                }
            }
        }

        AppScreen.ADD_FRIEND -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE },
                title = "Amigos \u2014 Add Friend",
                state = rememberWindowState(width = 500.dp, height = 600.dp)
            ) {
                AddFriendDialogContent(
                    onSave = { name, notes, frequency, channels ->
                        friendRepo.addFriend(name, notes, frequency.toLong())
                        val friend = friendRepo.getAllFriends().last()
                        channels.forEach { ch ->
                            friendRepo.addChannel(friend.id, ch.type, ch.address, ch.preferred)
                        }
                        currentScreen = AppScreen.NONE
                    },
                    onCancel = { currentScreen = AppScreen.NONE }
                )
            }
        }

        AppScreen.LOG -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE },
                title = "Amigos \u2014 Contact Log",
                state = rememberWindowState(width = 600.dp, height = 500.dp)
            ) {
                val logs = friendRepo.getAllContactLogs()
                val entries = logs.map { log ->
                    val friend = friendRepo.getFriend(log.friend_id)
                    LogEntry(
                        friendName = friend?.name ?: "Unknown",
                        channelType = log.channel_type,
                        promptedAt = log.prompted_at,
                        contactedAt = log.contacted_at,
                        skipped = log.skipped != 0L
                    )
                }
                LogViewContent(entries = entries, onClose = { currentScreen = AppScreen.NONE })
            }
        }

        AppScreen.SETTINGS -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE },
                title = "Amigos \u2014 Settings",
                state = rememberWindowState(width = 500.dp, height = 500.dp)
            ) {
                SettingsViewContent(
                    nudgesPerWeek = settingsRepo.nudgesPerWeek,
                    activeStart = settingsRepo.activeHoursStart,
                    activeEnd = settingsRepo.activeHoursEnd,
                    notificationDays = settingsRepo.notificationDays,
                    onSave = { nudges, start, end, days ->
                        settingsRepo.nudgesPerWeek = nudges
                        settingsRepo.activeHoursStart = start
                        settingsRepo.activeHoursEnd = end
                        settingsRepo.notificationDays = days
                        currentScreen = AppScreen.NONE
                    },
                    onClose = { currentScreen = AppScreen.NONE }
                )
            }
        }

        AppScreen.NONE -> { /* No window shown, app lives in tray */ }
    }
}

/**
 * Creates a tray icon as a simple colored circle using AWT BufferedImage,
 * then converts it to a Compose BitmapPainter for the Tray composable.
 */
private fun createTrayIcon(): BitmapPainter {
    val image = java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.color = java.awt.Color(0x6750A4) // Material purple
        g.fillOval(4, 4, 24, 24)
        g.dispose()
    }
    return BitmapPainter(image.toComposeImageBitmap())
}
