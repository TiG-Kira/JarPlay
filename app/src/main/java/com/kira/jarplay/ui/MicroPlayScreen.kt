package com.kira.jarplay.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kira.jarplay.R
import com.kira.jarplay.ui.theme.JarPlayTheme
import javax.microedition.lcdui.Canvas
import javax.microedition.lcdui.keyboard.KeyMapper
import javax.microedition.util.ContextHolder
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private const val PREF_KEYBOARD_LAYOUT = "keyboard_layout"
private const val PREF_KEYBOARD_EDITED = "keyboard_edited"

private const val PLACEHOLDER_KEY_CODE = -999

/** 帧率限制：跟随应用配置。 */
const val FRAME_RATE_LIMIT_PROFILE = -1

/** 帧率限制：不限帧。 */
const val FRAME_RATE_LIMIT_NONE = 0

// TYPE_NUM_ARR 布局：数字键在左侧(3列)，方向键和功能键在右侧
// 参考 VirtualKeyboard.java 中的 TYPE_NUM_ARR 布局
private val DEFAULT_LAYOUT = listOf(
    // 行0: L | 1 | 2 | 3 |   | ←↑ | M | ↑
    listOf(
        KeyInfo("L", Canvas.KEY_SOFT_LEFT),
        KeyInfo("1", Canvas.KEY_NUM1),
        KeyInfo("2", Canvas.KEY_NUM2),
        KeyInfo("3", Canvas.KEY_NUM3),
        KeyInfo("", PLACEHOLDER_KEY_CODE), // 占位
        KeyInfo("\u2196", KeyMapper.SE_KEY_SPECIAL_GAMING_A),  // UP_LEFT
        KeyInfo("M", KeyMapper.KEY_OPTIONS_MENU),
        KeyInfo("\u2191", Canvas.KEY_UP)
    ),
    // 行1:   | 4 | 5 | 6 |   | ←  | F | →
    listOf(
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("4", Canvas.KEY_NUM4),
        KeyInfo("5", Canvas.KEY_NUM5),
        KeyInfo("6", Canvas.KEY_NUM6),
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("\u2190", Canvas.KEY_LEFT),
        KeyInfo("F", Canvas.KEY_FIRE),
        KeyInfo("\u2192", Canvas.KEY_RIGHT)
    ),
    // 行2:   | 7 | 8 | 9 |   | ↓← | ↓ | ↓→
    listOf(
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("7", Canvas.KEY_NUM7),
        KeyInfo("8", Canvas.KEY_NUM8),
        KeyInfo("9", Canvas.KEY_NUM9),
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("\u2199", KeyMapper.SE_KEY_SPECIAL_GAMING_B),  // DOWN_LEFT
        KeyInfo("\u2193", Canvas.KEY_DOWN),
        KeyInfo("\u2198", Canvas.KEY_END)  // DOWN_RIGHT
    ),
    // 行3: R | * | 0 | # |   |    |   |
    listOf(
        KeyInfo("R", Canvas.KEY_SOFT_RIGHT),
        KeyInfo("*", Canvas.KEY_STAR),
        KeyInfo("0", Canvas.KEY_NUM0),
        KeyInfo("#", Canvas.KEY_POUND),
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("", PLACEHOLDER_KEY_CODE),
        KeyInfo("", PLACEHOLDER_KEY_CODE)
    )
)

private data class KeyInfo(val label: String, val keyCode: Int)

private data class KeyInfoRow(val keys: List<KeyInfo>)

private fun defaultLayout(): List<KeyInfoRow> =
    DEFAULT_LAYOUT.map { row -> KeyInfoRow(row.map { it.copy() }) }

private fun loadLayoutFromPrefs(context: Context): List<KeyInfoRow>? {
    val prefs = context.getSharedPreferences(PREF_KEYBOARD_LAYOUT, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(PREF_KEYBOARD_EDITED, false)) return null
    val rows = mutableListOf<KeyInfoRow>()
    for (rowIndex in DEFAULT_LAYOUT.indices) {
        val rowKeys = mutableListOf<KeyInfo>()
        var keyIndex = 0
        while (prefs.contains("row_${rowIndex}_key_${keyIndex}_code")) {
            val label = prefs.getString("row_${rowIndex}_key_${keyIndex}_label", "") ?: ""
            val code = prefs.getInt("row_${rowIndex}_key_${keyIndex}_code", PLACEHOLDER_KEY_CODE)
            rowKeys.add(KeyInfo(label, code))
            keyIndex++
        }
        if (rowKeys.isEmpty()) return null
        rows.add(KeyInfoRow(rowKeys))
    }
    return rows.ifEmpty { null }
}

