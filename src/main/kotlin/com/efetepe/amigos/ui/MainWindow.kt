package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository

enum class MainTab { FRIENDS, LOG, SETTINGS }

@Composable
fun MainWindowContent(
    friendRepo: FriendRepository,
    settingsRepo: SettingsRepository
) {
    var selectedTab by remember { mutableStateOf(MainTab.FRIENDS) }

    AmigosTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.padding(top = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Friends") },
                    selected = selectedTab == MainTab.FRIENDS,
                    onClick = { selectedTab = MainTab.FRIENDS }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Log") },
                    label = { Text("Log") },
                    selected = selectedTab == MainTab.LOG,
                    onClick = { selectedTab = MainTab.LOG }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS }
                )
            }

            // Content area
            when (selectedTab) {
                MainTab.FRIENDS -> FriendsTabContent(friendRepo)
                MainTab.LOG -> {
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
                    LogViewContent(entries = entries)
                }
                MainTab.SETTINGS -> {
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
                        }
                    )
                }
            }
        }
    }
}
