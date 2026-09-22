package com.kira.jarplay.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kira.jarplay.R
import com.kira.jarplay.ui.config.ConfigDetailScreen
import com.kira.jarplay.ui.theme.JarPlayTheme
import com.kira.jarplay.util.Constants.ACTION_EDIT
import com.kira.jarplay.util.Constants.ACTION_EDIT_PROFILE
import com.kira.jarplay.util.Constants.KEY_MIDLET_NAME
import java.io.File
import javax.microedition.shell.MicroActivity

class ConfigActivity : ComponentActivity() {

    private var profileDirPath: String? = null
    private var midletName: String? = null
    private var isProfile: Boolean = false
    private var workDir: String? = null
    private var needShow: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent.action
        isProfile = ACTION_EDIT_PROFILE == action
        needShow = isProfile || ACTION_EDIT == action
        val path = intent.dataString

        if (path == null) {
            needShow = false
            finish()
            return
        }

        if (isProfile) {
            profileDirPath = File(Config.getProfilesDir(), path).absolutePath
            workDir = Config.getEmulatorDir()
        } else {
            midletName = intent.getStringExtra(KEY_MIDLET_NAME)
            val appDir = File(path)
            val convertedDir = appDir.parentFile
            if (!appDir.isDirectory || convertedDir == null) {
                needShow = false
                finish()
                return
            }
            workDir = convertedDir.parent
            if (workDir == null) {
                needShow = false
                finish()
                return
            }
            profileDirPath = File(workDir + Config.MIDLET_CONFIGS_DIR + appDir.name).absolutePath
        }

        val configDir = File(profileDirPath!!)
        configDir.mkdirs()

        if (!needShow) {
            val i = Intent(this, MicroActivity::class.java)
            i.data = intent.data
            i.putExtra(KEY_MIDLET_NAME, intent.getStringExtra(KEY_MIDLET_NAME))
            startActivity(i)
            finish()
            return
        }

        val finalMidletName = midletName
        val finalWorkDir = workDir
        val finalIsProfile = isProfile
        val finalProfileDirPath = profileDirPath!!

        setContent {
            JarPlayTheme {
                ConfigDetailScreen(
                    profileDirPath = finalProfileDirPath,
                    midletName = finalMidletName,
                    isProfile = finalIsProfile,
                    workDir = finalWorkDir,
                    onBack = { finish() },
                    onStart = {
                        val i = Intent(this@ConfigActivity, MicroActivity::class.java)
                        i.data = intent.data
                        i.putExtra(KEY_MIDLET_NAME, intent.getStringExtra(KEY_MIDLET_NAME))
                        startActivity(i)
                        finish()
                    }
                )
            }
        }
    }
}