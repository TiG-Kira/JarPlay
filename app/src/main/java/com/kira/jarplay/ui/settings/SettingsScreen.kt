package com.kira.jarplay.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.config.ProfilesActivity
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferences = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    var virtualKeyboardEnabled by remember {
        mutableStateOf(preferences.getBoolean("pref_virtual_keyboard", false))
    }
    var keepScreenOn by remember {
        mutableStateOf(preferences.getBoolean("pref_keep_screen_on", false))
    }
    var enableVibration by remember {
        mutableStateOf(preferences.getBoolean("pref_enable_vibration", true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SmallTitle(text = stringResource(R.string.pref_category_appearance))
            Card(
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .padding(bottom = 12.dp),
            ) {
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
            Card(
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .padding(bottom = 12.dp),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.pref_virtual_keyboard),
                    summary = stringResource(R.string.pref_virtual_keyboard_summary),
                    checked = virtualKeyboardEnabled,
                    onCheckedChange = {
                        virtualKeyboardEnabled = it
                        preferences.edit().putBoolean("pref_virtual_keyboard", it).apply()
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.pref_keep_screen_on),
                    summary = stringResource(R.string.pref_keep_screen_on_summary),
                    checked = keepScreenOn,
                    onCheckedChange = {
                        keepScreenOn = it
                        preferences.edit().putBoolean("pref_keep_screen_on", it).apply()
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.pref_vibration_title),
                    summary = stringResource(R.string.pref_audio_summary),
                    checked = enableVibration,
                    onCheckedChange = {
                        enableVibration = it
                        preferences.edit().putBoolean("pref_enable_vibration", it).apply()
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
            Card(
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .padding(bottom = 12.dp),
            ) {
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
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "JarPlay v1.0.0",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
