package com.bhushantechsolutions.ninja

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.bhushantechsolutions.ninja.data.mock.MockBleManager
import com.bhushantechsolutions.ninja.ui.ConnectivityScreen
import com.bhushantechsolutions.ninja.ui.ConnectivityViewModel
import com.bhushantechsolutions.ninja.ui.PlatformSystemBarTheme

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF003547),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFC2E8FF),
    tertiary = Color(0xFFB39DDB),
    onTertiary = Color(0xFF32105C),
    tertiaryContainer = Color(0xFF4A2875),
    onTertiaryContainer = Color(0xFFEDDCFF),
    background = Color(0xFF101318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF1A1C22),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF262A32),
    onSurfaceVariant = Color(0xFFC4C6D0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF006688),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E8FF),
    onSecondaryContainer = Color(0xFF001E2B),
    tertiary = Color(0xFF6B4EA2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF25005A),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

@Composable
@Preview
fun App(viewModel: ConnectivityViewModel? = null) {
    val activeViewModel = viewModel ?: remember {
        ConnectivityViewModel(bleManager = MockBleManager())
    }

    val isDark = isSystemInDarkTheme()
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    PlatformSystemBarTheme(isDark = isDark)

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            ConnectivityScreen(viewModel = activeViewModel)
        }
    }
}
