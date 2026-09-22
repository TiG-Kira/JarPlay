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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppListModel
import com.kira.jarplay.installer.InstallerOverlay
import com.kira.jarplay.installer.InstallerState
import com.kira.jarplay.ui.apps.AppsScreen
import com.kira.jarplay.ui.settings.SettingsScreen
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.util.FileUtils
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
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
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    val labels = listOf(
        stringResource(R.string.apps),
        stringResource(R.string.settings),
    )
    val icons = listOf(MiuixIcons.VerticalSplit, MiuixIcons.Settings)

    val scrollBehavior = MiuixScrollBehavior()

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

    val navigationEventDispatcher = remember { NavigationEventDispatcher() }
    val navigationEventDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = navigationEventDispatcher
        }
    }

    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = labels[selectedTab],
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                NavigationBar {
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = icons[index],
                            label = label,
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = {
                            // JAR/JAD 的 MIME 类型并不统一，交给系统选择器自己过滤更可靠
                            filePickerLauncher.launch("*/*")
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = stringResource(R.string.install_app),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> AppsScreen(appListModel = appListModel, scrollBehavior = scrollBehavior)
                    1 -> SettingsScreen(scrollBehavior = scrollBehavior)
                }

                InstallerOverlay(
                    state = installerState,
                    onDismiss = { installerState.dismiss() }
                )
            }
        }
    }
}