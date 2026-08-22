package com.kira.jarplay.config;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import androidx.preference.PreferenceManager;

import com.kira.jarplay.BuildConfig;
import com.kira.jarplay.R;

import static com.kira.jarplay.util.Constants.*;

import com.kira.jarplay.util.FileUtils;

public class Config {
	public static final String DEX_OPT_CACHE_DIR = "dex_opt";
	public static final String FS_DIR = "/fs/";
	public static final String MIDLET_CONFIG_FILE = "/config.json";
	public static final String MIDLET_CONFIGS_DIR = "/configs/";
	public static final String MIDLET_DATA_DIR = "/data/";
	public static final String MIDLET_DEX_FILE = "/converted.dex";
	public static final String MIDLET_ICON_FILE = "/icon.png";
	public static final String MIDLET_KEY_LAYOUT_FILE = "/VirtualKeyboardLayout";
	public static final String MIDLET_MANIFEST_FILE = MIDLET_DEX_FILE + ".conf";
	public static final String MIDLET_RES_DIR = "/res";
	public static final String MIDLET_RES_FILE = "/res.jar";
	public static final String SHADERS_DIR = "/shaders/";

	private static String SCREENSHOTS_DIR;
	private static String emulatorDir;
	private static String dataDir;
	private static String configsDir;
	private static String profilesDir;
	private static String appDir;
	private static boolean initialized = false;

	private static final SharedPreferences.OnSharedPreferenceChangeListener sPrefListener =
			(sharedPreferences, key) -> {
				if (key.equals(PREF_EMULATOR_DIR)) {
					initDirs(sharedPreferences.getString(key, emulatorDir));
				}
			};

	private static synchronized void ensureInitialized() {
		if (initialized) return;
		Context context = ContextHolder.getAppContext();
		if (context == null) {
			initialized = true;
			return;
		}
		String appName = "JarPlay";
		if (!BuildConfig.FULL_EMULATOR) {
			appName = context.getString(R.string.app_name);
		}
		SCREENSHOTS_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
				+ "/" + appName;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String path = FileUtils.isExternalStorageLegacy() ?
				preferences.getString(PREF_EMULATOR_DIR, null) :
				context.getExternalFilesDir(null).getPath();
		if (path == null) {
			path = Environment.getExternalStorageDirectory() + "/" + appName;
		}
		initDirs(path);
		preferences.registerOnSharedPreferenceChangeListener(sPrefListener);
		initialized = true;
	}

	public static String getEmulatorDir() {
		ensureInitialized();
		return emulatorDir != null ? emulatorDir : "";
	}

	public static String getDataDir() {
		ensureInitialized();
		return dataDir != null ? dataDir : "";
	}

	public static String getConfigsDir() {
		ensureInitialized();
		return configsDir != null ? configsDir : "";
	}

	public static String getProfilesDir() {
		ensureInitialized();
		return profilesDir != null ? profilesDir : "";
	}

	public static String getAppDir() {
		ensureInitialized();
		return appDir != null ? appDir : "";
	}

	public static String getScreenshotsDir() {
		ensureInitialized();
		return SCREENSHOTS_DIR != null ? SCREENSHOTS_DIR : "";
	}

	public static String getShadersDir() {
		ensureInitialized();
		return emulatorDir + SHADERS_DIR;
	}

	public static String getFsInternalDir() {
		ensureInitialized();
		return emulatorDir + FS_DIR + "c/";
	}

	public static String getFsExternalDir() {
		ensureInitialized();
		if (FileUtils.isExternalStorageLegacy()) {
			return Environment.getExternalStorageDirectory().getPath() + "/";
		} else {
			return emulatorDir + FS_DIR + "e/";
		}
	}

	public static void startApp(Context context, String name, String path, boolean showSettings) {
		startApp(context, name, path, showSettings, null);
	}

	public static void startApp(Context context, String name, String path, boolean showSettings, String arguments) {
		File appDir = new File(path);
		String workDir = appDir.getParentFile().getParent();
		File file = new File(workDir + Config.MIDLET_CONFIGS_DIR + appDir.getName());
		if (showSettings || !file.exists()) {
			Intent intent = new Intent(ACTION_EDIT, Uri.parse(path),
					context, ConfigActivity.class);
			intent.putExtra(KEY_MIDLET_NAME, name);
			intent.putExtra(KEY_START_ARGUMENTS, arguments);
			context.startActivity(intent);
		} else {
			Intent intent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(path),
					context, MicroActivity.class);
			intent.putExtra(KEY_MIDLET_NAME, name);
			intent.putExtra(KEY_START_ARGUMENTS, arguments);
			context.startActivity(intent);
		}
	}

	private static void initDirs(String path) {
		emulatorDir = path;
		dataDir = emulatorDir + MIDLET_DATA_DIR;
		configsDir = emulatorDir + MIDLET_CONFIGS_DIR;
		profilesDir = emulatorDir + "/templates/";
		appDir = emulatorDir + "/converted/";
	}
}