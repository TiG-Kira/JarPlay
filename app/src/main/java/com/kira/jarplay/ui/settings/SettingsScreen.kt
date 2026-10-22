package com.kira.jarplay.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.kira.jarplay.BuildConfig
import com.kira.jarplay.R
import com.kira.jarplay.config.ProfilesActivity
import com.kira.jarplay.util.Constants
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    scrollBehavior: ScrollBehavior? = null,
    topClearance: Dp = 0.dp,
    bottomClearance: Dp = 0.dp,
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    // 子页面（主题 / 屏幕设置等）也会改这些偏好项，这里监听变化以保持开关同步
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val toolbarEnabled = remember(preferences, revision) {
        preferences.getBoolean(Constants.PREF_TOOLBAR, false)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (scrollBehavior != null) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                }
            ),
        contentPadding = PaddingValues(top = 8.dp + topClearance, bottom = 24.dp + bottomClearance),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SmallTitle(text = stringResource(R.string.pref_category_appearance))
            SettingsCard {
                ArrowPreference(
                    title = stringResource(R.string.pref_theme_title),
                    summary = stringResource(R.string.pref_theme_summary),
                    onClick = {
                        context.startActivity(Intent(context, ThemeActivity::class.java))
                    },
                )
            }
        }

        item {
            SmallTitle(text = stringResource(R.string.pref_category_emulator))
            SettingsCard {
                SwitchPreference(
                    title = stringResource(R.string.pref_toolbar_title),
                    summary = stringResource(R.string.pref_toolbar_summary),
                    checked = toolbarEnabled,
                    onCheckedChange = {
                        preferences.edit().putBoolean(Constants.PREF_TOOLBAR, it).apply()
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.pref_screen_settings),
                    summary = stringResource(R.string.pref_screen_summary),
                    onClick = {
                        context.startActivity(Intent(context, ScreenSettingsActivity::class.java))
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.pref_audio_settings),
                    summary = stringResource(R.string.pref_audio_summary),
                    onClick = {
                        context.startActivity(Intent(context, AudioSettingsActivity::class.java))
                    },
                )
            }
        }

        item {
            SmallTitle(text = stringResource(R.string.pref_category_other))
            SettingsCard {
                ArrowPreference(
                    title = stringResource(R.string.action_profiles),
                    summary = stringResource(R.string.profiles_description),
                    onClick = {
                        context.startActivity(Intent(context, ProfilesActivity::class.java))
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.action_about),
                    summary = stringResource(R.string.about_summary),
                    onClick = {
                        context.startActivity(Intent(context, AboutActivity::class.java))
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.action_help),
                    summary = stringResource(R.string.help_summary),
                    onClick = {
                        context.startActivity(Intent(context, HelpActivity::class.java))
                    },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "JarPlay ${BuildConfig.VERSION_NAME}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        content()
    }
}
