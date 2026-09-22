/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.kira.jarplay.config;

import android.util.SparseIntArray;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.util.ContextHolder;

import com.kira.jarplay.util.SparseIntArrayAdapter;

public class ProfileModel {
	public static final int VERSION = 4;

	/** 不缩放：虚拟屏幕按 1:1 像素绘制。 */
	public static final int SCALE_TYPE_NONE = 0;
	/** 等比缩放：保持虚拟屏幕宽高比，尽量放大以适配显示区域。 */
	public static final int SCALE_TYPE_FIT = 1;
	/** 拉伸铺满：不保持宽高比，直接拉伸填满整个显示区域。 */
	public static final int SCALE_TYPE_FILL = 2;

	/** 虚拟屏幕停靠位置：0 左、1 上、2 居中、3 右、4 下。 */
	public static final int GRAVITY_LEFT = 0;
	public static final int GRAVITY_TOP = 1;
	public static final int GRAVITY_CENTER = 2;
	public static final int GRAVITY_RIGHT = 3;
	public static final int GRAVITY_BOTTOM = 4;

	public static final int DEFAULT_SCREEN_WIDTH = 240;
	public static final int DEFAULT_SCREEN_HEIGHT = 320;
	public static final int DEFAULT_BACKGROUND_COLOR = 0xD0D0D0;

	/** True if this is a new profile (not yet saved to file) */
	public final transient boolean isNew;

	public transient File dir;

	@SerializedName("Version")
	public int version;

	@SerializedName("ScreenWidth")
	public int screenWidth;

	@SerializedName("ScreenHeight")
	public int screenHeight;

	@SerializedName("ScreenBackgroundColor")
	public int screenBackgroundColor;

	@SerializedName("ScreenScaleRatio")
	public int screenScaleRatio;

	@SerializedName("Orientation")
	public int orientation;

	@SerializedName("ScreenScaleToFit")
	public boolean screenScaleToFit;

	@SerializedName("ScreenKeepAspectRatio")
	public boolean screenKeepAspectRatio;

	@SerializedName("ScreenScaleType")
	public int screenScaleType;

	@SerializedName("ScreenGravity")
	public int screenGravity;

	@SerializedName("ScreenFilter")
	public boolean screenFilter;

	@SerializedName("ImmediateMode")
	public boolean immediateMode;

	@SerializedName("HwAcceleration")
	public boolean hwAcceleration;

	@SerializedName("GraphicsMode")
	public int graphicsMode;

	@SerializedName("Shader")
	public ShaderInfo shader;

	@SerializedName("ParallelRedrawScreen")
	public boolean parallelRedrawScreen;

	@SerializedName("ShowFps")
	public boolean showFps;

	@SerializedName("FpsLimit")
	public int fpsLimit;

	@SerializedName("ForceFullscreen")
	public boolean forceFullscreen;

	@SerializedName("FontSizeSmall")
	public int fontSizeSmall;

	@SerializedName("FontSizeMedium")
	public int fontSizeMedium;

	@SerializedName("FontSizeLarge")
	public int fontSizeLarge;

	@SerializedName("FontApplyDimensions")
	public boolean fontApplyDimensions;

	@SerializedName("FontAntiAlias")
	public boolean fontAA;

	@SerializedName("TouchInput")
	public boolean touchInput;

	@SerializedName("ShowKeyboard")
	public boolean showKeyboard;

	@SerializedName("VirtualKeyboardType")
	public int vkType;

	@SerializedName("ButtonShape")
	public int vkButtonShape;

	@SerializedName("VirtualKeyboardAlpha")
	public int vkAlpha;

	@SerializedName("VirtualKeyboardForceOpacity")
	public boolean vkForceOpacity;

	@SerializedName("VirtualKeyboardFeedback")
	public boolean vkFeedback;

	@SerializedName("VirtualKeyboardDelay")
	public int vkHideDelay;

	@SerializedName("VirtualKeyboardColorBackground")
	public int vkBgColor;

	@SerializedName("VirtualKeyboardColorBackgroundSelected")
	public int vkBgColorSelected;

	@SerializedName("VirtualKeyboardColorForeground")
	public int vkFgColor;

	@SerializedName("VirtualKeyboardColorForegroundSelected")
	public int vkFgColorSelected;

	@SerializedName("VirtualKeyboardColorOutline")
	public int vkOutlineColor;

	@SerializedName("Layout")
	public int keyCodesLayout;

	@JsonAdapter(SparseIntArrayAdapter.class)
	@SerializedName("KeyCodeMap")
	public SparseIntArray keyCodeMap;

	@JsonAdapter(SparseIntArrayAdapter.class)
	@SerializedName("KeyMappings")
	public SparseIntArray keyMappings;

	@SerializedName("SystemProperties")
	public String systemProperties;

	/**
	 * 无参构造。
	 * <p>
	 * Gson 反序列化时也会走这个构造方法，因此这里必须写入完整的默认值：
	 * 否则 config.json 中缺失的字段会保持 Java 的 0 值，导致
	 * ScreenScaleType=0（画面按原始分辨率显示在角落）以及
	 * ScreenBackgroundColor=0（黑屏）这类“配置被清零”的显示异常。
	 */
	@SuppressWarnings("unused") // Gson uses default constructor if present
	public ProfileModel() {
		isNew = false;
		applyDefaults();
	}

	public ProfileModel(File dir) {
		this.dir = dir;
		isNew = true;
		applyDefaults();
	}

	/** 返回一份带有完整默认值的新配置。 */
	public static ProfileModel defaults(File dir) {
		return new ProfileModel(dir);
	}

