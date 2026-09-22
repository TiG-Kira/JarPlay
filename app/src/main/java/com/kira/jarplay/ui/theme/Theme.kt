package com.kira.jarplay.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.preference.PreferenceManager
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 主题取值：0 = 跟随系统，1 = 浅色，2 = 深色。 */
const val COLOR_MODE_SYSTEM = 0
const val COLOR_MODE_LIGHT = 1
const val COLOR_MODE_DARK = 2

private const val PREF_THEME = "pref_theme"

val LocalColorMode = compositionLocalOf { COLOR_MODE_SYSTEM }

private fun colorModeFromKey(key: String?): Int = when (key) {
    "light" -> COLOR_MODE_LIGHT
    "night" -> COLOR_MODE_DARK
    else -> COLOR_MODE_SYSTEM
}

/**
 * 应用主题。
 *
 * 主题取值来自 `pref_theme` 偏好项，并且监听它的变化，这样
 * 「设置 → 主题」切换后不需要重启即可生效。
 */
@Composable
fun JarPlayTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    var colorMode by remember(prefs) {
        mutableStateOf(colorModeFromKey(prefs.getString(PREF_THEME, null)))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_THEME) {
                colorMode = colorModeFromKey(prefs.getString(PREF_THEME, null))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val controller = remember(colorMode) {
        when (colorMode) {
            COLOR_MODE_LIGHT -> ThemeController(ColorSchemeMode.Light)
            COLOR_MODE_DARK -> ThemeController(ColorSchemeMode.Dark)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }

    val isDark = isDarkTheme(colorMode)

    /*
     * Miuix 的弹窗（WindowDialog / WindowIconDropdownMenu / OverlayDialog 等）内部会注册
     * 返回手势处理器，需要宿主提供 LocalNavigationEventDispatcherOwner。
     * 本项目的宿主 Activity 版本不会提供它，因此这里显式创建一个“根”分发器
     * （parent 必须显式传 null，否则默认值会去读同一个 local 而直接抛异常）。
     */
    val navigationEventOwner = rememberNavigationEventDispatcherOwner(true, null)

    CompositionLocalProvider(
        LocalColorMode provides colorMode,
        LocalNavigationEventDispatcherOwner.provides(navigationEventOwner)
    ) {
        MiuixTheme(controller = controller) {
            SystemUiEffect(isDark = isDark)
            content()
        }
    }
}

@Composable
private fun isDarkTheme(colorMode: Int): Boolean = when (colorMode) {
    COLOR_MODE_DARK -> true
    COLOR_MODE_LIGHT -> false
    else -> {
        val configuration = LocalConfiguration.current
        configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun isInDarkTheme(): Boolean = isDarkTheme(LocalColorMode.current)

@Composable
@Suppress("DEPRECATION")
private fun SystemUiEffect(isDark: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val backgroundColor = MiuixTheme.colorScheme.background

    LaunchedEffect(isDark, view, context, backgroundColor) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window

        // 边到边显示：系统栏由页面自己消费 insets（Miuix 的 Scaffold / TopAppBar 已处理）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // API 35 起系统栏颜色由系统接管，旧版本仍需要手工设置
            val bgColorInt = backgroundColor.value.toInt()
            window.statusBarColor = bgColorInt
            window.navigationBarColor = bgColorInt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = 0
            }
        }
    }
}
