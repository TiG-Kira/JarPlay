package com.kira.jarplay.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppListModel
import com.kira.jarplay.installer.InstallerOverlay
import com.kira.jarplay.installer.InstallerState
import com.kira.jarplay.ui.apps.AppsScreen
import com.kira.jarplay.ui.glass.LocalGlassTopAppBarBackdrop
import com.kira.jarplay.ui.settings.SettingsScreen
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.util.FileUtils
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {

    private lateinit var installerState: InstallerState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appListModel = ViewModelProvider(this)[AppListModel::class.java]
        installerState = InstallerState(applicationContext, appListModel.appRepository)

        setContent {
            JarPlayTheme {
                MainContent(
                    installerState = installerState,
                    appListModel = appListModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        if (uri != null && ::installerState.isInitialized) {
            installerState.installFromUri(uri)
        }
    }
}

@Composable
fun MainContent(
    installerState: InstallerState,
    appListModel: AppListModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var selectedTab by remember { mutableIntStateOf(0) }

    val labels = listOf(
        stringResource(R.string.apps),
        stringResource(R.string.settings),
    )
    val icons = listOf(MiuixIcons.VerticalSplit, MiuixIcons.Settings)

    // 顶栏折叠是 miuix 的内置行为：大标题滚动后折叠成居中的小标题，GlassTopAppBar
    // 与普通 TopAppBar 一致。这里用默认构造即可，不要额外覆盖 snap / fling 动画参数，
    // 那些只影响折叠手感（曾把设置页本来正常的吸顶动画改坏），也不要传 canScroll
    // 之类的开关——设置页不传任何参数折叠就正常，问题只出在页面侧的滚动容器上。
    val scrollBehavior = MiuixScrollBehavior()

    // 隐藏手势提示线（小白条）后 navigationBars inset 上报为 0，底栏会贴死屏幕底边；
    // 保底 12dp 仅在 inset 不足时生效（同 KiTerminal-UX）。
    val systemNavBarsHeight = with(density) {
        maxOf(WindowInsets.navigationBars.getBottom(density), 12.dp.roundToPx()).toDp()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            installerState.installFromUri(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(context, R.string.permission_granted, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (FileUtils.isExternalStorageLegacy()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasWritePermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasWritePermission) {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    )
                }
            }
        }
    }

    // 玻璃顶栏与底栏共用的取景层：内容通过 miuixLayerBackdrop 折射给两根玻璃条。
    val glassBackdrop = rememberMiuixLayerBackdrop()

    // Miuix 组件（TopAppBar 返回手势 / OverlayDialog 等）需要 NavigationEventDispatcher。
    val navigationEventDispatcher = remember { NavigationEventDispatcher() }
    val navigationEventDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = navigationEventDispatcher
        }
    }

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
        LocalGlassTopAppBarBackdrop provides glassBackdrop,
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopAppBar(
                    title = labels[selectedTab],
                    scrollBehavior = scrollBehavior,
                    backdrop = glassBackdrop,
                    actions = {
                        if (selectedTab == 0) {
                            GlassIconButton(onClick = {
                                // JAR/JAD 的 MIME 类型并不统一，交给系统选择器自己过滤更可靠
                                filePickerLauncher.launch("*/*")
                            }) {
                                Icon(
                                    imageVector = MiuixIcons.Add,
                                    contentDescription = stringResource(R.string.install_app),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 内容层（带玻璃取景）：不做任何 padding——内容从顶栏与底栏下穿过，
                // 玻璃条才有东西可折射；首屏留白由页面自己的 contentPadding 提供。
                // 必须铺一层不透明的主题底色：玻璃条折射的是这个取景层，若内容透明，
                // 导航栏下方（无内容区域）会折射到窗口底色，呈现用户看到的“深色板块”。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.background)
                        .miuixLayerBackdrop(glassBackdrop)
                ) {
                    // 取景层必须自己铺一层不透明的主题底色，而且是作为「内容」铺：
                    // miuixLayerBackdrop 捕获的是本节点绘制的内容，节点自身 background
                    // 修饰符画的底色不在被捕获范围内。之前只在修饰符上铺底色，结果是
                    // 只有卡片挡住的位置玻璃才渲染得出来，一离开卡片（应用页空白区、
                    // 设置页卡片之间的空隙）底板玻璃就整块消失。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.background)
                    )

                    when (selectedTab) {
                        0 -> AppsScreen(
                            appListModel = appListModel,
                            scrollBehavior = scrollBehavior,
                            topClearance = innerPadding.calculateTopPadding(),
                            bottomClearance = GlassNavTotalHeight + systemNavBarsHeight,
                        )
                        1 -> SettingsScreen(
                            scrollBehavior = scrollBehavior,
                            topClearance = innerPadding.calculateTopPadding(),
                            bottomClearance = GlassNavTotalHeight + systemNavBarsHeight,
                        )
                    }

                    InstallerOverlay(
                        state = installerState,
                        onDismiss = { installerState.dismiss() }
                    )
                }

                // 浮动玻璃导航栏：位置同 Termux Ultra（两侧 24.dp，底部贴系统导航栏）。
                GlassNavigationBar(
                    items = icons.mapIndexed { index, icon ->
                        GlassNavigationItem(
                            icon = icon,
                            label = labels[index],
                        )
                    },
                    selectedIndex = selectedTab,
                    onSelect = { index -> selectedTab = index },
                    backdrop = glassBackdrop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = systemNavBarsHeight),
                )
            }
        }
    }
}

/** 浮动玻璃导航栏自身高度（GlassNavigationBarDefaults.Height）。 */
val GlassNavTotalHeight = 54.dp
