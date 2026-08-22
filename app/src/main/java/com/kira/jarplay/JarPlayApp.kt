package com.kira.jarplay

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager

import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.DialogConfigurationBuilder

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

import javax.microedition.util.ContextHolder

import com.kira.jarplay.util.Constants

class JarPlayApp : Application() {
    companion object {
        private val VALID_SIGNATURES = arrayOf(
            "78EF7758720A9902F731ED706F72C669C39B765C",
            "289F84A32207DF89BE749481ED4BD07E15FC268F",
            "FA8AA497194847D5715BAA62C6344D75A936EBA6"
        )
    }

    private val themeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == Constants.PREF_THEME) {
            setNightMode(sharedPreferences.getString(Constants.PREF_THEME, null))
        }
    }

    @Suppress("ConstantConditions")
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
        ContextHolder.setApplication(this)
        ACRA.init(this, CoreConfigurationBuilder()
            .withBuildConfigClass(BuildConfig::class.java)
            .withParallel(false)
            .withSendReportsInDevMode(false)
            .withPluginConfigurations(
                DialogConfigurationBuilder()
                    .withTitle(getString(R.string.crash_dialog_title))
                    .withText(getString(R.string.crash_dialog_message))
                    .withPositiveButtonText(getString(R.string.report_crash))
                    .withResTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog)
                    .withEnabled(true)
                    .build()
            ))
        val enabled = isSignatureValid && BuildConfig.FLAVOR != "dev"
        ACRA.errorReporter.setEnabled(enabled)
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp.registerOnSharedPreferenceChangeListener(themeListener)
        setNightMode(sp.getString(Constants.PREF_THEME, null))
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    @get:SuppressLint("PackageManagerGetSignatures")
    private val isSignatureValid: Boolean
        get() {
            try {
                val signatures: Array<Signature>
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info = packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                    @Suppress("UNCHECKED_CAST")
                    signatures = info.signingInfo?.apkContentsSigners as? Array<Signature>
                        ?: return false
                } else {
                    val info = packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNATURES
                    )
                    @Suppress("UNCHECKED_CAST")
                    signatures = info.signatures as Array<Signature>
                }
                val md = MessageDigest.getInstance("SHA-1")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                    val sha1 = String.format("%032X", BigInteger(1, md.digest()))
                    if (Arrays.asList(*VALID_SIGNATURES).contains(sha1)) {
                        return true
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return false
        }

    fun setNightMode(theme: String?) {
        var themeValue = theme
        if (themeValue == null) {
            themeValue = getString(R.string.pref_theme_default)
        }
        when (themeValue) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "auto-battery" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            "auto-time" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}