package com.kira.jarplay.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppItem
import com.kira.jarplay.applist.AppListModel
import com.kira.jarplay.util.AppUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AppsScreen(appListModel: AppListModel, scrollBehavior: ScrollBehavior? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<AppItem?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.Observer<List<AppItem>> { newApps ->
            apps = newApps ?: emptyList()
        }
        appListModel.getAppRepository().observeApps(lifecycleOwner, observer)
        onDispose {
            // Observer will be automatically removed by LifecycleOwner
        }
    }

    if (apps.isEmpty()) {
        EmptyAppsState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (scrollBehavior != null) {
                        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps, key = { it.id }) { appItem ->
                AppItemCard(
                    item = appItem,
                    onLaunch = {
                        com.kira.jarplay.config.Config.startApp(
                            context, appItem.title, appItem.pathExt, false
                        )
                    },
                    onSettings = {
                        com.kira.jarplay.config.Config.startApp(
                            context, appItem.title, appItem.pathExt, true
                        )
                    },
                    onDelete = { pendingDelete = appItem }
                )
            }
        }
    }

    val toDelete = pendingDelete
    OverlayDialog(
        show = toDelete != null,
        title = toDelete?.title ?: "",
        summary = stringResource(R.string.app_delete_confirm),
        onDismissRequest = { pendingDelete = null },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.cancel_action),
                    onClick = { pendingDelete = null },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = stringResource(R.string.action_context_delete),
                    onClick = {
                        val item = toDelete
                        pendingDelete = null
                        if (item != null) {
                            AppUtils.deleteApp(item)
                            appListModel.getAppRepository().delete(item)
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.app_deleted),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
private fun EmptyAppsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = MiuixIcons.VerticalSplit,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_apps),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_apps_description),
                style = MiuixTheme.textStyles.main,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppItemCard(
    item: AppItem,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit
) {
    val launchLabel = stringResource(R.string.app_launch)
    val settingsLabel = stringResource(R.string.app_configure)
    val deleteLabel = stringResource(R.string.action_context_delete)

    val menuEntry = remember(onLaunch, onSettings, onDelete, launchLabel, settingsLabel, deleteLabel) {
        DropdownEntry(
            items = listOf(
                DropdownItem(text = launchLabel, onClick = onLaunch),
                DropdownItem(text = settingsLabel, onClick = onSettings),
                DropdownItem(text = deleteLabel, onClick = onDelete),
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        onClick = onLaunch,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(item = item, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    item.author?.takeIf { it.isNotEmpty() },
                    item.version?.takeIf { it.isNotEmpty() }
                ).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OverlayIconDropdownMenu(
                entry = menuEntry,
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }
    }
}

@Composable
fun AppIcon(item: AppItem, modifier: Modifier = Modifier) {
    val iconBitmap = remember(item.id) { AppUtils.getIconBitmap(item) }
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap.asImageBitmap(),
            contentDescription = item.title,
            modifier = modifier.clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.title.firstOrNull()?.toString().orEmpty(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
