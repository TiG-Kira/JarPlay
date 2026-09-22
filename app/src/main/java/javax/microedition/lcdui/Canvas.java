/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2022 Nikita Shakarun
 * Copyright 2018-2022 Yriy Kharchenko
 * Copyright 2023 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui;

import static android.opengl.GLES20.*;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.lcdui.commands.AbstractSoftKeysBar;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.lcdui.event.CanvasEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventFilter;
import javax.microedition.lcdui.graphics.CanvasView;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.graphics.GlesView;
import javax.microedition.lcdui.graphics.ShaderProgram;
import javax.microedition.lcdui.keyboard.KeyMapper;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.lcdui.overlay.FpsCounter;
import javax.microedition.lcdui.overlay.Layer;
import javax.microedition.lcdui.overlay.Overlay;
import javax.microedition.lcdui.overlay.OverlayView;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import com.kira.jarplay.R;
import com.kira.jarplay.config.ShaderInfo;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Canvas extends Displayable {
	private static final String TAG = Canvas.class.getName();

	public static final int KEY_POUND = 35;
	public static final int KEY_STAR = 42;
	public static final int KEY_NUM0 = 48;
	public static final int KEY_NUM1 = 49;
	public static final int KEY_NUM2 = 50;
	public static final int KEY_NUM3 = 51;
	public static final int KEY_NUM4 = 52;
	public static final int KEY_NUM5 = 53;
	public static final int KEY_NUM6 = 54;
	public static final int KEY_NUM7 = 55;
	public static final int KEY_NUM8 = 56;
	public static final int KEY_NUM9 = 57;

	public static final int KEY_UP = -1;
	public static final int KEY_DOWN = -2;
	public static final int KEY_LEFT = -3;
	public static final int KEY_RIGHT = -4;
	public static final int KEY_FIRE = -5;
	public static final int KEY_SOFT_LEFT = -6;
	public static final int KEY_SOFT_RIGHT = -7;
	public static final int KEY_CLEAR = -8;
	public static final int KEY_SEND = -10;
	public static final int KEY_END = -11;

	public static final int UP = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 5;
	public static final int DOWN = 6;
	public static final int FIRE = 8;
	public static final int GAME_A = 9;
	public static final int GAME_B = 10;
	public static final int GAME_C = 11;
	public static final int GAME_D = 12;

	private static final float FULLSCREEN_HEIGHT_RATIO = 0.85f;

	private static boolean filter;
	private static boolean touchInput;
	private static int graphicsMode;
	private static ShaderInfo shaderFilter;
	private static boolean parallelRedraw;
	private static boolean forceFullscreen;
	private static boolean showFps;
	private static int backgroundColor;
	private static int scaleRatio = 100;
	private static int fpsLimit;
	private static boolean screenshotRawMode;
	private static int scaleType = 1;
	private static int screenGravity = 2;

	private final Object bufferLock = new Object();
	private final Object surfaceLock = new Object();
	private final PaintEvent paintEvent = new PaintEvent();
	private final SoftBar softBar = new SoftBar();
	private final CanvasWrapper canvasWrapper = new CanvasWrapper(filter);
	private final RectF virtualScreen = new RectF();

	protected int width, height;
	protected int maxHeight;
	private LinearLayout layout;
	private SurfaceView innerView;
	private Surface surface;
	private GLRenderer renderer;
	private int displayWidth;
	private int displayHeight;
	private boolean fullscreen;
	private boolean visible;
	private boolean sizeChangedCalled;
	private Image offscreen;
	private Image offscreenCopy;
	private int onX, onY, onWidth, onHeight;
	private long lastFrameTime = System.currentTimeMillis();
	private Handler uiHandler;
	private Overlay overlay;
	private FpsCounter fpsCounter;
	private boolean skipLeftSoft;
	private boolean skipRightSoft;
	private int[][] lastPointerPos = new int[20][2];

	/**
	 * 游戏实际绘制过的像素范围（帧缓冲坐标），用于把画面按“内容”而不是按整个画布来铺满。
	 * <p>
	 * 部分老游戏把分辨率硬编码在代码里（例如《仙剑奇侠传》只画 128x144），完全无视画布大小，
	 * 于是画面只出现在画布左上角、其余区域永远是空白。这里累计记录 alpha 非 0 的像素范围
	 * （{@link Image#createImage(int, int)} 生成的位图初始全透明，只要游戏画过就会写入
	 * alpha），从而知道真正有内容的区域。
	 */
	private int contentLeft = Integer.MAX_VALUE;
	private int contentTop = Integer.MAX_VALUE;
	private int contentRight = -1;
	private int contentBottom = -1;
	private int[] contentScanBuffer;
	private int contentScanTick;

	/** 当前用于显示的内容区域（帧缓冲坐标）。缩放模式作用在这个区域上，而不是整个画布。 */
	private int srcLeft, srcTop, srcWidth, srcHeight;
	private final Rect srcRect = new Rect();

	protected Canvas() {
		this(forceFullscreen);
	}

	protected Canvas(boolean fullscreen) {
		this.fullscreen = fullscreen;
		super.softBar = softBar;
		if (graphicsMode == 1) {
			renderer = new GLRenderer();
		}
		if (parallelRedraw) {
			uiHandler = new Handler(Looper.getMainLooper(), msg -> repaintScreen());
		}
		displayWidth = ContextHolder.getDisplayWidth();
		displayHeight = ContextHolder.getDisplayHeight();
		updateSize();
	}

	public static void setShaderFilter(ShaderInfo shader) {
		Canvas.shaderFilter = shader;
	}

	public static void setScale(int screenGravity, int scaleType, int scaleRatio) {
		Canvas.screenGravity = screenGravity;
		Canvas.scaleType = scaleType;
		Canvas.scaleRatio = scaleRatio;
	}

	public static void setBackgroundColor(int color) {
		backgroundColor = color | 0xFF000000;
	}

	public static void setFilterBitmap(boolean filter) {
		Canvas.filter = filter;
	}

	public static void setHasTouchInput(boolean touchInput) {
		Canvas.touchInput = touchInput;
	}

	public static void setGraphicsMode(int mode, boolean parallel) {
		Canvas.graphicsMode = mode;
		Canvas.parallelRedraw = (mode == 0 || mode == 3) && parallel;
	}

	public static void setForceFullscreen(boolean forceFullscreen) {
		Canvas.forceFullscreen = forceFullscreen;
	}

	public static void setShowFps(boolean showFps) {
		Canvas.showFps = showFps;
	}

	public static void setLimitFps(int fpsLimit) {
		if (fpsLimit == 0 && (graphicsMode == 1 || graphicsMode == 2)) {
			// hack for async redraw
			fpsLimit = 1000;
		}
		Canvas.fpsLimit = fpsLimit;
	}

	public static void setScreenshotRawMode(boolean enable) {
		screenshotRawMode = enable;
	}

	public int getKeyCode(int gameAction) {
		int res = KeyMapper.getKeyCode(gameAction);
		if (res != Integer.MAX_VALUE) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown game action " + gameAction);
		}
	}

	public int getGameAction(int keyCode) {
		return KeyMapper.getGameAction(keyCode);
	}

	public String getKeyName(int keyCode) {
		String res = KeyMapper.getKeyName(keyCode);
		if (res != null) {
			return res;
		} else {
			throw new IllegalArgumentException("unknown keycode " + keyCode);
		}
	}

	public void postKeyPressed(int keyCode) {
		if (keyCode == KEY_SOFT_LEFT && softBar.fireLeftSoft()) {
			skipLeftSoft = true;
			return;
		} else if (keyCode == KEY_SOFT_RIGHT && softBar.fireRightSoft()) {
			skipRightSoft = true;
			return;
		}
		Display.postEvent(CanvasEvent.getInstance(this,
				CanvasEvent.KEY_PRESSED,
				KeyMapper.convertKeyCode(keyCode)));
	}

	public void postKeyReleased(int keyCode) {
		if (keyCode == KEY_SOFT_LEFT && skipLeftSoft) {
			skipLeftSoft = false;
			return;
		} else if (keyCode == KEY_SOFT_RIGHT && skipRightSoft) {
			skipRightSoft = false;
			return;
		}
		Display.postEvent(CanvasEvent.getInstance(this,
				CanvasEvent.KEY_RELEASED,
				KeyMapper.convertKeyCode(keyCode)));
	}

	public void postKeyRepeated(int keyCode) {
		if (keyCode == KEY_SOFT_LEFT && skipLeftSoft) {
			return;
		} else if (keyCode == KEY_SOFT_RIGHT && skipRightSoft) {
			return;
		}
		Display.postEvent(CanvasEvent.getInstance(this,
				CanvasEvent.KEY_REPEATED,
				KeyMapper.convertKeyCode(keyCode)));
	}

	public void doShowNotify() {
		visible = true;
		showNotify();
	}

	public void doHideNotify() {
		hideNotify();
		visible = false;
	}

	public void onDraw(android.graphics.Canvas canvas) {
		if (graphicsMode != 2) return; // Fix for Android Pie
		CanvasWrapper g = canvasWrapper;
		g.bind(canvas);
		g.clear(backgroundColor);
		synchronized (bufferLock) {
			offscreenCopy.getBitmap().prepareToDraw();
			g.drawImage(offscreenCopy, srcRect, virtualScreen);
		}
		if (fpsCounter != null) {
			fpsCounter.increment();
		}
	}

	public Single<Bitmap> getScreenShot() {
		if (renderer != null && !screenshotRawMode) {
			return renderer.takeScreenShot();
		}
		return Single.create(emitter -> {
			Bitmap bitmap;
			if (screenshotRawMode) {
				synchronized (bufferLock) {
					bitmap = Bitmap.createBitmap(offscreenCopy.getBitmap(), 0, 0,
							offscreenCopy.getWidth(), offscreenCopy.getHeight());
				}
			} else {
				bitmap = Bitmap.createBitmap(onWidth, onHeight, Bitmap.Config.ARGB_8888);
				canvasWrapper.bind(new android.graphics.Canvas(bitmap));
				synchronized (bufferLock) {
					canvasWrapper.drawImage(offscreenCopy, srcRect, new RectF(0, 0, onWidth, onHeight));
				}
			}
			emitter.onSuccess(bitmap);
		});
	}

	private boolean checkSizeChanged() {
		int tmpWidth = width;
		int tmpHeight = height;
		updateSize();
		return width != tmpWidth || height != tmpHeight;
	}

	/**
	 * Update the size and position of the virtual screen relative to the real one.
	 */
	public void updateSize() {
		int previousWidth = width;
		int previousHeight = height;
		/*
		 * We turn the sizes of the virtual screen into the sizes of the visible canvas.
		 *
		 * At the same time, we take into account that one or both virtual sizes can be less
		 * than zero, which means auto-selection of this size so that the resulting canvas
		 * has the same aspect ratio as the actual screen of the device.
		 */
		if (displayWidth <= 0 || displayHeight <= 0) {
			// 显示区域尺寸未知时（例如布局尚未完成）回退到设备参数，避免出现除零与零尺寸画面。
			displayWidth = ContextHolder.getDisplayWidth();
			displayHeight = ContextHolder.getDisplayHeight();
			if (displayWidth <= 0) {
				displayWidth = Math.max(virtualWidth, 1);
			}
			if (displayHeight <= 0) {
				displayHeight = Math.max(virtualHeight, 1);
			}
		}

		int scaledDisplayHeight;
		VirtualKeyboard vk = ContextHolder.getVk();
		boolean isPhoneSkin = vk != null && vk.isPhone();

		// if phone keyboard layout is active, then scale down the virtual screen
		if (isPhoneSkin) {
			float vkHeight = vk.getPhoneKeyboardHeight(displayWidth, displayHeight);
			scaledDisplayHeight = (int) (displayHeight - vkHeight - 1);
		} else {
			scaledDisplayHeight = displayHeight;
		}
		if (scaledDisplayHeight <= 0) {
			scaledDisplayHeight = displayHeight;
		}
		if (virtualWidth > 0) {
			if (virtualHeight > 0) {
				/*
				 * the width and height of the canvas are strictly set
				 */
				width = virtualWidth;
				height = virtualHeight;
			} else {
				/*
				 * only the canvas width is set
				 * height is selected by the ratio of the real screen
				 */
				width = virtualWidth;
				height = Math.max(1, scaledDisplayHeight * virtualWidth / displayWidth);
			}
		} else {
			if (virtualHeight > 0) {
				/*
				 * only the canvas height is set
				 * width is selected by the ratio of the real screen
				 */
				width = Math.max(1, displayWidth * virtualHeight / scaledDisplayHeight);
				height = virtualHeight;
			} else {
				/*
				 * nothing is set - screen-sized canvas
				 */
				width = displayWidth;
				height = scaledDisplayHeight;
			}
		}

		/*
		 * Determine which part of the framebuffer actually holds the MIDlet's picture.
		 *
		 * Some old games hard-code their resolution and draw into a corner of the canvas, so
		 * scaling the whole canvas would leave large empty borders. When the painted area is
		 * noticeably smaller than the canvas we scale that area instead, and the game fills
		 * the display area no matter which resolution is configured.
		 */
		int contentW = contentRight - contentLeft + 1;
		int contentH = contentBottom - contentTop + 1;
		if (contentW <= 0 || contentH <= 0
				|| (contentW >= width - 3 && contentH >= height - 3)) {
			// nothing painted yet, or the picture already covers the whole canvas
			srcLeft = 0;
			srcTop = 0;
			srcWidth = Math.max(1, width);
			srcHeight = Math.max(1, height);
		} else {
			srcLeft = Math.max(0, contentLeft);
			srcTop = Math.max(0, contentTop);
			srcWidth = Math.min(contentW, width - srcLeft);
			srcHeight = Math.min(contentH, height - srcTop);
			if (srcWidth <= 0 || srcHeight <= 0) {
				srcLeft = 0;
				srcTop = 0;
				srcWidth = Math.max(1, width);
				srcHeight = Math.max(1, height);
			}
		}
		srcRect.set(srcLeft, srcTop, srcLeft + srcWidth, srcTop + srcHeight);

		/*
		 * We turn the size of the canvas into the size of the image
		 * that will be displayed on the screen of the device.
		 */
		int scaleRatio = Canvas.scaleRatio;
		if (scaleRatio <= 0) {
			// 配置损坏（例如被写成 0）时曾导致整个画面被压缩成 0 像素，这里兜底成 100%。
			scaleRatio = 100;
		}
		switch (scaleType) {
			case 0:
				// without scaling
				onWidth = srcWidth;
				onHeight = srcHeight;
				break;
			case 2:
				// scaling without preserving the aspect ratio:
				// just stretch the picture to full screen
				onWidth = displayWidth;
				onHeight = scaledDisplayHeight;
				if (scaleRatio > 100) {
					scaleRatio = 100;
				}
				break;
			case 1:
			default:
				// fit inside the display area while preserving the aspect ratio,
				// scaling the picture up when possible
				float fitScale = Math.min(
						(float) displayWidth / srcWidth,
						(float) scaledDisplayHeight / srcHeight);
				if (!(fitScale > 0f)) {
					fitScale = 1f;
				}
				onWidth = Math.max(1, Math.round(srcWidth * fitScale));
				onHeight = Math.max(1, Math.round(srcHeight * fitScale));
				if (scaleRatio > 100) {
					scaleRatio = 100;
				}
				break;
		}

		onWidth = Math.max(1, onWidth * scaleRatio / 100);
		onHeight = Math.max(1, onHeight * scaleRatio / 100);

		switch (Canvas.screenGravity) {
			case 0: // left
				onX = 0;
				onY = (scaledDisplayHeight - onHeight) / 2;
				break;
			case 1: // top
				onX = (displayWidth - onWidth) / 2;
				onY = 0;
				break;
			case 3: // right
				onX = displayWidth - onWidth;
				onY = (scaledDisplayHeight - onHeight) / 2;
				break;
			case 4: // bottom
				onX = (displayWidth - onWidth) / 2;
				onY = scaledDisplayHeight - onHeight;
				break;
			case 2: // center
			default:
				onX = (displayWidth - onWidth) / 2;
				onY = (scaledDisplayHeight - onHeight) / 2;
				break;
		}


		/*
		 * calculate the maximum height
		 */
		maxHeight = height;

		softBar.resize();
		/*
		 * calculate the current height
		 */
		float softBarHeight = softBar.bounds.height();
		if (softBarHeight > 0) {
			float scaleY = (float) onHeight / srcHeight;
			height = (int) (height - softBarHeight / scaleY);
			onHeight -= softBarHeight;
		}

		RectF screen = new RectF(0, 0, displayWidth, displayHeight);
		virtualScreen.set(onX, onY, onX + onWidth, onY + onHeight);

		if (previousWidth != width || previousHeight != height) {
			// 画布尺寸变了，之前记录的内容范围不再有效
			resetContentBounds();
		}

		synchronized (bufferLock) {
			if (offscreen == null) {
				offscreen = Image.createImage(width, maxHeight, 0);
				offscreenCopy = Image.createImage(width, maxHeight, 0);
			}
			if (offscreen.getWidth() != width || offscreen.getHeight() != height) {
				offscreen.setSize(width, height);
				offscreenCopy.setSize(width, height);
			}
		}
		if (overlay != null) {
			overlay.resize(screen, onX, onY, onX + onWidth, onY + onHeight + softBarHeight);
		}

		if (graphicsMode == 1) {
			float gl = 2.0f * virtualScreen.left / displayWidth - 1.0f;
			float gt = 1.0f - 2.0f * virtualScreen.top / displayHeight;
			float gr = 2.0f * virtualScreen.right / displayWidth - 1.0f;
			float gb = 1.0f - 2.0f * virtualScreen.bottom / displayHeight;
			Bitmap bitmap = offscreen.getBitmap();
			float bw = Math.max(1, bitmap.getWidth());
			float bh = Math.max(1, bitmap.getHeight());
			float u0 = Math.min(1f, srcLeft / bw);
			float v0 = Math.min(1f, srcTop / bh);
			float u1 = Math.min(1f, (srcLeft + srcWidth) / bw);
			float v1 = Math.min(1f, (srcTop + srcHeight) / bh);
			renderer.updateSize(gl, gt, gr, gb, u0, v0, u1, v1);
		}
		repaintInternal();
	}

	/** 清空记录的内容范围（画布尺寸变化时调用）。 */
	private void resetContentBounds() {
		contentLeft = Integer.MAX_VALUE;
		contentTop = Integer.MAX_VALUE;
		contentRight = -1;
		contentBottom = -1;
		contentScanTick = 0;
	}

	/**
	 * 记录 MIDlet 实际绘制过的像素范围。
	 * <p>
	 * 每帧扫描会有开销，因此低频抽样；一旦画面已经铺满画布就彻底停止扫描。
	 * 注意范围只增不减：先出现一个小的启动 LOGO、之后再画满全屏的游戏也能被正确处理。
	 */
	private void updateContentBounds() {
		if (contentLeft == 0 && contentTop == 0 && contentRight >= width && contentBottom >= height) {
			return;	// 已经铺满画布，无需再扫描
		}
		int tick = contentScanTick++;
		if (tick % 12 != 0) {
			return;
		}
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
		synchronized (bufferLock) {
			if (offscreen == null) {
				return;
			}
			Bitmap bitmap = offscreen.getBitmap();
			int bw = bitmap.getWidth();
			int bh = bitmap.getHeight();
			if (bw <= 0 || bh <= 0) {
				return;
			}
			if (contentScanBuffer == null || contentScanBuffer.length < bw * bh) {
				contentScanBuffer = new int[bw * bh];
			}
			bitmap.getPixels(contentScanBuffer, 0, bw, 0, 0, bw, bh);
			int[] pixels = contentScanBuffer;
			for (int y = 0; y < bh; y++) {
				int rowStart = y * bw;
				for (int x = 0; x < bw; x++) {
					if ((pixels[rowStart + x] >>> 24) != 0) {
						if (x < minX) minX = x;
						if (x > maxX) maxX = x;
						if (y < minY) minY = y;
						if (y > maxY) maxY = y;
					}
				}
			}
		}
		if (maxX < 0) {
			return;
		}
		boolean changed = false;
		if (minX < contentLeft) {
			contentLeft = minX;
			changed = true;
		}
		if (minY < contentTop) {
			contentTop = minY;
			changed = true;
		}
		if (maxX > contentRight) {
			contentRight = maxX;
			changed = true;
		}
		if (maxY > contentBottom) {
			contentBottom = maxY;
			changed = true;
		}
		if (changed && (contentRight - contentLeft + 1 != srcWidth
				|| contentBottom - contentTop + 1 != srcHeight)) {
			// 内容范围变了，重新计算显示区域
			ViewHandler.postEvent(this::updateSize);
		}
	}

	/**
	 * Convert the screen coordinates of the pointer into the virtual ones.
	 *
	 * @param x the pointer coordinate on the real screen
	 * @return the corresponding pointer coordinate on the virtual screen
	 */
	private float convertPointerX(float x) {
		return (x - onX) * srcWidth / onWidth + srcLeft;
	}

	/**
	 * Convert the screen coordinates of the pointer into the virtual ones.
	 *
	 * @param y the pointer coordinate on the real screen
	 * @return the corresponding pointer coordinate on the virtual screen
	 */
	private float convertPointerY(float y) {
		return (y - onY) * srcHeight / onHeight + srcTop;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View getDisplayableView() {
		if (layout == null) {
			layout = (LinearLayout) super.getDisplayableView();
			MicroActivity activity = ContextHolder.getActivity();
			if (graphicsMode == 1) {
				GlesView glesView = new GlesView(activity);
				glesView.setRenderer(renderer);
				glesView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
				renderer.setView(glesView);
				innerView = glesView;
			} else {
				CanvasView canvasView = new CanvasView(this, activity);
				if (graphicsMode == 2) {
					canvasView.setWillNotDraw(false);
				}
				canvasView.getHolder().setFormat(PixelFormat.RGBA_8888);
				innerView = canvasView;
			}
			ViewCallbacks callback = new ViewCallbacks(innerView);
			innerView.getHolder().addCallback(callback);
			innerView.setOnTouchListener(callback);
			innerView.setOnKeyListener(callback);
			innerView.setFocusableInTouchMode(true);
			layout.addView(innerView);
			innerView.requestFocus();
		}
		return layout;
	}

	@Override
	public void clearDisplayableView() {
		super.clearDisplayableView();
		layout = null;
		innerView = null;
	}

	public void setFullScreenMode(boolean flag) {
		if (fullscreen == flag) {
			return;
		}
		fullscreen = flag;
		updateSize();
		softBar.notifyChanged();
		if (!visible) {
			return;
		}
		Display.postEvent(CanvasEvent.getInstance(this, CanvasEvent.SIZE_CHANGED, width, height));
		repaintInternal();
	}

	public boolean hasPointerEvents() {
		return touchInput;
	}

	public boolean hasPointerMotionEvents() {
		return touchInput;
	}

	public boolean hasRepeatEvents() {
		return true;
	}

	public boolean isDoubleBuffered() {
		return true;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	protected abstract void paint(Graphics g);

	public final void repaint() {
		repaint(0, 0, width, height);
	}

	public final void repaint(int x, int y, int width, int height) {
		limitFps();
		boolean post;
		synchronized (paintEvent) {
			post = paintEvent.invalidateClip(this, x, y, x + width, y + height) && !paintEvent.isPending;
			if (post) {
				paintEvent.isPending = true;
			}
		}
		if (post) {
			Display.postEvent(paintEvent);
		}
	}

	private void repaintInternal() {
		synchronized (paintEvent) {
			paintEvent.invalidateClip(this, 0, 0, width, height);
		}
		Display.postEvent(paintEvent);
	}

	// GameCanvas
	public void flushBuffer(Image image, int x, int y, int width, int height) {
		limitFps();
		if (width <= 0 || height <= 0 ||
				x + width < 0 || y + height < 0 ||
				x >= this.width || y >= this.height) {
			return;
		}
		synchronized (bufferLock) {
			offscreenCopy.getSingleGraphics().flush(image, x, y, width, height);
		}
		requestFlushToScreen();
	}

	// ExtendedImage
	public void flushBuffer(Image image, int x, int y) {
		limitFps();
		synchronized (bufferLock) {
			image.copyTo(offscreenCopy, x, y);
		}
		requestFlushToScreen();
	}

	private void limitFps() {
		if (fpsLimit <= 0) return;
		try {
			long millis = (1000 / fpsLimit) - (System.currentTimeMillis() - lastFrameTime);
			if (millis > 0) Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lastFrameTime = System.currentTimeMillis();
	}

	@SuppressLint("NewApi")
	private boolean repaintScreen() {
		Surface surface = this.surface;
		if (surface == null || !surface.isValid()) {
			return true;
		}
		try {
			synchronized (surfaceLock) {
				android.graphics.Canvas canvas = graphicsMode == 3 ?
						surface.lockHardwareCanvas() : surface.lockCanvas(null);
				if (canvas == null) {
					return true;
				}
				CanvasWrapper g = this.canvasWrapper;
				g.bind(canvas);
				g.clear(backgroundColor);
				synchronized (bufferLock) {
					g.drawImage(offscreenCopy, virtualScreen);
				}
				surface.unlockCanvasAndPost(canvas);
			}
			if (fpsCounter != null) {
				fpsCounter.increment();
			}
			if (parallelRedraw) uiHandler.removeMessages(0);
		} catch (Exception e) {
			Log.w(TAG, "repaintScreen: " + e);
		}
		return true;
	}

	/**
	 * After calling this method, an immediate redraw is guaranteed to occur,
	 * and the calling thread is blocked until it is completed.
	 */
	public final void serviceRepaints() {
		Display.getEventQueue().serviceRepaints(paintEvent);
	}

	protected void showNotify() {
	}

	protected void hideNotify() {
	}

	protected void keyPressed(int keyCode) {
	}

	protected void keyRepeated(int keyCode) {
	}

	protected void keyReleased(int keyCode) {
	}

	public void pointerPressed(int pointer, float x, float y) {
		if (Display.isMultiTouchSupported()) {
			Display.setPointerNumber(pointer);
			pointerPressed(Math.round(x), Math.round(y));
			Display.resetPointerNumber();
		} else if (pointer == 0) {
			pointerPressed(Math.round(x), Math.round(y));
		}
	}

	public void pointerDragged(int pointer, float x, float y) {
		if (Display.isMultiTouchSupported()) {
			Display.setPointerNumber(pointer);
			pointerDragged(Math.round(x), Math.round(y));
			Display.resetPointerNumber();
		} else if (pointer == 0) {
			pointerDragged(Math.round(x), Math.round(y));
		}
	}

	public void pointerReleased(int pointer, float x, float y) {
		if (Display.isMultiTouchSupported()) {
			Display.setPointerNumber(pointer);
			pointerReleased(Math.round(x), Math.round(y));
			Display.resetPointerNumber();
		} else if (pointer == 0) {
			pointerReleased(Math.round(x), Math.round(y));
		}
	}

	protected void pointerPressed(int x, int y) {
	}

	protected void pointerDragged(int x, int y) {
	}

	protected void pointerReleased(int x, int y) {
	}

	void setInvisible() {
		this.visible = false;
	}

	public void doKeyPressed(int keyCode) {
		keyPressed(keyCode);
	}

	public void doKeyRepeated(int keyCode) {
		keyRepeated(keyCode);
	}

	public void doKeyReleased(int keyCode) {
		keyReleased(keyCode);
	}

	private class GLRenderer implements GLSurfaceView.Renderer {
		private final FloatBuffer vbo = ByteBuffer.allocateDirect(8 * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		private GLSurfaceView mView;
		private final int[] bgTextureId = new int[1];
		private ShaderProgram program;
		private boolean isStarted;

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			program = new ShaderProgram(shaderFilter);
			int c = Canvas.backgroundColor;
			glClearColor((c >> 16 & 0xff) / 255.0f, (c >> 8 & 0xff) / 255.0f, (c & 0xff) / 255.0f, 1.0f);
			glDisable(GL_BLEND);
			glDisable(GL_DEPTH_TEST);
			glDepthMask(false);
			initTex();
			Bitmap bitmap = offscreenCopy.getBitmap();
			program.loadVbo(vbo, bitmap.getWidth(), bitmap.getHeight());
			if (shaderFilter != null && shaderFilter.values != null) {
				glUniform4fv(program.uSetting, 1, shaderFilter.values, 0);
			}
			isStarted = true;
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			glViewport(0, 0, width, height);
			glUniform2f(program.uPixelDelta, 1.0f / width, 1.0f / height);
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			glClear(GL_COLOR_BUFFER_BIT);
			synchronized (bufferLock) {
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, offscreenCopy.getBitmap(), 0);
			}
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			if (fpsCounter != null) {
				fpsCounter.increment();
			}
		}

		private void initTex() {
			glGenTextures(1, bgTextureId, 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, bgTextureId[0]);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			// юнит текстуры
			glUniform1i(program.uTextureUnit, 0);
		}

		/**
		 * @param u0,v0,u1,v1 纹理坐标范围，用于只取帧缓冲中有内容的那块子矩形
		 */
		public void updateSize(float gl, float gt, float gr, float gb,
							   float u0, float v0, float u1, float v1) {
			synchronized (vbo) {
				FloatBuffer vertex_bg = vbo;
				vertex_bg.rewind();
				vertex_bg.put(gl).put(gt).put(u0).put(v0);// lt
				vertex_bg.put(gl).put(gb).put(u0).put(v1);// lb
				vertex_bg.put(gr).put(gt).put(u1).put(v0);// rt
				vertex_bg.put(gr).put(gb).put(u1).put(v1);// rb
			}
			if (isStarted) {
				mView.queueEvent(() -> {
					Bitmap bitmap = offscreenCopy.getBitmap();
					synchronized (vbo) {
						program.loadVbo(vbo, bitmap.getWidth(), bitmap.getHeight());
					}
				});
			}
		}

		public void requestRender() {
			mView.requestRender();
		}

		public void setView(GLSurfaceView mView) {
			this.mView = mView;
		}

		public void stop() {
			isStarted = false;
			mView.onPause();
		}

		public void start() {
			mView.onResume();
		}

		private Single<Bitmap> takeScreenShot() {
			return Single.<ByteBuffer>create(emitter -> {
						ByteBuffer buf = ByteBuffer.allocateDirect(onWidth * onHeight * 4).order(ByteOrder.nativeOrder());
						mView.requestRender();
						mView.queueEvent(() -> {
							try {
								glReadPixels(displayWidth - onWidth - onX, displayHeight - onHeight - onY, onWidth, onHeight, GL_RGBA, GL_UNSIGNED_BYTE, buf);
								emitter.onSuccess(buf);
							} catch (Throwable e) {
								emitter.onError(e);
							}
						});
					}).timeout(3, TimeUnit.SECONDS)
					.subscribeOn(Schedulers.computation())
					.observeOn(Schedulers.computation())
					.map(bb -> {
						Bitmap rawBitmap = Bitmap.createBitmap(onWidth, onHeight, Bitmap.Config.ARGB_8888);
						bb.rewind();
						rawBitmap.copyPixelsFromBuffer(bb);
						Matrix m = new Matrix();
						m.setScale(1.0f, -1.0f);
						return Bitmap.createBitmap(rawBitmap, 0, 0, onWidth, onHeight, m, false);
					});
		}
	}

	private class PaintEvent extends Event implements EventFilter {
		private int clipLeft;
		private int clipTop;
		private int clipRight;
		private int clipBottom;

		private boolean isPending;

		private int enqueued = 0;

		@Override
		public void process() {
			if (!visible) {
				return;
			}
			int l, t, r, b;
			synchronized (this) {
				isPending = false;
				l = clipLeft;
				t = clipTop;
				r = clipRight;
				b = clipBottom;
				clipLeft = 0;
				clipTop = 0;
				clipRight = 0;
				clipBottom = 0;
			}
			if (r - l <= 0 || b - t <= 0) {
				return;
			}
			Graphics g = offscreen.getSingleGraphics();
			g.reset(l, t, r, b);
			try {
				paint(g);
			} catch (Throwable e) {
				Log.e(TAG, "Error in paint()", e);
			}
			updateContentBounds();
			synchronized (bufferLock) {
				offscreen.copyTo(offscreenCopy);
			}
			if (surface == null || !surface.isValid()) {
				return;
			}
			requestFlushToScreen();
		}

		@Override
		public void recycle() {
		}

		@Override
		public void enterQueue() {
			enqueued++;
		}

		@Override
		public void leaveQueue() {
			enqueued--;
		}

		/**
		 * The queue should contain no more than two repaint events
		 * <p>
		 * One won't be smooth enough, and if you add more than two,
		 * then how to determine exactly how many of them need to be added?
		 */
		@Override
		public boolean placeableAfter(Event event) {
			return event != this;
		}

		@Override
		public boolean accept(Event event) {
			return event == this;
		}

		private boolean invalidateClip(Canvas canvas, int l, int t, int r, int b) {
			boolean empty = clipRight - clipLeft <= 0 && clipBottom - clipTop <= 0;
			if (empty) {
				clipLeft = l;
				clipTop = t;
				clipRight = r;
				clipBottom = b;
			} else {
				if (clipLeft > l) clipLeft = l;
				if (clipTop > t) clipTop = t;
				if (clipRight < r) clipRight = r;
				if (clipBottom < b) clipBottom = b;
			}
			int w = width;
			int h = height;

			if (clipLeft < 0) clipLeft = 0;
			if (clipTop < 0) clipTop = 0;
			if (clipRight > w) clipRight = w;
			if (clipBottom > h) clipBottom = h;

			return empty;
		}
	}

	private class ViewCallbacks implements View.OnTouchListener, SurfaceHolder.Callback, View.OnKeyListener {
		private final View mView;
		OverlayView overlayView;

		public ViewCallbacks(View view) {
			mView = view;
			overlayView = ContextHolder.getActivity().binding.overlayView;
		}

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			switch (event.getAction()) {
				case KeyEvent.ACTION_DOWN:
					return onKeyDown(keyCode, event);
				case KeyEvent.ACTION_UP:
					return onKeyUp(keyCode, event);
				case KeyEvent.ACTION_MULTIPLE:
					if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
						String characters = event.getCharacters();
						for (int i = 0; i < characters.length(); i++) {
							int cp = characters.codePointAt(i);
							postKeyPressed(cp);
							postKeyReleased(cp);
						}
						return true;
					} else {
						return onKeyDown(keyCode, event);
					}
			}
			return false;
		}

		public boolean onKeyDown(int keyCode, KeyEvent event) {
			keyCode = KeyMapper.convertAndroidKeyCode(keyCode, event);
			if (keyCode == 0) {
				return false;
			}
			if (event.getRepeatCount() == 0) {
				if (overlay == null || !overlay.keyPressed(keyCode)) {
					postKeyPressed(keyCode);
				}
			} else {
				if (overlay == null || !overlay.keyRepeated(keyCode)) {
					postKeyRepeated(keyCode);
				}
			}
			return true;
		}

		public boolean onKeyUp(int keyCode, KeyEvent event) {
			int midpKeyCode = KeyMapper.convertAndroidKeyCode(keyCode, event);
			if (midpKeyCode == 0) {
				return false;
			}
			if (overlay == null || !overlay.keyReleased(midpKeyCode)) {
				postKeyReleased(midpKeyCode);
			}
			return true;
		}

		@Override
		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					if (overlay != null) {
						overlay.show();
					}
				case MotionEvent.ACTION_POINTER_DOWN:
					int index = event.getActionIndex();
					int id = event.getPointerId(index);
					float x = event.getX(index);
					float y = event.getY(index);
					if (overlay != null) {
						overlay.pointerPressed(id, x, y);
					}
					if (touchInput && virtualScreen.contains(x, y)) {
						int cX = Math.round(convertPointerX(x));
						int cY = Math.round(convertPointerY(y));
						if (id < 20) {
							lastPointerPos[id][0] = cX;
							lastPointerPos[id][1] = cY;
						}
						Display.postEvent(CanvasEvent.getInstance(Canvas.this,
								CanvasEvent.POINTER_PRESSED,
								id,
								cX,
								cY));
					}
					break;
				case MotionEvent.ACTION_MOVE:
					int pointerCount = event.getPointerCount();
					int historySize = event.getHistorySize();
					for (int h = 0; h < historySize; h++) {
						for (int p = 0; p < pointerCount; p++) {
							id = event.getPointerId(p);
							x = event.getHistoricalX(p, h);
							y = event.getHistoricalY(p, h);
							if (overlay != null) {
								overlay.pointerDragged(id, x, y);
							}
							if (touchInput && virtualScreen.contains(x, y)) {
								int cX = Math.round(convertPointerX(x));
								int cY = Math.round(convertPointerY(y));
								if (id < 20) {
									int oX = lastPointerPos[id][0];
									int oY = lastPointerPos[id][1];
									if (oX == cX && oY == cY) {
										continue;
									}
									lastPointerPos[id][0] = cX;
									lastPointerPos[id][1] = cY;
								}
								Display.postEvent(CanvasEvent.getInstance(Canvas.this,
										CanvasEvent.POINTER_DRAGGED,
										id,
										cX,
										cY));
							}
						}
					}
					for (int p = 0; p < pointerCount; p++) {
						id = event.getPointerId(p);
						x = event.getX(p);
						y = event.getY(p);
						if (overlay != null) {
							overlay.pointerDragged(id, x, y);
						}
						if (touchInput && virtualScreen.contains(x, y)) {
							int cX = Math.round(convertPointerX(x));
							int cY = Math.round(convertPointerY(y));
							if (id < 20) {
								int oX = lastPointerPos[id][0];
								int oY = lastPointerPos[id][1];
								if (oX == cX && oY == cY) {
									continue;
								}
								lastPointerPos[id][0] = cX;
								lastPointerPos[id][1] = cY;
							}
							Display.postEvent(CanvasEvent.getInstance(Canvas.this,
									CanvasEvent.POINTER_DRAGGED,
									id,
									cX,
									cY));
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					if (overlay != null) {
						overlay.hide();
					}
				case MotionEvent.ACTION_POINTER_UP:
					index = event.getActionIndex();
					id = event.getPointerId(index);
					x = event.getX(index);
					y = event.getY(index);
					if (overlay != null) {
						overlay.pointerReleased(id, x, y);
					}
					if (touchInput && virtualScreen.contains(x, y)) {
						int cX = Math.round(convertPointerX(x));
						int cY = Math.round(convertPointerY(y));
						lastPointerPos[id][0] = cX;
						lastPointerPos[id][1] = cY;
						Display.postEvent(CanvasEvent.getInstance(Canvas.this,
								CanvasEvent.POINTER_RELEASED,
								id,
								cX,
								cY));
					}
					break;
				case MotionEvent.ACTION_CANCEL:
					if (overlay != null) {
						overlay.cancel();
					}
					break;
				default:
					return false;
			}
			return true;
		}

		@Override
		public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int newWidth, int newHeight) {
			if (displayWidth > displayHeight) {
				if (newWidth < newHeight) {
					softBar.closeMenu();
				}
			} else if (newWidth > newHeight) {
				softBar.closeMenu();
			}
			displayWidth = newWidth;
			displayHeight = newHeight;
			if (checkSizeChanged() || !sizeChangedCalled) {
				Display.postEvent(CanvasEvent.getInstance(Canvas.this,
						CanvasEvent.SIZE_CHANGED,
						width,
						height));
				repaintInternal();
				sizeChangedCalled = true;
			}
		}

		@Override
		public void surfaceCreated(@NonNull SurfaceHolder holder) {
			if (renderer != null) {
				renderer.start();
			}
			surface = holder.getSurface();
			Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.SHOW_NOTIFY));
			repaintInternal();
			if (showFps) {
				fpsCounter = new FpsCounter(overlayView);
				overlayView.addLayer(fpsCounter);
			}
			overlayView.addLayer(softBar, 0);
			overlayView.setVisibility(true);
			overlay = ContextHolder.getVk();
			if (overlay != null) {
				overlay.setTarget(Canvas.this);
			}
		}

		@Override
		public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
			if (renderer != null) {
				renderer.stop();
			}
			synchronized (surfaceLock) {
				surface = null;
			}
			Display.postEvent(CanvasEvent.getInstance(Canvas.this, CanvasEvent.HIDE_NOTIFY));
			if (fpsCounter != null) {
				fpsCounter.stop();
				overlayView.removeLayer(fpsCounter);
				fpsCounter = null;
			}
			overlayView.removeLayer(softBar);
			softBar.closeMenu();
			overlayView.setVisibility(false);
			if (overlay != null) {
				overlay.setTarget(null);
				overlay.cancel();
				overlay = null;
			}
		}

	}

	private void requestFlushToScreen() {
		if (graphicsMode == 1) {
			if (innerView != null) {
				renderer.requestRender();
			}
		} else if (graphicsMode == 2) {
			if (innerView != null) {
				innerView.postInvalidate();
			}
		} else if (!parallelRedraw) {
			repaintScreen();
		} else if (!uiHandler.hasMessages(0)) {
			uiHandler.sendEmptyMessage(0);
		}
	}

	private class SoftBar extends AbstractSoftKeysBar implements Layer {
		private final OverlayView overlayView;
		private final float padding;
		private final int textColor;
		private final int bgColor;
		private final RectF bounds = new RectF();

		private String leftLabel;
		private String rightLabel;
		private float textScale = 1.0f;

		private SoftBar() {
			super(Canvas.this, false);
			MicroActivity activity = ContextHolder.getActivity();
			this.overlayView = activity.binding.overlayView;
			DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
			padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics);
			textColor = ContextCompat.getColor(activity, R.color.accent);
			bgColor = ContextCompat.getColor(activity, R.color.background);
			notifyChanged();
		}

		private void showPopup() {
			PopupWindow popup = prepareMenu(fullscreen ? 0 : 1);
			popup.setWidth(Math.min(displayWidth, displayHeight) / 2);
			popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			int x = (int) (displayWidth - bounds.right);
			int y = (int) (displayHeight - bounds.top);
			popup.showAtLocation(overlayView, Gravity.RIGHT | Gravity.BOTTOM, x, y);
		}

		@Override
		protected void onCommandsChanged() {
			super.onCommandsChanged();
			if (!fullscreen) {
				int size = commands.size();
				switch (size) {
					case 0:
						break;
					case 1:
						leftLabel = commands.get(0).getAndroidLabel();
						rightLabel = null;
						break;
					case 2:
						leftLabel = commands.get(1).getAndroidLabel();
						rightLabel = commands.get(0).getAndroidLabel();
						break;
					default:
						leftLabel = overlayView.getResources().getString(R.string.cmd_menu);
						rightLabel = commands.get(0).getAndroidLabel();
				}
			}
			overlayView.postInvalidate();
		}

		private boolean fireLeftSoft() {
			int size = commands.size();
			if (size == 0) {
				return false;
			}
			if (fullscreen) {
				if (listener != null) {
					showPopup();
					return true;
				}
				return false;
			}
			if (size > 2) {
				showPopup();
				return true;
			}
			if (listener != null) {
				fireCommandAction(commands.get(size > 1 ? 1 : 0));
			}
			return true;
		}

		private boolean fireRightSoft() {
			int size = commands.size();
			if (size == 0) {
				return false;
			}
			if (fullscreen) {
				if (size == 1) {
					return false;
				}
				if (listener != null) {
					showPopup();
					return true;
				}
				return false;
			}
			if (listener != null) {
				fireCommandAction(commands.get(0));
			}
			return true;
		}

		@Override
		public void paint(CanvasWrapper g) {
			if (bounds.isEmpty() || commands.size() == 0) {
				return;
			}
			g.setFillColor(bgColor);
			g.fillRect(bounds);

			if (leftLabel == null) {
				return;
			}
			g.setTextAlign(Paint.Align.LEFT);
			g.setTextScale(textScale);
			g.setTextColor(textColor);
			float y = bounds.centerY();
			g.drawString(leftLabel, bounds.left + padding * textScale, y);
			if (rightLabel != null) {
				g.setTextAlign(Paint.Align.RIGHT);
				g.drawString(rightLabel, bounds.right - padding * textScale, y);
			}

			g.setTextAlign(Paint.Align.CENTER);
			g.setTextScale(1.0f);
		}

		public void resize() {
			float left;
			float right;
			float bottom;
			VirtualKeyboard vk = ContextHolder.getVk();
			if (vk != null && vk.isPhone()) {
				float vkTop = displayHeight - vk.getPhoneKeyboardHeight(displayWidth, displayHeight) - 1;
				if (onWidth < displayWidth / 2.0f || onWidth > displayWidth) {
					textScale = 1.0f;
					left = 0;
					right = displayWidth;
					bottom = vkTop;
				} else {
					textScale = (float) onWidth / displayWidth;
					canvasWrapper.setTextScale(textScale);
					left = onX;
					right = onX + onWidth;
					bottom = onY + onHeight;
					if (bottom > vkTop) {
						bottom = vkTop;
					}
				}
			} else {
				float width = onWidth;
				float minSide = Math.min(displayWidth, displayHeight);
				if (width <= minSide) {
					textScale = width / minSide;
					canvasWrapper.setTextScale(textScale);
					left = onX;
				} else {
					left = (onX + onWidth) / 2.0f - minSide / 2.0f;
					if (left + minSide > displayWidth) {
						left = displayWidth / 2.0f - minSide / 2.0f;
					}
					width = minSide;
				}
				bottom = onY + onHeight;
				right = left + width;
				if (bottom > displayHeight) {
					bottom = displayHeight;
				}
			}
			float top = fullscreen ? bottom : bottom - canvasWrapper.getTextHeight();
			bounds.set(left, top, right, bottom);
			canvasWrapper.setTextScale(1.0f);
		}
	}
}
