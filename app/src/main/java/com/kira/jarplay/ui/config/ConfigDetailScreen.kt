package com.kira.jarplay.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.config.ProfileModel
import com.kira.jarplay.config.ProfilesManager
import com.kira.jarplay.ui.glass.rememberGlassPageBackdrop
import com.kira.jarplay.ui.glass.standaloneContentPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import yuku.ambilwarna.AmbilWarnaDialog

private val HorizontalPadding = PaddingValues(horizontal = 12.dp)

@Composable
fun ConfigDetailScreen(
    profileDirPath: String,
    midletName: String?,
    isProfile: Boolean,
    workDir: String?,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()

    val screenOrientationEntries = stringArrayResource(R.array.PREF_ORIENTATION_ENTRIES)
    val screenGravityEntries = stringArrayResource(R.array.pref_screen_gravity_entries)
    val scaleTypeEntries = stringArrayResource(R.array.pref_scale_type_entries)
    val graphicsModeEntries = stringArrayResource(R.array.pref_graphics_mode_entries)
    val layoutEntries = stringArrayResource(R.array.PREF_LAYOUT_ENTRIES)
    val buttonShapeEntries = stringArrayResource(R.array.pref_button_shape_entries)

    // 关键：加载完成前保持 null，避免把一份空配置写回磁盘（历史 bug 的根源）
    var params by remember { mutableStateOf<ProfileModel?>(null) }

    LaunchedEffect(profileDirPath) {
        val dir = java.io.File(profileDirPath)
        params = withContext(Dispatchers.IO) {
            (ProfilesManager.loadConfig(dir) ?: ProfileModel(dir)).also { it.normalize() }
        }
    }

    DisposableEffect(profileDirPath) {
        onDispose {
            params?.let { ProfilesManager.saveConfig(it) }
        }
    }

    val glassPage = rememberGlassPageBackdrop()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            GlassTopAppBar(
                title = if (isProfile) profileDirPath.substringAfterLast("/") else (midletName ?: ""),
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
        },
        floatingActionButton = {
            if (!isProfile) {
                FloatingActionButton(
                    onClick = {
                        params?.let { ProfilesManager.saveConfig(it) }
                        onStart()
                    }
                ) {
                    Icon(
                        imageVector = MiuixIcons.Play,
                        contentDescription = stringResource(R.string.START_CMD)
                    )
                }
            }
        }
    ) { paddingValues ->
        val current = params
        if (current == null) {
            Box(
                modifier = Modifier
                    .then(glassPage.contentModifier)
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.config_loading),
                    style = MiuixTheme.textStyles.main,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .then(glassPage.contentModifier)
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = standaloneContentPadding(paddingValues, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SmallTitle(text = stringResource(R.string.PREF_SCREEN_OPTIONS))
                SettingCard {
                    ScreenSizeRow(
                        width = current.screenWidth,
                        height = current.screenHeight,
                        onWidthChange = { params = current.editCopy { screenWidth = it } },
                        onHeightChange = { params = current.editCopy { screenHeight = it } },
                        onSwap = {
                            params = current.editCopy {
                                screenWidth = current.screenHeight
                                screenHeight = current.screenWidth
                            }
                        }
                    )

                    TextFieldRow(
                        label = stringResource(R.string.PREF_SCALE_RATIO),
                        value = current.screenScaleRatio.toString(),
                        onValueChange = { v ->
                            params = current.editCopy { screenScaleRatio = v.toIntOrNull() ?: 100 }
                        }
                    )

                    OverlayDropdownPreference(
                        items = scaleTypeEntries.toList(),
                        selectedIndex = current.screenScaleType.coerceIn(0, scaleTypeEntries.lastIndex),
                        title = stringResource(R.string.pref_screen_scale_type),
                        summary = stringResource(R.string.pref_screen_scale_type_summary),
                        onSelectedIndexChange = { params = current.editCopy { screenScaleType = it } }
                    )

                    OverlayDropdownPreference(
                        items = screenGravityEntries.toList(),
                        selectedIndex = current.screenGravity.coerceIn(0, screenGravityEntries.lastIndex),
                        title = stringResource(R.string.pref_screen_gravity),
                        summary = stringResource(R.string.pref_screen_gravity_summary),
                        enabled = current.screenScaleType != ProfileModel.SCALE_TYPE_FILL,
                        onSelectedIndexChange = { params = current.editCopy { screenGravity = it } }
                    )

                    ColorPickerRow(
                        label = stringResource(R.string.PREF_BACKGROUND),
                        color = current.screenBackgroundColor,
                        onColorChanged = { c -> params = current.editCopy { screenBackgroundColor = c } }
                    )

                    OverlayDropdownPreference(
                        items = screenOrientationEntries.toList(),
                        selectedIndex = current.orientation.coerceIn(0, screenOrientationEntries.lastIndex),
                        title = stringResource(R.string.PREF_ORIENTATION),
                        onSelectedIndexChange = { params = current.editCopy { orientation = it } }
                    )

                    OverlayDropdownPreference(
                        items = graphicsModeEntries.toList(),
                        selectedIndex = current.graphicsMode.coerceIn(0, graphicsModeEntries.lastIndex),
                        title = stringResource(R.string.pref_graphics_mode_title),
                        onSelectedIndexChange = { params = current.editCopy { graphicsMode = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FILTER),
                        checked = current.screenFilter,
                        onCheckedChange = { params = current.editCopy { screenFilter = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_IMMEDIATE),
                        checked = current.immediateMode,
                        onCheckedChange = { params = current.editCopy { immediateMode = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.parallel_screen_redrawing),
                        checked = current.parallelRedrawScreen,
                        onCheckedChange = { params = current.editCopy { parallelRedrawScreen = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FORCE_FULLSCREEN),
                        checked = current.forceFullscreen,
                        onCheckedChange = { params = current.editCopy { forceFullscreen = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_SHOW_FPS),
                        checked = current.showFps,
                        onCheckedChange = { params = current.editCopy { showFps = it } }
                    )

                    TextFieldRow(
                        label = stringResource(R.string.PREF_LIMIT_FPS),
                        value = if (current.fpsLimit > 0) current.fpsLimit.toString() else "",
                        onValueChange = { v ->
                            params = current.editCopy { fpsLimit = v.toIntOrNull() ?: 0 }
                        }
                    )
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.PREF_FONT_OPTIONS))
                SettingCard {
                    FontSizeRow(
                        small = current.fontSizeSmall,
                        medium = current.fontSizeMedium,
                        large = current.fontSizeLarge,
                        onSmallChange = { params = current.editCopy { fontSizeSmall = it } },
                        onMediumChange = { params = current.editCopy { fontSizeMedium = it } },
                        onLargeChange = { params = current.editCopy { fontSizeLarge = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FONT_SIZE_IN_SP),
                        checked = current.fontApplyDimensions,
                        onCheckedChange = { params = current.editCopy { fontApplyDimensions = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FONT_ANTI_ALIASING),
                        checked = current.fontAA,
                        onCheckedChange = { params = current.editCopy { fontAA = it } }
                    )
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.pref_input_devices_title))
                SettingCard {
                    SwitchPreference(
                        title = stringResource(R.string.PREF_TOUCH_INPUT),
                        checked = current.touchInput,
                        onCheckedChange = { params = current.editCopy { touchInput = it } }
                    )

                    OverlayDropdownPreference(
                        items = layoutEntries.toList(),
                        selectedIndex = current.keyCodesLayout.coerceIn(0, layoutEntries.lastIndex),
                        title = stringResource(R.string.PREF_LAYOUT),
                        onSelectedIndexChange = { params = current.editCopy { keyCodesLayout = it } }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_VIRTUAL_KEYBOARD_OPTIONS),
                        checked = current.showKeyboard,
                        onCheckedChange = { params = current.editCopy { showKeyboard = it } }
                    )

                    if (current.showKeyboard) {
                        OverlayDropdownPreference(
                            items = buttonShapeEntries.toList(),
                            selectedIndex = current.vkButtonShape.coerceIn(0, buttonShapeEntries.lastIndex),
                            title = stringResource(R.string.pref_button_shape_title),
                            onSelectedIndexChange = { params = current.editCopy { vkButtonShape = it } }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.PREF_VK_FEEDBACK),
                            checked = current.vkFeedback,
                            onCheckedChange = { params = current.editCopy { vkFeedback = it } }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.PREF_VK_FORCE_OPACITY),
                            checked = current.vkForceOpacity,
                            onCheckedChange = { params = current.editCopy { vkForceOpacity = it } }
                        )

                        SliderPreference(
                            value = current.vkAlpha.toFloat(),
                            onValueChange = { params = current.editCopy { vkAlpha = it.toInt().coerceIn(0, 255) } },
                            title = stringResource(R.string.PREF_VK_ALPHA),
                            valueText = current.vkAlpha.toString(),
                            valueRange = 0f..255f
                        )

                        TextFieldRow(
                            label = stringResource(R.string.PREF_VK_HIDE_DELAY),
                            value = if (current.vkHideDelay > 0) current.vkHideDelay.toString() else "",
                            onValueChange = { v ->
                                params = current.editCopy { vkHideDelay = v.toIntOrNull() ?: 0 }
                            }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_FORE),
                            color = current.vkFgColor,
                            onColorChanged = { c -> params = current.editCopy { vkFgColor = c } }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_BACK),
                            color = current.vkBgColor,
                            onColorChanged = { c -> params = current.editCopy { vkBgColor = c } }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_SEL_FORE),
                            color = current.vkFgColorSelected,
                            onColorChanged = { c -> params = current.editCopy { vkFgColorSelected = c } }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_SEL_BACK),
                            color = current.vkBgColorSelected,
                            onColorChanged = { c -> params = current.editCopy { vkBgColorSelected = c } }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_OUTLINE),
                            color = current.vkOutlineColor,
                            onColorChanged = { c -> params = current.editCopy { vkOutlineColor = c } }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.PREF_SYS_PROPS))
                SettingCard {
                    TextField(
                        value = current.systemProperties ?: "",
                        onValueChange = { v -> params = current.editCopy { systemProperties = v } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(BasicComponentDefaults.InsideMargin)
                            .heightIn(min = 160.dp)
                    )
                }
            }
        }
        }
    }
}

