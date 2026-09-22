package com.kira.jarplay.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.kira.jarplay.R
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.util.Constants
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference

class ScreenSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarPlayTheme {
                ScreenSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun ScreenSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val scrollBehavior = MiuixScrollBehavior()

    // 让页面上的开关始终与 SharedPreferences 保持一致
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_screen_settings),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SmallTitle(text = stringResource(R.string.pref_category_display))
                SettingsGroup {
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_TOOLBAR,
                        titleRes = R.string.pref_toolbar_title,
                        summaryRes = R.string.pref_toolbar_summary,
                    )
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_STATUSBAR,
                        titleRes = R.string.pref_enable_statusbar_title,
                        summaryRes = R.string.pref_enable_statusbar_summary,
                    )
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_ADD_CUTOUT_AREA,
                        titleRes = R.string.pref_add_cutout_area_title,
                        summaryRes = R.string.pref_add_cutout_area_summary,
                    )                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_BLACK_BACKGROUND,
                        titleRes = R.string.pref_black_background_title,
                        summaryRes = R.string.pref_black_background_summary,
                    )
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.pref_category_behavior))
                SettingsGroup {
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_KEEP_SCREEN,
                        titleRes = R.string.pref_wakelock_title,
                        summaryRes = R.string.pref_wakelock_summary,
                    )
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = Constants.PREF_SCREENSHOT_SWITCH,
                        titleRes = R.string.pref_screenshot_title,
                        summaryRes = R.string.pref_screenshot_summary,
                    )
                    DisplaySwitch(
                        preferences = preferences,
                        revision = revision,
                        key = MICRO3D_MESSAGE_KEY,
                        titleRes = R.string.pref_mascot_title,
                        summaryRes = R.string.pref_mascot_summary,
                    )
                }
            }
        }
    }
}

private const val MICRO3D_MESSAGE_KEY = "micro3d_using_message"

@Composable
private fun DisplaySwitch(
    preferences: SharedPreferences,
    revision: Int,
    key: String,
    titleRes: Int,
    summaryRes: Int,
    default: Boolean = false
) {
    val checked = remember(preferences, key, revision) {
        preferences.getBoolean(key, default)
    }
    SwitchPreference(
        title = stringResource(titleRes),
        summary = stringResource(summaryRes),
        checked = checked,
        onCheckedChange = { preferences.edit().putBoolean(key, it).apply() },
    )
}

@Composable
private fun SettingsGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        content()
    }
}