	/**
	 * 写入所有字段的默认值。只应被构造方法调用。
	 */
	private void applyDefaults() {
		version = VERSION;

		screenWidth = DEFAULT_SCREEN_WIDTH;
		screenHeight = DEFAULT_SCREEN_HEIGHT;
		screenBackgroundColor = DEFAULT_BACKGROUND_COLOR;
		// 默认拉伸铺满显示区域；旧版本默认值 1（等比缩放）在长屏手机上会留下大片黑边。
		screenScaleType = SCALE_TYPE_FILL;
		screenGravity = GRAVITY_CENTER;
		screenScaleRatio = 100;
		screenScaleToFit = true;
		screenKeepAspectRatio = true;
		graphicsMode = 1;

		fontSizeSmall = 18;
		fontSizeMedium = 22;
		fontSizeLarge = 26;
		fontAA = true;

		showKeyboard = true;
		touchInput = true;

		vkButtonShape = VirtualKeyboard.ROUND_RECT_SHAPE;
		vkAlpha = 64;

		vkBgColor = 0xD0D0D0;
		vkFgColor = 0x000080;
		vkBgColorSelected = 0x000080;
		vkFgColorSelected = 0xFFFFFF;
		vkOutlineColor = 0xFFFFFF;

		systemProperties = ContextHolder.getAssetAsString("defaults/system.props");
	}

	/**
	 * 把加载到的配置修正到合法区间，并补齐无法为空的字段。
	 * 必须在 {@link ProfilesManager#loadConfig(File)} 之后调用。
	 */
	public void normalize() {
		if (version <= 0) {
			version = VERSION;
		}
		if (screenWidth < 0 || screenHeight < 0) {
			screenWidth = Math.max(screenWidth, 0);
			screenHeight = Math.max(screenHeight, 0);
		}
		if (screenWidth == 0 && screenHeight == 0) {
			// 两者都为 0 表示“自动”，交给 Canvas 自行计算，保持原样。
		} else if (screenWidth <= 0 || screenHeight <= 0) {
			screenWidth = DEFAULT_SCREEN_WIDTH;
			screenHeight = DEFAULT_SCREEN_HEIGHT;
		}
		if (screenScaleRatio <= 0) {
			screenScaleRatio = 100;
		}
		if (screenScaleRatio > 100) {
			screenScaleRatio = 100;
		}
		if (screenScaleType < SCALE_TYPE_NONE || screenScaleType > SCALE_TYPE_FILL) {
			screenScaleType = SCALE_TYPE_FILL;
		}
		if (screenGravity < GRAVITY_LEFT || screenGravity > GRAVITY_BOTTOM) {
			screenGravity = GRAVITY_CENTER;
		}
		if (screenBackgroundColor == 0) {
			// 0 是历史遗留的“未设置”状态，会被渲染成纯黑背景。
			screenBackgroundColor = DEFAULT_BACKGROUND_COLOR;
		}
		if (graphicsMode < 0 || graphicsMode > 3) {
			graphicsMode = 1;
		}
		if (fontSizeSmall <= 0) {
			fontSizeSmall = 18;
		}
		if (fontSizeMedium <= 0) {
			fontSizeMedium = 22;
		}
		if (fontSizeLarge <= 0) {
			fontSizeLarge = 26;
		}
		if (systemProperties == null || systemProperties.isEmpty()) {
			systemProperties = ContextHolder.getAssetAsString("defaults/system.props");
		}
	}

	public ProfileModel copy() {
		ProfileModel newModel = defaults(dir);
		newModel.version = version;
		newModel.screenWidth = screenWidth;
		newModel.screenHeight = screenHeight;
		newModel.screenBackgroundColor = screenBackgroundColor;
		newModel.screenScaleRatio = screenScaleRatio;
		newModel.orientation = orientation;
		newModel.screenScaleToFit = screenScaleToFit;
		newModel.screenKeepAspectRatio = screenKeepAspectRatio;
		newModel.screenScaleType = screenScaleType;
		newModel.screenGravity = screenGravity;
		newModel.screenFilter = screenFilter;
		newModel.immediateMode = immediateMode;
		newModel.hwAcceleration = hwAcceleration;
		newModel.graphicsMode = graphicsMode;
		newModel.shader = shader;
		newModel.parallelRedrawScreen = parallelRedrawScreen;
		newModel.showFps = showFps;
		newModel.fpsLimit = fpsLimit;
		newModel.forceFullscreen = forceFullscreen;
		newModel.fontSizeSmall = fontSizeSmall;
		newModel.fontSizeMedium = fontSizeMedium;
		newModel.fontSizeLarge = fontSizeLarge;
		newModel.fontApplyDimensions = fontApplyDimensions;
		newModel.fontAA = fontAA;
		newModel.touchInput = touchInput;
		newModel.showKeyboard = showKeyboard;
		newModel.vkType = vkType;
		newModel.vkButtonShape = vkButtonShape;
		newModel.vkAlpha = vkAlpha;
		newModel.vkForceOpacity = vkForceOpacity;
		newModel.vkFeedback = vkFeedback;
		newModel.vkHideDelay = vkHideDelay;
		newModel.vkBgColor = vkBgColor;
		newModel.vkBgColorSelected = vkBgColorSelected;
		newModel.vkFgColor = vkFgColor;
		newModel.vkFgColorSelected = vkFgColorSelected;
		newModel.vkOutlineColor = vkOutlineColor;
		newModel.keyCodesLayout = keyCodesLayout;
		newModel.keyCodeMap = keyCodeMap;
		newModel.keyMappings = keyMappings;
		newModel.systemProperties = systemProperties;
		return newModel;
	}
}
