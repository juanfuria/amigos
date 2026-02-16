package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efetepe.amigos.data.models.ChannelType

data class ChannelInput(
    val type: ChannelType = ChannelType.WHATSAPP,
    val address: String = "",
    val preferred: Boolean = false
)

@Composable
fun AddFriendDialogContent(
    initialName: String = "",
    initialNotes: String = "",
    initialFrequency: Int = 30,
    initialChannels: List<ChannelInput> = listOf(ChannelInput()),
    onSave: (name: String, notes: String?, frequency: Int, channels: List<ChannelInput>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var notes by remember { mutableStateOf(initialNotes) }
    var frequency by remember { mutableStateOf(initialFrequency.toString()) }
    var channels by remember { mutableStateOf(initialChannels) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Friend", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it.filter { ch -> ch.isDigit() } },
                label = { Text("Contact every N days") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Contact Channels", style = MaterialTheme.typography.titleSmall)

            channels.forEachIndexed { index, channel ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(0.35f)) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(channel.type.displayName)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ChannelType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        channels = channels.toMutableList().apply {
                                            this[index] = channel.copy(type = type)
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = channel.address,
                        onValueChange = { newAddr ->
                            channels = channels.toMutableList().apply {
                                this[index] = channel.copy(address = newAddr)
                            }
                        },
                        label = { Text("Address") },
                        modifier = Modifier.weight(0.65f)
                    )
                }
            }

            TextButton(onClick = { channels = channels + ChannelInput() }) {
                Text("+ Add Channel")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val freq = frequency.toIntOrNull() ?: 30
                        val validChannels = channels.filter { it.address.isNotBlank() }
                            .mapIndexed { i, c -> if (i == 0) c.copy(preferred = true) else c }
                        onSave(name, notes.ifBlank { null }, freq, validChannels)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && channels.any { it.address.isNotBlank() }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
