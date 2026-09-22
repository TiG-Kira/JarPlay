package com.kira.jarplay.installer

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import com.kira.jarplay.R
import com.kira.jarplay.applist.AppItem
import com.kira.jarplay.appsdb.AppRepository
import com.kira.jarplay.config.Config
import com.kira.jarplay.jar.Descriptor
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MIDLET_JAR_SIZE = "MIDlet-Jar-Size"

sealed class InstallerStatus {
    data object Loading : InstallerStatus()
    data class ShowInfo(
        val descriptor: Descriptor,
        val manifest: Descriptor?,
        val iconPath: String?,
        val appExists: Boolean,
        val currentVersion: String?,
        val isFromNet: Boolean,
        val canRun: Boolean
    ) : InstallerStatus()
    data class NeedSelectJar(val descriptor: Descriptor) : InstallerStatus()
    data class Converting(val descriptor: Descriptor) : InstallerStatus()
    data class Success(val app: AppItem) : InstallerStatus()
    data class Error(val message: String) : InstallerStatus()
}

class InstallerState(
    private val context: Context,
    private val appRepository: AppRepository
) {
    private val compositeDisposable = CompositeDisposable()
    private var installer: AppInstaller? = null
    private var isFromNet: Boolean = false

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _status = MutableStateFlow<InstallerStatus>(InstallerStatus.Loading)
    val status: StateFlow<InstallerStatus> = _status.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    fun installFromUri(uri: Uri) {
        isFromNet = uri.scheme == "http" || uri.scheme == "https"
        _title.value = context.getString(R.string.installer_title)
        _status.value = InstallerStatus.Loading
        _showDialog.value = true
        startInstall(null, uri)
    }

    fun reinstallApp(id: Int) {
        isFromNet = false
        _title.value = context.getString(R.string.installer_title)
        _status.value = InstallerStatus.Loading
        _showDialog.value = true
        startReinstall(id)
    }

    fun dismiss() {
        clear()
        _showDialog.value = false
    }

    fun confirmInstall() {
        val currentInstaller = installer ?: return
        _status.value = InstallerStatus.Converting(currentInstaller.newDescriptor)
        _title.value = context.getString(R.string.installer_title)
        val disposable = Single.create(currentInstaller::install)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Int -> onInstallProgress(result) },
                { e: Throwable -> onError(e) }
            )
        compositeDisposable.add(disposable)
    }

    fun selectJarFile(uri: Uri) {
        val currentInstaller = installer ?: return
        val disposable = currentInstaller.updateInfo(uri)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Int -> onLoadProgress(result) },
                { e: Throwable -> onError(e) }
            )
        compositeDisposable.add(disposable)
    }

    fun startApp(context: Context, app: AppItem) {
        Config.startApp(context, app.title, app.pathExt, false)
        dismiss()
    }

    fun runInstalledApp(context: Context) {
        val currentInstaller = installer ?: return
        val app = currentInstaller.existsApp
        currentInstaller.clearCache()
        currentInstaller.deleteTemp()
        Config.startApp(context, app.title, app.pathExt, false)
        dismiss()
    }

    private fun startInstall(path: String?, uri: Uri) {
        val appContext = context.applicationContext as Application
        val currentInstaller = AppInstaller(path, uri, appContext, appRepository)
        installer = currentInstaller
        val disposable = Single.create(currentInstaller::loadInfo)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Int -> onLoadProgress(result) },
                { e: Throwable -> onError(e) }
            )
        compositeDisposable.add(disposable)
    }

    private fun startReinstall(id: Int) {
        val appContext = context.applicationContext as Application
        val currentInstaller = AppInstaller(id, appContext, appRepository)
        installer = currentInstaller
        val disposable = Single.create(currentInstaller::loadInfo)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Int -> onLoadProgress(result) },
                { e: Throwable -> onError(e) }
            )
        compositeDisposable.add(disposable)
    }

    private fun onLoadProgress(status: Int) {
        val currentInstaller = installer ?: return
        val nd = currentInstaller.newDescriptor

        when (status) {
            AppInstaller.STATUS_NEW -> {
                if (currentInstaller.jar != null) {
                    confirmInstall()
                    return
                }
                showInfo(nd, currentInstaller.manifest, currentInstaller.iconPath, false, null, false)
            }
            AppInstaller.STATUS_OLDEST -> {
                showInfo(nd, currentInstaller.manifest, currentInstaller.iconPath, true, currentInstaller.currentVersion, false)
            }
            AppInstaller.STATUS_EQUAL -> {
                showInfo(nd, currentInstaller.manifest, currentInstaller.iconPath, true, currentInstaller.currentVersion, true)
            }
            AppInstaller.STATUS_NEWEST -> {
                showInfo(nd, currentInstaller.manifest, currentInstaller.iconPath, true, currentInstaller.currentVersion, false)
            }
            AppInstaller.STATUS_UNMATCHED -> {
                showInfo(nd, currentInstaller.manifest, currentInstaller.iconPath, false, null, false)
            }
            AppInstaller.STATUS_NEED_JAD -> {
                _status.value = InstallerStatus.NeedSelectJar(nd)
            }
        }
    }

    private fun onInstallProgress(status: Int) {
        val currentInstaller = installer ?: return
        if (status == AppInstaller.STATUS_SUCCESS) {
            val app = currentInstaller.existsApp
            _title.value = context.getString(R.string.installer_title)
            _status.value = InstallerStatus.Success(app)
            return
        }
        onLoadProgress(status)
    }

    private fun showInfo(
        descriptor: Descriptor,
        manifest: Descriptor?,
        iconPath: String?,
        appExists: Boolean,
        currentVersion: String?,
        canRun: Boolean
    ) {
        _title.value = context.getString(R.string.installer_title)
        _status.value = InstallerStatus.ShowInfo(
            descriptor = descriptor,
            manifest = manifest,
            iconPath = iconPath,
            appExists = appExists,
            currentVersion = currentVersion,
            isFromNet = isFromNet,
            canRun = canRun
        )
    }

    private fun onError(e: Throwable) {
        e.printStackTrace()
        clear()
        _status.value = InstallerStatus.Error("${context.getString(R.string.error)}: ${e.message}")
    }

    private fun clear() {
        installer?.apply {
            clearCache()
            deleteTemp()
        }
        installer = null
        compositeDisposable.clear()
    }

    fun getIconDrawable(iconPath: String?): Drawable? {
        if (iconPath == null) return null
        return Drawable.createFromPath(iconPath)
    }

    fun getDescriptorInfo(descriptor: Descriptor): CharSequence {
        val info = descriptor.getInfo(context)
        val jarSize = descriptor.attrs[MIDLET_JAR_SIZE]
        val jarUrl = descriptor.jarUrl
        if (jarSize != null && jarUrl != null) {
            info.append('\n').append(context.getString(R.string.midlet_source)).append(jarUrl)
        }
        return info
    }

    fun getReinstallMessage(): String {
        return context.getString(R.string.reinstall)
    }

    fun getReinstallOlderMessage(newVersion: String, oldVersion: String): String {
        return context.getString(R.string.reinstall_older, newVersion, oldVersion)
    }

    fun getReinstallNewestMessage(newVersion: String, oldVersion: String): String {
        return context.getString(R.string.reinstall_newest, newVersion, oldVersion)
    }
}