package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LogEntry(
    val friendName: String,
    val channelType: String,
    val promptedAt: String,
    val contactedAt: String?,
    val skipped: Boolean
)

@Composable
fun LogViewContent(entries: List<LogEntry>, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Contact Log", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text("No contact history yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.friendName, style = MaterialTheme.typography.titleSmall)
                                Text("via ${entry.channelType} — ${entry.promptedAt}")
                                Text(
                                    when {
                                        entry.skipped -> "Skipped"
                                        entry.contactedAt != null -> "Contacted: ${entry.contactedAt}"
                                        else -> "Pending"
                                    },
                                    color = when {
                                        entry.skipped -> MaterialTheme.colorScheme.error
                                        entry.contactedAt != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
