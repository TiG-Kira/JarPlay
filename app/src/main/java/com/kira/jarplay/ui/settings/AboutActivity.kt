package com.kira.jarplay.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import coil.compose.AsyncImage
import com.kira.jarplay.BuildConfig
import com.kira.jarplay.R
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.utils.ApkDownloader
import com.kira.jarplay.utils.UpdateChecker
import com.kira.jarplay.utils.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kira.jarplay.ui.glass.rememberGlassPageBackdrop
import com.kira.jarplay.ui.glass.standaloneContentPadding

private const val PREF_UPDATE = "update_preferences"
private const val KEY_ENABLE_BETA = "enable_beta_update"

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarPlayTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // 本页在主界面取景层之外，自建一层供玻璃顶栏折射页面内容
    val glassPage = rememberGlassPageBackdrop()
    // 显式 canScroll = { true }：miuix 0.9.4 的 MiuixScrollBehavior 默认值不会真正跟踪滚动，
    // 不指定会导致玻璃顶栏的 material 在滚动后不浮现（折叠失效）。
    val canScrollAlways: () -> Boolean = remember { { true } }
    val scrollBehavior = MiuixScrollBehavior(canScroll = canScrollAlways)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val systemNavBarsHeight = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }

    val updatePrefs = remember { context.getSharedPreferences(PREF_UPDATE, android.content.Context.MODE_PRIVATE) }

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloadingApk by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }
    var pendingInstallVersion by remember { mutableStateOf<String?>(null) }
    var releaseStatus by remember { mutableStateOf<UpdateChecker.ReleaseStatus?>(null) }
    var betaUpdateEnabled by remember { mutableStateOf(updatePrefs.getBoolean(KEY_ENABLE_BETA, false)) }
    var showBetaWarningDialog by remember { mutableStateOf(false) }

    val currentVersion = remember { BuildConfig.VERSION_NAME }
    val darkTheme = isSystemInDarkTheme()

    // Termux Ultra 招牌呼吸渐变背景（纯 Compose Brush，全 API 可用；无需 RuntimeShader/skSL 资源）
    val infiniteTransition = rememberInfiniteTransition(label = "breathingGradient")
    val gradientFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientBreathing"
    )
    val lightGradient = Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFFFFE5EF), Color(0xFFF9DBE5), gradientFraction),
            lerp(Color(0xFFF7BAD1), Color(0xFFBCC1FF), gradientFraction),
            lerp(Color(0xFFA3A5F9), Color(0xFF8EAFFF), gradientFraction)
        )
    )
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFF330FE0), Color(0xFF934CBC), gradientFraction),
            lerp(Color(0xFF4C238C), Color(0xFF9E35AA), gradientFraction),
            lerp(Color(0xFF1C28D3), Color(0xFF0033C6), gradientFraction)
        )
    )

    // 滚动状态跟踪：头部随滚动淡出
    val listState = rememberLazyListState()
    val headerHeightPx = with(density) { 100.dp.toPx() }
    var scrollFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.offset ?: 0 }
            .collect { offset ->
                val firstIndex = listState.firstVisibleItemIndex
                scrollFraction = if (firstIndex == 0) {
                    (-offset.toFloat() / headerHeightPx).coerceIn(0f, 1f)
                } else {
                    1f
                }
            }
    }
    val headerAlphaAnim by animateFloatAsState(
        targetValue = 1f * (1f - scrollFraction),
        label = "headerAlpha"
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        scope.launch {
            val status = UpdateChecker.getReleaseStatus(currentVersion)
            if (status != null) {
                releaseStatus = status
            }
        }
    }

    val navigationEventDispatcher = remember { NavigationEventDispatcher() }
    val navigationEventDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = navigationEventDispatcher
        }
    }

    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        ) {
            // 呼吸渐变背景层（贴在内容之下）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - scrollFraction) * (if (darkTheme) 0.5f else 1f) }
                    .background(if (darkTheme) darkGradient else lightGradient)
                    .zIndex(-1f)
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    GlassTopAppBar(
                        title = stringResource(R.string.action_about),
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
                    state = listState,
                    modifier = Modifier
                        .then(glassPage.contentModifier)
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = standaloneContentPadding(
                        paddingValues,
                        bottom = systemNavBarsHeight + 26.dp
                    ),
                    verticalArrangement = Arrangement.Top
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = 1f - scrollFraction }
                                .padding(top = 60.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 头部前景色随主题；logo 白色像素透出渐变背景，其余染成同色
                            val headerFg = if (darkTheme) Color.White else Color(0xFF333333)
                            val fgInt = android.graphics.Color.argb(
                                (headerFg.alpha * 255).toInt(),
                                (headerFg.red * 255).toInt(),
                                (headerFg.green * 255).toInt(),
                                (headerFg.blue * 255).toInt()
                            )
                            val appIcon = remember(darkTheme) {
                                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap()?.let { bm ->
                                    val copy = bm.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                    val pixels = IntArray(copy.width * copy.height)
                                    copy.getPixels(pixels, 0, copy.width, 0, 0, copy.width, copy.height)
                                    for (i in pixels.indices) {
                                        val c = pixels[i]
                                        val r = (c shr 16) and 0xFF
                                        val g = (c shr 8) and 0xFF
                                        val b = c and 0xFF
                                        val a = (c shr 24) and 0xFF
                                        val brightness = (r + g + b) / 3f
                                        if (a > 128 && brightness > 180f) {
                                            // 白色像素 → 透明，让渐变背景透出
                                            pixels[i] = 0x00000000
                                        } else if (a > 128) {
                                            // 其余非透明像素 → 染成与文字同色
                                            pixels[i] = fgInt
                                        }
                                    }
                                    copy.setPixels(pixels, 0, copy.width, 0, 0, copy.width, copy.height)
                                    BitmapPainter(copy.asImageBitmap())
                                }
                            }
                            if (appIcon != null) {
                                Image(
                                    painter = appIcon,
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(100.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.Add,
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(60.dp),
                                    tint = headerFg
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.app_name_full),
                                style = TextStyle(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = headerFg
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentVersion,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color = headerFg
                                    )
                                )
                                if (releaseStatus == UpdateChecker.ReleaseStatus.PRERELEASE) {
                                    BetaTag()
                                } else if (releaseStatus == UpdateChecker.ReleaseStatus.NOT_FOUND) {
                                    InternalBuildTag()
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoRow(
                                    title = stringResource(R.string.device_model),
                                    value = android.os.Build.MODEL
                                )
                                InfoRow(
                                    title = stringResource(R.string.android_version),
                                    value = android.os.Build.VERSION.RELEASE
                                )
                                InfoRow(
                                    title = stringResource(R.string.kernel_version),
                                    value = android.os.Build.DISPLAY
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            ArrowPreference(
                                title = stringResource(R.string.developer_name),
                                summary = "@TiG-Kira",
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/TiG-Kira/JarPlay")
                                    )
                                    context.startActivity(intent)
                                },
                                startAction = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = "https://github.com/TiG-Kira.png",
                                            contentDescription = "Developer Avatar",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        val updateSummary = when {
                            checkingUpdate -> stringResource(R.string.checking_updates)
                            updateResult is UpdateResult.UpdateAvailable -> {
                                val available = updateResult as UpdateResult.UpdateAvailable
                                if (available.isBeta) stringResource(R.string.beta_version_available)
                                else stringResource(R.string.new_version_available)
                            }
                            updateResult is UpdateResult.UpToDate -> stringResource(R.string.up_to_date)
                            else -> null
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            ArrowPreference(
                                title = stringResource(R.string.check_updates),
                                // 默认副标题应是本项自己的描述；此前误用 about_message（应用简介），
                                // 导致“检查更新”一栏显示成整段应用介绍。
                                summary = updateSummary
                                    ?: stringResource(R.string.about_check_updates_desc),
                                onClick = {
                                    if (!checkingUpdate) {
                                        checkingUpdate = true
                                        scope.launch {
                                            updateResult = UpdateChecker.checkForUpdates(currentVersion, betaUpdateEnabled)
                                            showUpdateDialog = true
                                            checkingUpdate = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            SwitchPreference(
                                title = stringResource(R.string.enable_beta_update),
                                summary = stringResource(R.string.enable_beta_update_desc),
                                checked = betaUpdateEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        betaUpdateEnabled = true
                                        showBetaWarningDialog = true
                                    } else {
                                        betaUpdateEnabled = false
                                        updatePrefs.edit().putBoolean(KEY_ENABLE_BETA, false).apply()
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name_full),
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                )
                                Text(
                                    text = currentVersion,
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.about_message),
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.license_info),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showUpdateDialog && updateResult != null) {
            if (pendingInstallVersion != null && ApkDownloader.hasInstallPermission(context)) {
                val apkFile = ApkDownloader.getDownloadedApkFile(context, pendingInstallVersion!!)
                if (apkFile.exists()) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        ApkDownloader.installApk(context, apkFile)
                        pendingInstallVersion = null
                        showUpdateDialog = false
                    }
                } else {
                    pendingInstallVersion = null
                }
            }

            val result = updateResult!!
            OverlayDialog(
                show = showUpdateDialog,
                title = when (result) {
                    is UpdateResult.UpdateAvailable -> if (result.isBeta) stringResource(R.string.beta_version_available) else stringResource(R.string.new_version_found)
                    is UpdateResult.UpToDate -> stringResource(R.string.up_to_date)
                    is UpdateResult.CheckFailed -> stringResource(R.string.check_failed)
                },
                summary = when (result) {
                    is UpdateResult.UpdateAvailable -> {
                        val preReleaseTag = if (result.isBeta) " (Beta) " else ""
                        "${stringResource(R.string.current_version)}: ${result.currentVersionName}\n${stringResource(R.string.latest_version)}: ${result.latestVersionName}${preReleaseTag}"
                    }
                    is UpdateResult.UpToDate -> {
                        "${stringResource(R.string.is_latest)} ${result.currentVersionName}"
                    }
                    is UpdateResult.CheckFailed -> {
                        stringResource(R.string.check_failed)
                    }
                },
                onDismissRequest = { showUpdateDialog = false },
                content = {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (result is UpdateResult.UpdateAvailable && result.releaseNotes.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.update_log),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        MarkdownText(
                            text = result.releaseNotes,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .heightIn(max = 200.dp)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        )
                    }
                    if (downloadingApk) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.downloading),
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                )
                                Text(
                                    text = "$downloadProgress%",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(downloadProgress / 100f)
                                        .height(6.dp)
                                        .background(MiuixTheme.colorScheme.primary)
                                )
                            }
                            if (totalBytes > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatUpdateFileSize(downloadedBytes)} / ${formatUpdateFileSize(totalBytes)}",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (result is UpdateResult.UpdateAvailable) Arrangement.spacedBy(8.dp) else Arrangement.Center
                    ) {
                        if (result is UpdateResult.UpdateAvailable) {
                            TextButton(
                                text = stringResource(R.string.later),
                                onClick = { showUpdateDialog = false },
                                modifier = Modifier.weight(1f),
                                enabled = !downloadingApk
                            )
                            TextButton(
                                text = stringResource(R.string.manual),
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(result.releaseUrl)
                                    )
                                    context.startActivity(intent)
                                    showUpdateDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !downloadingApk
                            )
                            Button(
                                onClick = {
                                    downloadingApk = true
                                    downloadProgress = 0
                                    downloadedBytes = 0L
                                    totalBytes = 0L
                                    val downloadUrl = ApkDownloader.constructDownloadUrl(result.latestVersionName, context)
                                    scope.launch(Dispatchers.IO) {
                                        val downloadResult = ApkDownloader.downloadAndInstall(
                                            context,
                                            downloadUrl,
                                            result.latestVersionName
                                        ) { progress, downloaded, total ->
                                            downloadProgress = progress
                                            downloadedBytes = downloaded
                                            totalBytes = total
                                        }
                                        downloadingApk = false
                                        if (downloadResult.isFailure) {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(result.releaseUrl)
                                            )
                                            context.startActivity(intent)
                                        } else {
                                            if (!ApkDownloader.hasInstallPermission(context)) {
                                                pendingInstallVersion = result.latestVersionName
                                            }
                                        }
                                        if (ApkDownloader.hasInstallPermission(context)) {
                                            showUpdateDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    color = MiuixTheme.colorScheme.primary
                                ),
                                enabled = !downloadingApk
                            ) {
                                Text(text = stringResource(R.string.download), fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { showUpdateDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    color = MiuixTheme.colorScheme.primary
                                )
                            ) {
                                Text(text = stringResource(R.string.ok), fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                }
            )
        }

        OverlayDialog(
            show = showBetaWarningDialog,
            title = stringResource(R.string.beta_warning_title),
            summary = stringResource(R.string.beta_warning_message),
            onDismissRequest = {
                betaUpdateEnabled = false
                showBetaWarningDialog = false
            },
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.beta_warning_cancel),
                        onClick = {
                            betaUpdateEnabled = false
                            showBetaWarningDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            updatePrefs.edit().putBoolean(KEY_ENABLE_BETA, true).apply()
                            showBetaWarningDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.beta_warning_confirm),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun BetaTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.error.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Beta",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.error
            )
        )
    }
}

@Composable
fun InternalBuildTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Internal",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
        )
    }
}

@Composable
fun InfoRow(title: String, value: String) {
    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )
        )
        Text(
            text = title,
            style = TextStyle(
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.lines()
    Column(modifier = modifier) {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        )
                        Text(
                            text = line.substring(2),
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = line,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    )
                }
            }
        }
    }
}

fun formatUpdateFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${(bytes / 1024.0).toInt()} KB"
        bytes < 1024L * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0)).let { String.format("%.1f", it) }} MB"
        else -> "${(bytes / (1024.0 * 1024.0 * 1024.0)).let { String.format("%.2f", it) }} GB"
    }
}
