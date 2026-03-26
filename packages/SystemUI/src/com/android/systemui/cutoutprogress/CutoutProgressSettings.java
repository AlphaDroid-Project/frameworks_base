/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * Copyright (C) 2026 AlphaDroid
 */

package com.android.systemui.cutoutprogress;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class CutoutProgressSettings {

    public static final String KEY_DOWNLOAD_ENABLED = "cutout_progress_download_enabled";
    public static final String KEY_CHARGING_RING_ENABLED = "cutout_progress_charging_ring_enabled";
    public static final String KEY_CHARGING_PULSE_ENABLED = "cutout_progress_charging_pulse_enabled";

    public static final String KEY_RING_COLOR_MODE = "cutout_progress_ring_color_mode";
    public static final String KEY_RING_COLOR = "cutout_progress_ring_color";
    public static final String KEY_STROKE_WIDTH = "cutout_progress_stroke_width_dp10";
    public static final String KEY_BG_RING_ENABLED = "cutout_progress_bg_ring_enabled";
    public static final String KEY_RING_OPACITY = "cutout_progress_ring_opacity";
    public static final String KEY_DIALOG_BG_OPACITY = "cutout_progress_dialog_bg_opacity";

    public static final String KEY_ISLAND_COMPACT_MODE = "cutout_progress_island_compact";
    public static final String KEY_ISLAND_POSITION = "cutout_progress_island_position";
    public static final String KEY_ISLAND_TIMEOUT = "cutout_progress_island_timeout";

    public static final String KEY_PATH_MODE = "cutout_progress_path_mode";
    public static final String KEY_RING_GAP_X1000 = "cutout_progress_ring_gap_x1000";
    public static final String KEY_RING_SCALE_X_X1000 = "cutout_progress_ring_scale_x_x1000";
    public static final String KEY_RING_SCALE_Y_X1000 = "cutout_progress_ring_scale_y_x1000";
    public static final String KEY_RING_OFFSET_X_DP10 = "cutout_progress_ring_offset_x_dp10";
    public static final String KEY_RING_OFFSET_Y_DP10 = "cutout_progress_ring_offset_y_dp10";

    public static final int RING_COLOR_MODE_ACCENT = 0;
    public static final int RING_COLOR_MODE_RAINBOW = 1;
    public static final int RING_COLOR_MODE_CUSTOM = 2;

    private static final float DEF_RING_GAP = 1.155f;
    private static final float DEF_RING_SCALE = 1.0f;
    private static final float DEF_RING_OFFSET_DP = 0.0f;
    private static final boolean DEF_PATH_MODE = false;

    private static final String[] KEYS = {
            KEY_DOWNLOAD_ENABLED,
            KEY_CHARGING_RING_ENABLED,
            KEY_CHARGING_PULSE_ENABLED,
            KEY_RING_COLOR_MODE,
            KEY_RING_COLOR,
            KEY_STROKE_WIDTH,
            KEY_BG_RING_ENABLED,
            KEY_RING_OPACITY,
            KEY_DIALOG_BG_OPACITY,
            KEY_ISLAND_COMPACT_MODE,
            KEY_ISLAND_POSITION,
            KEY_ISLAND_TIMEOUT,
            KEY_PATH_MODE,
            KEY_RING_GAP_X1000,
            KEY_RING_SCALE_X_X1000,
            KEY_RING_SCALE_Y_X1000,
            KEY_RING_OFFSET_X_DP10,
            KEY_RING_OFFSET_Y_DP10
    };

    private final ContentResolver mCr;
    private final Handler mHandler;
    private Runnable mListener;

    private final ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mListener != null) {
                mHandler.post(mListener);
            }
        }
    };

    public CutoutProgressSettings(ContentResolver cr, Handler handler) {
        mCr = cr;
        mHandler = handler;
    }

    public void observe(Runnable listener) {
        mListener = listener;
        for (String key : KEYS) {
            mCr.registerContentObserver(Settings.System.getUriFor(key), false, mObserver);
        }
    }

    public void stopObserving() {
        mCr.unregisterContentObserver(mObserver);
        mListener = null;
    }

    public boolean isEnabled() {
        return isChargingRingEnabled() || isDownloadRingEnabled();
    }

    public boolean isDownloadRingEnabled() {
        return getInt(KEY_DOWNLOAD_ENABLED, 1) != 0;
    }

    public boolean isChargingRingEnabled() {
        return getInt(KEY_CHARGING_RING_ENABLED, 1) != 0;
    }

    public boolean isChargingPulseEnabled() {
        return getInt(KEY_CHARGING_PULSE_ENABLED, 1) != 0;
    }

    public int getRingColorMode() {
        return clamp(getInt(KEY_RING_COLOR_MODE, RING_COLOR_MODE_ACCENT),
                RING_COLOR_MODE_ACCENT, RING_COLOR_MODE_CUSTOM);
    }

    public int getRingColor() {
        return getInt(KEY_RING_COLOR, 0xFF2196F3);
    }

    public float getStrokeWidthDp() {
        return getInt(KEY_STROKE_WIDTH, 20) / 10f;
    }

    public boolean isBgRingEnabled() {
        return getInt(KEY_BG_RING_ENABLED, 1) != 0;
    }

    public int getRingOpacityPercent() {
        return clamp(getInt(KEY_RING_OPACITY, 90), 0, 100);
    }

    public int getDialogBgOpacityPercent() {
        return clamp(getInt(KEY_DIALOG_BG_OPACITY, 70), 0, 100);
    }

    public boolean isIslandCompactMode() {
        return getInt(KEY_ISLAND_COMPACT_MODE, 1) != 0;
    }

    public int getIslandPosition() {
        return clamp(getInt(KEY_ISLAND_POSITION, 0), 0, 2);
    }

    public int getIslandTimeoutMs() {
        return getInt(KEY_ISLAND_TIMEOUT, 5) * 1000;
    }

    public boolean isPathMode() {
        return getInt(KEY_PATH_MODE, DEF_PATH_MODE ? 1 : 0) != 0;
    }

    public float getRingGap() {
        return getInt(KEY_RING_GAP_X1000, (int) (DEF_RING_GAP * 1000f)) / 1000f;
    }

    public float getRingScaleX() {
        return getInt(KEY_RING_SCALE_X_X1000, (int) (DEF_RING_SCALE * 1000f)) / 1000f;
    }

    public float getRingScaleY() {
        return getInt(KEY_RING_SCALE_Y_X1000, (int) (DEF_RING_SCALE * 1000f)) / 1000f;
    }

    public float getRingOffsetXDp() {
        return getInt(KEY_RING_OFFSET_X_DP10, (int) (DEF_RING_OFFSET_DP * 10f)) / 10f;
    }

    public float getRingOffsetYDp() {
        return getInt(KEY_RING_OFFSET_Y_DP10, (int) (DEF_RING_OFFSET_DP * 10f)) / 10f;
    }

    private int getInt(String key, int def) {
        return Settings.System.getInt(mCr, key, def);
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}