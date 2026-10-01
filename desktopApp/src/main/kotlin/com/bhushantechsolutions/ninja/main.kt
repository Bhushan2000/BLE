package com.bhushantechsolutions.ninja

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Ninja",
    ) {
        App()
    }
}