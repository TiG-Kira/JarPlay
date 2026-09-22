package com.kira.jarplay.installer

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppItem
import com.kira.jarplay.jar.Descriptor
import com.kira.jarplay.util.AppUtils
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallerOverlay(
    state: InstallerState,
    onDismiss: () -> Unit
) {
    val show by state.showDialog.collectAsState()
    val status by state.status.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            state.selectJarFile(uri)
        }
    }

    OverlayDialog(
        show = show,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.installer_title),
    ) {
        when (val currentStatus = status) {
            is InstallerStatus.Loading -> {
                LoadingContent()
            }
            is InstallerStatus.ShowInfo -> {
                ShowInfoContent(
                    status = currentStatus,
                    onConfirm = { state.confirmInstall() },
                    onRun = {
                        state.runInstalledApp(context)
                    },
                    onDismiss = onDismiss
                )
            }
            is InstallerStatus.NeedSelectJar -> {
                SelectJarContent(
                    onSelectJar = { filePickerLauncher.launch("application/java-archive") },
                    onDismiss = onDismiss
                )
            }
            is InstallerStatus.Converting -> {
                ConvertingContent()
            }
            is InstallerStatus.Success -> {
                SuccessContent(
                    app = currentStatus.app,
                    onStart = { state.startApp(context, currentStatus.app) },
                    onDismiss = onDismiss
                )
            }
            is InstallerStatus.Error -> {
                ErrorContent(
                    message = currentStatus.message,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MiuixTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.converting_wait),
            style = MiuixTheme.textStyles.main
        )
    }
}

@Composable
private fun ShowInfoContent(
    status: InstallerStatus.ShowInfo,
    onConfirm: () -> Unit,
    onRun: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreInstallIcon(
                iconPath = status.iconPath,
                descriptor = status.descriptor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.descriptor.name,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status.descriptor.vendor,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val versionInfo = if (status.appExists && status.currentVersion != null) {
            if (status.descriptor.compareVersion(status.currentVersion) > 0) {
                stringResource(
                    R.string.reinstall_newest,
                    status.descriptor.version,
                    status.currentVersion
                )
            } else if (status.descriptor.compareVersion(status.currentVersion) < 0) {
                stringResource(
                    R.string.reinstall_older,
                    status.descriptor.version,
                    status.currentVersion
                )
            } else {
                stringResource(R.string.reinstall)
            }
        } else {
            ""
        }

        if (versionInfo.isNotEmpty()) {
            Text(
                text = versionInfo,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val info = buildString {
            append(stringResource(R.string.midlet_name)).append(status.descriptor.name).append('\n')
            append(stringResource(R.string.midlet_vendor)).append(status.descriptor.vendor).append('\n')
            append(stringResource(R.string.midlet_version)).append(status.descriptor.version)
        }

        Text(
            text = info,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface
        )

        if (status.isFromNet) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.warn_install_from_net),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (status.canRun) {
                TextButton(
                    text = stringResource(R.string.START_CMD),
                    onClick = onRun
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(
                    text = if (status.appExists) stringResource(R.string.action_reinstall) else stringResource(R.string.install)
                )
            }
        }
    }
}

@Composable
private fun SelectJarContent(
    onSelectJar: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.install_jar_needed),
            style = MiuixTheme.textStyles.body2
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSelectJar,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(text = stringResource(R.string.choose))
            }
        }
    }
}

@Composable
private fun ConvertingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.installer_title),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MiuixTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.converting_wait),
            style = MiuixTheme.textStyles.main
        )
    }
}

@Composable
private fun SuccessContent(
    app: AppItem,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstalledAppIcon(item = app)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.title,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.install_done),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(text = stringResource(R.string.START_CMD))
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun PreInstallIcon(iconPath: String?, descriptor: Descriptor) {
    if (iconPath != null) {
        val bitmap = remember(iconPath) {
            try {
                BitmapFactory.decodeFile(iconPath)
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = descriptor.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            return
        }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = descriptor.name.first().toString(),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun InstalledAppIcon(item: AppItem) {
    val bitmap = remember(item.id) { AppUtils.getIconBitmap(item) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = item.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
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
