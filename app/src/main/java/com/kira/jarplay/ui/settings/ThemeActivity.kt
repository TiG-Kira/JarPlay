package com.kira.jarplay.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.ui.theme.JarPlayTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

class ThemeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarPlayTheme {
                ThemeScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }
    var selectedTheme by remember {
        mutableStateOf(preferences.getString("pref_theme", "system") ?: "system")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_theme_title),
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
                SmallTitle(text = stringResource(R.string.pref_theme_category))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeOption(
                        title = stringResource(R.string.pref_theme_system),
                        summary = stringResource(R.string.pref_theme_system_summary),
                        selected = selectedTheme == "system",
                        onClick = {
                            selectedTheme = "system"
                            preferences.edit().putString("pref_theme", "system").apply()
                        }
                    )
                    ThemeOption(
                        title = stringResource(R.string.pref_theme_light),
                        summary = stringResource(R.string.pref_theme_light_summary),
                        selected = selectedTheme == "light",
                        onClick = {
                            selectedTheme = "light"
                            preferences.edit().putString("pref_theme", "light").apply()
                        }
                    )
                    ThemeOption(
                        title = stringResource(R.string.pref_theme_night),
                        summary = stringResource(R.string.pref_theme_night_summary),
                        selected = selectedTheme == "night",
                        onClick = {
                            selectedTheme = "night"
                            preferences.edit().putString("pref_theme", "night").apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.main,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