/** 统一的分组容器：SmallTitle 之后的 Card，间距与左右 12dp 边距统一在此处表达。 */
@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HorizontalPadding)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors()
    ) {
        content()
    }
}

@Composable
private fun TextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(BasicComponentDefaults.InsideMargin),
        label = label,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun CompactNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value.toString(),
        onValueChange = { v -> onValueChange(v.toIntOrNull() ?: 0) },
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = true,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ScreenSizeRow(
    width: Int,
    height: Int,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(BasicComponentDefaults.InsideMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactNumberField(
            label = stringResource(R.string.PREF_WIDTH),
            value = width,
            onValueChange = onWidthChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "×",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MiuixTheme.textStyles.main
        )
        CompactNumberField(
            label = stringResource(R.string.PREF_HEIGHT),
            value = height,
            onValueChange = onHeightChange,
            modifier = Modifier.weight(1f)
        )
        GlassIconButton(
            onClick = onSwap,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = MiuixIcons.VerticalSplit,
                contentDescription = stringResource(R.string.swap_resolution)
            )
        }
    }
}

@Composable
private fun FontSizeRow(
    small: Int,
    medium: Int,
    large: Int,
    onSmallChange: (Int) -> Unit,
    onMediumChange: (Int) -> Unit,
    onLargeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(BasicComponentDefaults.InsideMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactNumberField(
            label = stringResource(R.string.PREF_FONT_SMALL),
            value = small,
            onValueChange = onSmallChange,
            modifier = Modifier.weight(1f)
        )
        CompactNumberField(
            label = stringResource(R.string.PREF_FONT_MEDIUM),
            value = medium,
            onValueChange = onMediumChange,
            modifier = Modifier.weight(1f)
        )
        CompactNumberField(
            label = stringResource(R.string.PREF_FONT_LARGE),
            value = large,
            onValueChange = onLargeChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Int,
    onColorChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(BasicComponentDefaults.InsideMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.main,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            text = "#%06X".format(color and 0xFFFFFF),
            onClick = {
                AmbilWarnaDialog(
                    context,
                    color or 0xFF000000.toInt(),
                    object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: AmbilWarnaDialog?, c: Int) {
                            onColorChanged(c and 0xFFFFFF)
                        }

                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                    }
                ).show()
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Color(
                        red = ((color shr 16) and 0xFF) / 255f,
                        green = ((color shr 8) and 0xFF) / 255f,
                        blue = (color and 0xFF) / 255f
                    )
                )
        )
    }
}

/**
 * 复制一份配置并就地修改字段。
 *
 * Java 的 [ProfileModel.copy] 没有参数，无法使用 Kotlin 具名参数，
 * 因此这里提供一个 `apply` 风格的包装，让调用点保持可读。
 */
private inline fun ProfileModel.editCopy(block: ProfileModel.() -> Unit): ProfileModel =
    copy().apply(block)
