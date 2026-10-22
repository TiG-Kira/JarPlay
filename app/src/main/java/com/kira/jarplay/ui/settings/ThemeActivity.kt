package com.kira.jarplay.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.ui.glass.rememberGlassPageBackdrop
import com.kira.jarplay.ui.glass.standaloneContentPadding
import com.kira.jarplay.ui.theme.JarPlayTheme
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
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

private const val PREF_THEME = "pref_theme"

@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }
    var selectedTheme by remember {
        mutableStateOf(preferences.getString(PREF_THEME, "system") ?: "system")
    }
    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    fun select(value: String) {
        selectedTheme = value
        preferences.edit().putString(PREF_THEME, value).apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopAppBar(
                    title = stringResource(R.string.pref_theme_title),
                    backdrop = glassPage.backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        GlassIconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixGlassIcons.ChevronBackward,
                                contentDescription = stringResource(R.string.close),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .then(glassPage.contentModifier)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = standaloneContentPadding(paddingValues, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    SmallTitle(text = stringResource(R.string.pref_theme_category))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors()
                    ) {
                        ThemeOption(
                            title = stringResource(R.string.pref_theme_system),
                            summary = stringResource(R.string.pref_theme_system_summary),
                            selected = selectedTheme == "system",
                            onClick = { select("system") }
                        )
                        ThemeOption(
                            title = stringResource(R.string.pref_theme_light),
                            summary = stringResource(R.string.pref_theme_light_summary),
                            selected = selectedTheme == "light",
                            onClick = { select("light") }
                        )
                        ThemeOption(
                            title = stringResource(R.string.pref_theme_night),
                            summary = stringResource(R.string.pref_theme_night_summary),
                            selected = selectedTheme == "night",
                            onClick = { select("night") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        },
        onClick = onClick
    )
}
