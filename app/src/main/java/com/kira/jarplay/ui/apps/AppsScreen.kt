package com.kira.jarplay.ui.apps

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppItem
import com.kira.jarplay.applist.AppListModel
import com.kira.jarplay.util.AppUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AppsScreen(appListModel: AppListModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = MiuixIcons.VerticalSplit,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_apps),
                    style = MiuixTheme.textStyles.main,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_apps_description),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(apps) { appItem ->
                AppItemCard(
                    item = appItem,
                    onClick = {
                        com.kira.jarplay.config.Config.startApp(
                            context, appItem.title, appItem.pathExt, false
                        )
                    },
                    onSettingsClick = {
                        com.kira.jarplay.config.Config.startApp(
                            context, appItem.title, appItem.pathExt, true
                        )
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun AppItemCard(
    item: AppItem,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(item = item, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.author != null && item.author.isNotEmpty()) {
                    Text(
                        text = item.author,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = { showMenu = !showMenu }
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }

        if (showMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_settings),
                    style = MiuixTheme.textStyles.button,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            showMenu = false
                            onSettingsClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_context_rename),
                    style = MiuixTheme.textStyles.button,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showMenu = false }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AppIcon(item: AppItem, modifier: Modifier = Modifier) {
    val iconBitmap = remember(item.id) {
        AppUtils.getIconBitmap(item)
    }
    if (iconBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = iconBitmap.asImageBitmap(),
            contentDescription = item.title,
            modifier = modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.title.first().toString(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onPrimary
            )
        }
    }
}
