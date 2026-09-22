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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Play
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (isProfile) profileDirPath.substringAfterLast("/") else (midletName ?: ""),
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
    ) { innerPadding ->
        val current = params
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SmallTitle(text = stringResource(R.string.PREF_SCREEN_OPTIONS))
                SettingCard {
                    ScreenSizeRow(
                        width = current.screenWidth,
                        height = current.screenHeight,
                        onWidthChange = { params = current.copy(screenWidth = it) },
                        onHeightChange = { params = current.copy(screenHeight = it) },
                        onSwap = {
                            params = current.copy(
                                screenWidth = current.screenHeight,
                                screenHeight = current.screenWidth
                            )
                        }
                    )

                    TextFieldRow(
                        label = stringResource(R.string.PREF_SCALE_RATIO),
                        value = current.screenScaleRatio.toString(),
                        onValueChange = { v ->
                            params = current.copy(screenScaleRatio = v.toIntOrNull() ?: 100)
                        }
                    )

                    OverlayDropdownPreference(
                        items = scaleTypeEntries.toList(),
                        selectedIndex = current.screenScaleType.coerceIn(0, scaleTypeEntries.lastIndex),
                        title = stringResource(R.string.pref_screen_scale_type),
                        summary = stringResource(R.string.pref_screen_scale_type_summary),
                        onSelectedIndexChange = { params = current.copy(screenScaleType = it) }
                    )

                    OverlayDropdownPreference(
                        items = screenGravityEntries.toList(),
                        selectedIndex = current.screenGravity.coerceIn(0, screenGravityEntries.lastIndex),
                        title = stringResource(R.string.pref_screen_gravity),
                        summary = stringResource(R.string.pref_screen_gravity_summary),
                        enabled = current.screenScaleType != ProfileModel.SCALE_TYPE_FILL,
                        onSelectedIndexChange = { params = current.copy(screenGravity = it) }
                    )

                    ColorPickerRow(
                        label = stringResource(R.string.PREF_BACKGROUND),
                        color = current.screenBackgroundColor,
                        onColorChanged = { c -> params = current.copy(screenBackgroundColor = c) }
                    )

                    OverlayDropdownPreference(
                        items = screenOrientationEntries.toList(),
                        selectedIndex = current.orientation.coerceIn(0, screenOrientationEntries.lastIndex),
                        title = stringResource(R.string.PREF_ORIENTATION),
                        onSelectedIndexChange = { params = current.copy(orientation = it) }
                    )

                    OverlayDropdownPreference(
                        items = graphicsModeEntries.toList(),
                        selectedIndex = current.graphicsMode.coerceIn(0, graphicsModeEntries.lastIndex),
                        title = stringResource(R.string.pref_graphics_mode_title),
                        onSelectedIndexChange = { params = current.copy(graphicsMode = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FILTER),
                        checked = current.screenFilter,
                        onCheckedChange = { params = current.copy(screenFilter = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_IMMEDIATE),
                        checked = current.immediateMode,
                        onCheckedChange = { params = current.copy(immediateMode = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.parallel_screen_redrawing),
                        checked = current.parallelRedrawScreen,
                        onCheckedChange = { params = current.copy(parallelRedrawScreen = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FORCE_FULLSCREEN),
                        checked = current.forceFullscreen,
                        onCheckedChange = { params = current.copy(forceFullscreen = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_SHOW_FPS),
                        checked = current.showFps,
                        onCheckedChange = { params = current.copy(showFps = it) }
                    )

                    TextFieldRow(
                        label = stringResource(R.string.PREF_LIMIT_FPS),
                        value = if (current.fpsLimit > 0) current.fpsLimit.toString() else "",
                        onValueChange = { v ->
                            params = current.copy(fpsLimit = v.toIntOrNull() ?: 0)
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
                        onSmallChange = { params = current.copy(fontSizeSmall = it) },
                        onMediumChange = { params = current.copy(fontSizeMedium = it) },
                        onLargeChange = { params = current.copy(fontSizeLarge = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FONT_SIZE_IN_SP),
                        checked = current.fontApplyDimensions,
                        onCheckedChange = { params = current.copy(fontApplyDimensions = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_FONT_ANTI_ALIASING),
                        checked = current.fontAA,
                        onCheckedChange = { params = current.copy(fontAA = it) }
                    )
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.pref_input_devices_title))
                SettingCard {
                    SwitchPreference(
                        title = stringResource(R.string.PREF_TOUCH_INPUT),
                        checked = current.touchInput,
                        onCheckedChange = { params = current.copy(touchInput = it) }
                    )

                    OverlayDropdownPreference(
                        items = layoutEntries.toList(),
                        selectedIndex = current.keyCodesLayout.coerceIn(0, layoutEntries.lastIndex),
                        title = stringResource(R.string.PREF_LAYOUT),
                        onSelectedIndexChange = { params = current.copy(keyCodesLayout = it) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.PREF_VIRTUAL_KEYBOARD_OPTIONS),
                        checked = current.showKeyboard,
                        onCheckedChange = { params = current.copy(showKeyboard = it) }
                    )

                    if (current.showKeyboard) {
                        OverlayDropdownPreference(
                            items = buttonShapeEntries.toList(),
                            selectedIndex = current.vkButtonShape.coerceIn(0, buttonShapeEntries.lastIndex),
                            title = stringResource(R.string.pref_button_shape_title),
                            onSelectedIndexChange = { params = current.copy(vkButtonShape = it) }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.PREF_VK_FEEDBACK),
                            checked = current.vkFeedback,
                            onCheckedChange = { params = current.copy(vkFeedback = it) }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.PREF_VK_FORCE_OPACITY),
                            checked = current.vkForceOpacity,
                            onCheckedChange = { params = current.copy(vkForceOpacity = it) }
                        )

                        SliderPreference(
                            value = current.vkAlpha.toFloat(),
                            onValueChange = { params = current.copy(vkAlpha = it.toInt().coerceIn(0, 255)) },
                            title = stringResource(R.string.PREF_VK_ALPHA),
                            valueText = current.vkAlpha.toString(),
                            valueRange = 0f..255f
                        )

                        TextFieldRow(
                            label = stringResource(R.string.PREF_VK_HIDE_DELAY),
                            value = if (current.vkHideDelay > 0) current.vkHideDelay.toString() else "",
                            onValueChange = { v ->
                                params = current.copy(vkHideDelay = v.toIntOrNull() ?: 0)
                            }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_FORE),
                            color = current.vkFgColor,
                            onColorChanged = { c -> params = current.copy(vkFgColor = c) }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_BACK),
                            color = current.vkBgColor,
                            onColorChanged = { c -> params = current.copy(vkBgColor = c) }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_SEL_FORE),
                            color = current.vkFgColorSelected,
                            onColorChanged = { c -> params = current.copy(vkFgColorSelected = c) }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_SEL_BACK),
                            color = current.vkBgColorSelected,
                            onColorChanged = { c -> params = current.copy(vkBgColorSelected = c) }
                        )

                        ColorPickerRow(
                            label = stringResource(R.string.PREF_VK_OUTLINE),
                            color = current.vkOutlineColor,
                            onColorChanged = { c -> params = current.copy(vkOutlineColor = c) }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = stringResource(R.string.PREF_SYS_PROPS))
                SettingCard {
                    TextField(
                        value = current.systemProperties ?: "",
                        onValueChange = { v -> params = current.copy(systemProperties = v) },
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

/** 统一的分组容器：SmallTitle 之后的 Card，间距与左右 12dp 边距统一在此处表达。 */
@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HorizontalPadding)
            .padding(bottom = 12.dp)
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
        IconButton(
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

/** 该页面未使用的占位，保持与旧实现一致的字体大小标签宽度。 */
@Composable
@Suppress("unused")
private fun LabelColumn(text: String) {
    Column(modifier = Modifier.height(0.dp)) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}