private fun saveLayoutToPrefs(context: Context, layout: List<KeyInfoRow>) {
    val editor = context.getSharedPreferences(PREF_KEYBOARD_LAYOUT, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .putBoolean(PREF_KEYBOARD_EDITED, true)
    for (rowIndex in layout.indices) {
        val row = layout[rowIndex]
        editor.putInt("row_${rowIndex}_size", row.keys.size)
        for (keyIndex in row.keys.indices) {
            editor.putString("row_${rowIndex}_key_${keyIndex}_label", row.keys[keyIndex].label)
            editor.putInt("row_${rowIndex}_key_${keyIndex}_code", row.keys[keyIndex].keyCode)
        }
    }
    editor.apply()
}

private fun resetLayoutInPrefs(context: Context) {
    context.getSharedPreferences(PREF_KEYBOARD_LAYOUT, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

/**
 * 播放页跨 ComposeView 共享的界面状态。
 *
 * 工具栏与虚拟键盘挂载在两个独立的 [ComposeView] 上，无法共享 `remember` 状态，
 * 因此由 MicroActivity 持有这个对象并分别注入。
 */
class MicroPlayUiState {
    var editMode by mutableStateOf(false)
    var keyboardVisible by mutableStateOf(true)
    var frameRateLimit by mutableStateOf(FRAME_RATE_LIMIT_PROFILE)
    var showActionSheet by mutableStateOf(false)
    var showExitDialog by mutableStateOf(false)
}

/** 播放页控制层回调，由 Java 侧实现。 */
interface MicroPlayCallbacks {
    /** 结束当前 MIDlet 并关闭播放页。 */
    fun onClose()

    /** 打开该游戏的应用设置，并结束当前 MIDlet。 */
    fun onOpenAppSettings()

    fun onScreenshot()

    fun onToggleKeyboard()

    fun onSetFrameRateLimit(fps: Int)
}

object MicroPlayScreenHelper {

    @JvmStatic
    fun createState(): MicroPlayUiState = MicroPlayUiState()

    /**
     * 初始化播放页控制层。
     *
     * @param showTopBar true 显示顶部工具栏；false 仅显示一个浮动的菜单按钮（沉浸模式）
     */
    @JvmStatic
    fun setControlsContent(
        composeView: ComposeView,
        state: MicroPlayUiState,
        title: String,
        showTopBar: Boolean,
        callbacks: MicroPlayCallbacks
    ) {
        composeView.setContent {
            JarPlayTheme {
                MicroPlayControls(
                    state = state,
                    title = title,
                    showTopBar = showTopBar,
                    callbacks = callbacks
                )
            }
        }
    }

    @JvmStatic
    fun setVirtualKeyboardContent(composeView: ComposeView, state: MicroPlayUiState) {
        composeView.setContent {
            JarPlayTheme {
                MicroVirtualKeyboard(state = state)
            }
        }
    }
}

/** 播放页的一个操作项。菜单与操作面板共用同一份定义，避免两处文案/行为漂移。 */
private data class PlayAction(
    val label: String,
    val summary: String? = null,
    val selected: Boolean = false,
    val destructive: Boolean = false,
    val onTrigger: () -> Unit
)

@Composable
private fun MicroPlayControls(
    state: MicroPlayUiState,
    title: String,
    showTopBar: Boolean,
    callbacks: MicroPlayCallbacks
) {
    var showFrameRateDialog by remember { mutableStateOf(false) }

    val frameRateSummary = when {
        state.frameRateLimit == FRAME_RATE_LIMIT_PROFILE ->
            stringResource(R.string.play_frame_rate_follow)

        state.frameRateLimit <= FRAME_RATE_LIMIT_NONE ->
            stringResource(R.string.play_frame_rate_none)

        else -> stringResource(R.string.play_frame_rate_value, state.frameRateLimit)
    }

    val actions = buildList {
        add(
            PlayAction(
                label = stringResource(R.string.play_edit_layout),
                summary = stringResource(R.string.play_edit_layout_summary),
                selected = state.editMode,
                onTrigger = {
                    val next = !state.editMode
                    state.editMode = next
                    if (next) state.keyboardVisible = true
                }
            )
        )
        add(
            PlayAction(
                label = stringResource(R.string.play_keyboard),
                summary = if (state.keyboardVisible) {
                    stringResource(R.string.play_keyboard_shown)
                } else {
                    stringResource(R.string.play_keyboard_hidden)
                },
                selected = state.keyboardVisible,
                onTrigger = { callbacks.onToggleKeyboard() }
            )
        )
        add(
            PlayAction(
                label = stringResource(R.string.play_frame_rate),
                summary = frameRateSummary,
                onTrigger = { showFrameRateDialog = true }
            )
        )
        add(
            PlayAction(
                label = stringResource(R.string.take_screenshot),
                onTrigger = { callbacks.onScreenshot() }
            )
        )
        add(
            PlayAction(
                label = stringResource(R.string.action_settings),
                onTrigger = { callbacks.onOpenAppSettings() }
            )
        )
        add(
            PlayAction(
                label = stringResource(R.string.play_exit),
                destructive = true,
                onTrigger = { state.showExitDialog = true }
            )
        )
    }

    val menuEntry = remember(actions) {
        DropdownEntry(
            items = actions.map { action ->
                DropdownItem(
                    text = action.label,
                    summary = action.summary,
                    selected = action.selected,
                    onClick = action.onTrigger
                )
            }
        )
    }

    if (showTopBar) {
        // 顶部工具栏占据真实高度（不覆盖画面）；状态栏可见时下移，避免被状态栏压住
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { callbacks.onClose() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.play_exit)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { callbacks.onToggleKeyboard() }) {
                        Image(
                            painter = painterResource(R.drawable.ic_action_keyboard),
                            contentDescription = stringResource(R.string.play_toggle_keyboard),
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    WindowIconDropdownMenu(
                        entry = menuEntry,
                        backgroundColor = Color.Transparent,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                }
            )
        }
    } else {
        // 沉浸模式：只保留一个浮动的菜单按钮，尽量不遮挡画面
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.End)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            WindowIconDropdownMenu(
                entry = menuEntry,
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }
    }

    // 菜单键 / 返回键唤起的操作面板
    WindowDialog(
        show = state.showActionSheet,
        title = title,
        onDismissRequest = { state.showActionSheet = false },
        content = {
            Column {
                actions.forEach { action ->
                    BasicComponent(
                        title = action.label,
                        summary = action.summary,
                        onClick = {
                            state.showActionSheet = false
                            action.onTrigger()
                        }
                    )
                }
            }
        }
    )

    // 退出二次确认
    WindowDialog(
        show = state.showExitDialog,
        title = stringResource(R.string.CONFIRMATION_REQUIRED),
        summary = stringResource(R.string.FORCE_CLOSE_CONFIRMATION),
        onDismissRequest = { state.showExitDialog = false },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.cancel_action),
                    onClick = { state.showExitDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        state.showExitDialog = false
                        callbacks.onClose()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
                        contentColor = MiuixTheme.colorScheme.onError,
                        disabledContentColor = MiuixTheme.colorScheme.disabledOnPrimaryButton
                    )
                ) {
                    Text(
                        text = stringResource(R.string.play_exit_confirm),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )

    if (showFrameRateDialog) {
        FrameRateLimitDialog(
            currentLimit = state.frameRateLimit,
            onDismissRequest = { showFrameRateDialog = false },
            onConfirm = { fps ->
                state.frameRateLimit = fps
                callbacks.onSetFrameRateLimit(fps)
                showFrameRateDialog = false
            }
        )
    }
}

@Composable
private fun FrameRateLimitDialog(
    currentLimit: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember {
        mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "")
    }

    WindowDialog(
        show = true,
        title = stringResource(R.string.play_frame_rate),
        summary = stringResource(R.string.play_frame_rate_summary),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    label = stringResource(R.string.play_frame_rate_unit),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.reset),
                        onClick = { onConfirm(FRAME_RATE_LIMIT_PROFILE) },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val parsed = text.toIntOrNull() ?: FRAME_RATE_LIMIT_NONE
                            onConfirm(parsed.coerceIn(0, 999))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonColors(
                            color = MiuixTheme.colorScheme.primary,
                            disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
                            contentColor = MiuixTheme.colorScheme.onPrimary,
                            disabledContentColor = MiuixTheme.colorScheme.disabledOnPrimaryButton
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun MicroVirtualKeyboard(state: MicroPlayUiState) {
    if (!state.keyboardVisible) {
        return
    }

    val context = LocalContext.current
    val editMode = state.editMode
    val keyLayout = remember { mutableStateListOf<KeyInfoRow>() }
    var selectedKey by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    if (keyLayout.isEmpty()) {
        keyLayout.addAll(loadLayoutFromPrefs(context) ?: defaultLayout())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (editMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.layout_edit_mode),
                        style = MiuixTheme.textStyles.main,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.play_layout_hint),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                TextButton(
                    text = stringResource(R.string.RESET_LAYOUT_CMD),
                    onClick = {
                        resetLayoutInPrefs(context)
                        keyLayout.clear()
                        keyLayout.addAll(defaultLayout())
                        selectedKey = null
                        state.editMode = false
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        saveLayoutToPrefs(context, keyLayout.toList())
                        selectedKey = null
                        state.editMode = false
                    },
                    colors = ButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
                        contentColor = MiuixTheme.colorScheme.onPrimary,
                        disabledContentColor = MiuixTheme.colorScheme.disabledOnPrimaryButton
                    )
                ) {
                    Text(
                        text = stringResource(R.string.layout_edit_finish),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        keyLayout.forEachIndexed { rowIndex, rowData ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowData.keys.forEachIndexed { keyIndex, keyInfo ->
                    if (keyInfo.keyCode == PLACEHOLDER_KEY_CODE) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } else {
                        val isSelected = editMode && selectedKey == (rowIndex to keyIndex)
                        VirtualKey(
                            label = keyInfo.label,
                            keyCode = keyInfo.keyCode,
                            editMode = editMode,
                            selected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = selectedKey
                                when (current) {
                                    null -> selectedKey = rowIndex to keyIndex
                                    rowIndex to keyIndex -> selectedKey = null
                                    else -> {
                                        val (fromRow, fromKey) = current
                                        val from = keyLayout[fromRow].keys[fromKey]
                                        val to = keyLayout[rowIndex].keys[keyIndex]
                                        keyLayout[fromRow] = KeyInfoRow(
                                            keyLayout[fromRow].keys.toMutableList()
                                                .also { it[fromKey] = to }
                                        )
                                        keyLayout[rowIndex] = KeyInfoRow(
                                            keyLayout[rowIndex].keys.toMutableList()
                                                .also { it[keyIndex] = from }
                                        )
                                        selectedKey = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualKey(
    label: String,
    keyCode: Int,
    editMode: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val handler = remember(keyCode) { Handler(Looper.getMainLooper()) }
    var isPressed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val repeatRunnable = remember(keyCode) {
        object : Runnable {
            override fun run() {
                if (isPressed) {
                    postKey(CanvasKeyEvent.DOWN, keyCode)
                    handler.postDelayed(this, 50L)
                }
            }
        }
    }

    DisposableEffect(keyCode) {
        onDispose {
            handler.removeCallbacksAndMessages(null)
            isPressed = false
        }
    }

    val colorScheme = MiuixTheme.colorScheme
    val containerColor = when {
        selected || isPressed -> colorScheme.primary
        editMode -> colorScheme.secondaryContainer
        else -> colorScheme.secondaryVariant
    }
    val contentColor = when {
        selected || isPressed -> colorScheme.onPrimary
        editMode -> colorScheme.onSecondaryContainer
        else -> colorScheme.onSecondaryVariant
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (editMode) {
                    Modifier.border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) colorScheme.primary else colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        if (editMode) {
                            onClick()
                        } else {
                            isPressed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            postKey(CanvasKeyEvent.DOWN, keyCode)
                            handler.postDelayed(repeatRunnable, 400L)
                        }
                        true
                    }

                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        if (!editMode) {
                            isPressed = false
                            handler.removeCallbacks(repeatRunnable)
                            postKey(CanvasKeyEvent.UP, keyCode)
                        }
                        true
                    }

                    else -> false
                }
            }
    ) {
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            cornerRadius = 12.dp,
            colors = ButtonColors(
                color = containerColor,
                disabledColor = colorScheme.disabledSecondaryVariant,
                contentColor = contentColor,
                disabledContentColor = colorScheme.disabledOnSecondaryVariant
            )
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

private enum class CanvasKeyEvent { DOWN, UP }

private fun postKey(event: CanvasKeyEvent, keyCode: Int) {
    val displayable = ContextHolder.getActivity()?.getCurrent() as? Canvas ?: return
    when (event) {
        CanvasKeyEvent.DOWN -> displayable.postKeyPressed(keyCode)
        CanvasKeyEvent.UP -> displayable.postKeyReleased(keyCode)
    }
}
