package com.kira.jarplay.ui.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.ui.theme.JarPlayTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
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
    val preferences = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    var showFPS by remember {
        mutableStateOf(preferences.getBoolean("pref_show_fps", false))
    }
    var forceFullscreen by remember {
        mutableStateOf(preferences.getBoolean("pref_force_fullscreen", true))
    }
    var immediateProcessing by remember {
        mutableStateOf(preferences.getBoolean("pref_immediate", false))
    }
    var limitFPS by remember {
        mutableStateOf(preferences.getBoolean("pref_limit_fps", false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_screen_settings),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SmallTitle(text = stringResource(R.string.pref_category_display))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.PREF_SHOW_FPS),
                        summary = stringResource(R.string.PREF_SHOW_FPS_SUMMARY),
                        checked = showFPS,
                        onCheckedChange = {
                            showFPS = it
                            preferences.edit().putBoolean("pref_show_fps", it).apply()
                        }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.PREF_FORCE_FULLSCREEN),
                        summary = stringResource(R.string.PREF_FORCE_FULLSCREEN_SUMMARY),
                        checked = forceFullscreen,
                        onCheckedChange = {
                            forceFullscreen = it
                            preferences.edit().putBoolean("pref_force_fullscreen", it).apply()
                        }
                    )
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.pref_category_performance))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.PREF_IMMEDIATE),
                        summary = stringResource(R.string.PREF_IMMEDIATE_SUMMARY),
                        checked = immediateProcessing,
                        onCheckedChange = {
                            immediateProcessing = it
                            preferences.edit().putBoolean("pref_immediate", it).apply()
                        }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.PREF_LIMIT_FPS),
                        summary = stringResource(R.string.PREF_LIMIT_FPS_SUMMARY),
                        checked = limitFPS,
                        onCheckedChange = {
                            limitFPS = it
                            preferences.edit().putBoolean("pref_limit_fps", it).apply()
                        }
                    )
                }
            }
        }
    }
}
