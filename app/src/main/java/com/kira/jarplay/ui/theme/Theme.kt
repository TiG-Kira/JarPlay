package com.kira.jarplay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

val LocalColorMode = compositionLocalOf { 0 }

@Composable
fun JarPlayTheme(
    colorMode: Int = 0,
    content: @Composable () -> Unit,
) {
    val controller = remember(colorMode) {
        when (colorMode) {
            1 -> ThemeController(ColorSchemeMode.Light)
            2 -> ThemeController(ColorSchemeMode.Dark)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }
    CompositionLocalProvider(LocalColorMode provides colorMode) {
        MiuixTheme(
            controller = controller,
            content = content,
        )
    }
}

@Composable
fun isInDarkTheme(): Boolean = when (LocalColorMode.current) {
    2 -> true
    else -> androidx.compose.foundation.isSystemInDarkTheme()
}
