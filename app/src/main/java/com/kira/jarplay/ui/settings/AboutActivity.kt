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
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val scrollBehavior = MiuixScrollBehavior()
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
                    TopAppBar(
                        title = stringResource(R.string.action_about),
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
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
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = systemNavBarsHeight + 26.dp
                    ),
                    verticalArrangement = Arrangement.Top
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = headerAlphaAnim }
                                .padding(top = 60.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val appIcon = remember {
                                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                                    ?.toBitmap()
                                    ?.asImageBitmap()
                                    ?.let { BitmapPainter(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (appIcon != null) {
                                    Image(
                                        painter = appIcon,
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(64.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = MiuixIcons.Basic.ArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.app_name_full),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentVersion,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/TiG-Kira/JarPlay")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = "https://github.com/TiG-Kira.png",
                                            contentDescription = "Developer Avatar",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.developer_name),
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MiuixTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "@TiG-Kira",
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = MiuixIcons.Basic.ArrowRight,
                                    contentDescription = stringResource(R.string.arrow),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.size(24.dp)
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
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (!checkingUpdate) {
                                        checkingUpdate = true
                                        scope.launch {
                                            updateResult = UpdateChecker.checkForUpdates(currentVersion, betaUpdateEnabled)
                                            showUpdateDialog = true
                                            checkingUpdate = false
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (checkingUpdate) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.check_updates),
                                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        )
                                        val hasUpdate = updateResult is UpdateResult.UpdateAvailable
                                        if (hasUpdate) {
                                            val available = updateResult as UpdateResult.UpdateAvailable
                                            Text(
                                                text = if (available.isBeta)
                                                    stringResource(R.string.beta_version_available)
                                                else
                                                    stringResource(R.string.new_version_available),
                                                style = TextStyle(
                                                    fontSize = 13.sp,
                                                    color = MiuixTheme.colorScheme.error
                                                )
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = MiuixIcons.Basic.ArrowRight,
                                    contentDescription = stringResource(R.string.arrow),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.size(24.dp)
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
                    is UpdateResult.CheckFailed -> stringResource(R.string.up_to_date)
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
                        stringResource(R.string.up_to_date)
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
