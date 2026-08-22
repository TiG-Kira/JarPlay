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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference

class AudioSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarPlayTheme {
                AudioSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun AudioSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    var vibrationEnabled by remember {
        mutableStateOf(preferences.getBoolean("pref_enable_vibration", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_audio_settings),
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SmallTitle(text = stringResource(R.string.pref_category_audio))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.pref_vibration_title),
                        summary = stringResource(R.string.pref_audio_summary),
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            preferences.edit().putBoolean("pref_enable_vibration", it).apply()
                        }
                    )
                }
            }
        }
    }
}
