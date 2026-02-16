package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.models.ChannelType
import kotlinx.datetime.Clock

private sealed class FriendsMode {
    data object List : FriendsMode()
    data object Add : FriendsMode()
    data class Edit(val friendId: Long) : FriendsMode()
}

@Composable
fun FriendsTabContent(friendRepo: FriendRepository) {
    var mode by remember { mutableStateOf<FriendsMode>(FriendsMode.List) }
    // Use a refresh counter to trigger recomposition when data changes
    var refreshKey by remember { mutableStateOf(0) }

    when (val currentMode = mode) {
        is FriendsMode.List -> {
            FriendsList(
                friendRepo = friendRepo,
                refreshKey = refreshKey,
                onAdd = { mode = FriendsMode.Add },
                onEdit = { id -> mode = FriendsMode.Edit(id) },
                onDeleted = { refreshKey++ }
            )
        }
        is FriendsMode.Add -> {
            AddFriendDialogContent(
                onSave = { name, notes, frequency, channels ->
                    friendRepo.addFriend(name, notes, frequency.toLong())
                    val friend = friendRepo.getAllFriends().last()
                    channels.forEach { ch ->
                        friendRepo.addChannel(friend.id, ch.type, ch.address, ch.preferred)
                    }
                    refreshKey++
                    mode = FriendsMode.List
                },
                onCancel = { mode = FriendsMode.List }
            )
        }
        is FriendsMode.Edit -> {
            val friend = friendRepo.getFriend(currentMode.friendId)
            if (friend != null) {
                val channels = friendRepo.getChannels(friend.id)
                AddFriendDialogContent(
                    title = "Edit Friend",
                    initialName = friend.name,
                    initialNotes = friend.notes ?: "",
                    initialFrequency = friend.desired_frequency_days.toInt(),
                    initialChannels = channels.map { ch ->
                        ChannelInput(
                            type = ChannelType.valueOf(ch.channel_type),
                            address = ch.address,
                            preferred = ch.preferred != 0L
                        )
                    }.ifEmpty { listOf(ChannelInput()) },
                    onSave = { name, notes, frequency, newChannels ->
                        friendRepo.updateFriend(friend.id, name, notes, frequency.toLong())
                        friendRepo.deleteChannelsByFriendId(friend.id)
                        newChannels.forEach { ch ->
                            friendRepo.addChannel(friend.id, ch.type, ch.address, ch.preferred)
                        }
                        refreshKey++
                        mode = FriendsMode.List
                    },
                    onCancel = { mode = FriendsMode.List }
                )
            } else {
                mode = FriendsMode.List
            }
        }
    }
}

@Composable
private fun FriendsList(
    friendRepo: FriendRepository,
    refreshKey: Int,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val friends = remember(refreshKey) { friendRepo.getAllFriends() }
    var friendToDelete by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Friends", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAdd) { Text("Add Friend") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (friends.isEmpty()) {
            Text(
                "No friends added yet. Click 'Add Friend' to get started.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(friends, key = { it.id }) { friend ->
                    val channels = remember(refreshKey) { friendRepo.getChannels(friend.id) }
                    val preferredChannel = channels.firstOrNull()
                    val lastContact = remember(refreshKey) { friendRepo.getLastContactDate(friend.id) }
                    val daysSince = lastContact?.let {
                        ((Clock.System.now() - it).inWholeDays).toString() + " days ago"
                    } ?: "Never"

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.name, style = MaterialTheme.typography.titleMedium)
                                if (preferredChannel != null) {
                                    Text(
                                        "${ChannelType.valueOf(preferredChannel.channel_type).displayName} — every ${friend.desired_frequency_days} days",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Last contact: $daysSince",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onEdit(friend.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { friendToDelete = friend.id }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    friendToDelete?.let { id ->
        val friend = friendRepo.getFriend(id)
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            title = { Text("Delete Friend") },
            text = { Text("Remove ${friend?.name ?: "this friend"} and all their contact history?") },
            confirmButton = {
                TextButton(onClick = {
                    friendRepo.deleteFriend(id)
                    friendToDelete = null
                    onDeleted()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { friendToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
