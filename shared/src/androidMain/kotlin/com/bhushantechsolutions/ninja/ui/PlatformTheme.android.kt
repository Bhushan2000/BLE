package com.bhushantechsolutions.ninja.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
actual fun PlatformSystemBarTheme(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (controller != null) {
                        val appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                        if (!isDark) {
                            controller.setSystemBarsAppearance(appearance, appearance)
                        } else {
                            controller.setSystemBarsAppearance(0, appearance)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    var flags = window.decorView.systemUiVisibility
                    flags = if (!isDark) {
                        flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    }
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = flags
                }
            }
        }
    }
}
