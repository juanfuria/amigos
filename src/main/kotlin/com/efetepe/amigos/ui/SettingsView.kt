package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsViewContent(
    nudgesPerWeek: Int,
    quietStart: String,
    quietEnd: String,
    notificationDays: List<String>,
    onSave: (nudges: Int, start: String, end: String, days: List<String>) -> Unit,
    onClose: () -> Unit
) {
    var nudges by remember { mutableStateOf(nudgesPerWeek.toString()) }
    var start by remember { mutableStateOf(quietStart) }
    var end by remember { mutableStateOf(quietEnd) }
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember { mutableStateOf(notificationDays.toSet()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }

            OutlinedTextField(
                value = nudges,
                onValueChange = { nudges = it.filter { ch -> ch.isDigit() } },
                label = { Text("Nudges per week") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Quiet hours start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("Quiet hours end") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Notification days", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                allDays.forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        },
                        label = { Text(day) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(nudges.toIntOrNull() ?: 3, start, end, selectedDays.toList().sorted())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
