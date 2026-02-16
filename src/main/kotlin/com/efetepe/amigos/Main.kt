package com.efetepe.amigos

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Amigos") {
        Text("Amigos is running!")
    }
}
