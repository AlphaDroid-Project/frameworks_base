/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * Copyright (C) 2026 AlphaDroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.cutoutprogress;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.view.WindowManager;

import com.android.internal.app.IBatteryStats;
import com.android.settingslib.fuelgauge.BatteryStatus;
import com.android.systemui.CoreStartable;
import com.android.systemui.cutoutprogress.ring.CutoutRingView;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.statusbar.notification.collection.NotifPipeline;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener;

import java.util.Locale;

import javax.inject.Inject;

@SysUISingleton
public class CutoutProgressController implements CoreStartable {

    private static final int WINDOW_TYPE = 2024;

    private final Context mContext;
    private final NotifPipeline mPipeline;
    private final Handler mMainHandler;
    private final IBatteryStats mBatteryStats;

    private final CutoutProgressSettings mSettings;
    private final DownloadStateTracker mTracker;
    private CutoutRingView mRingView;

    private boolean mOverlayAttached = false;
    private boolean mListenerRegistered = false;
    private boolean mBatteryReceiverRegistered = false;

    private int mCurrentDivider = 1000;
    private boolean mHasDashCharger = false;
    private boolean mHasWarpCharger = false;
    private boolean mHasVoocCharger = false;

    private final NotifCollectionListener mNotifListener = new NotifCollectionListener() {
        @Override
        public void onEntryAdded(NotificationEntry entry) {
            if (!mSettings.isEnabled()) return;
            mTracker.onNotificationChanged(entry);
        }

        @Override
        public void onEntryUpdated(NotificationEntry entry) {
            if (!mSettings.isEnabled()) return;
            mTracker.onNotificationChanged(entry);
        }

        @Override
        public void onEntryRemoved(NotificationEntry entry, int reason) {
            if (!mSettings.isEnabled()) return;
            mTracker.onNotificationRemoved(entry, reason);
        }
    };

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mSettings.isEnabled() || !mSettings.isChargingRingEnabled()) {
                mMainHandler.post(() -> mRingView.setChargingState(false, 0, null, null, null, null));
                return;
            }

            BatteryStatus bs = new BatteryStatus(intent);

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int pct = scale > 0 ? level * 100 / scale : 0;

            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

            float chargingCurrent = bs.maxChargingCurrent;
            float chargingVoltage = bs.maxChargingVoltage;
            float chargingWattage = bs.maxChargingWattage;

            if (chargingCurrent <= 0) {
                BatteryManager bm = context.getSystemService(BatteryManager.class);
                if (bm != null) {
                    int currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    if (currentNow != 0) chargingCurrent = Math.abs(currentNow);
                }
            }

            if (chargingWattage <= 0 && chargingCurrent > 0 && chargingVoltage > 0) {
                chargingWattage = (chargingCurrent / 1000f) * (chargingVoltage / 1000f);
            }

            String statsLine1 = "";
            if (chargingCurrent >= mCurrentDivider * 1000) {
                statsLine1 += String.format(Locale.US, "%.1fA", (chargingCurrent / mCurrentDivider / 1000f));
            } else if (chargingCurrent > 0) {
                statsLine1 += String.format(Locale.US, "%.0fmA", (chargingCurrent / mCurrentDivider));
            }

            if (chargingWattage > 0) {
                statsLine1 += (statsLine1.isEmpty() ? "" : " • ")
                        + String.format(Locale.US, "%.1fW", (chargingWattage / mCurrentDivider / 1000f));
            }

            if (chargingVoltage > 0) {
                statsLine1 += (statsLine1.isEmpty() ? "" : " • ")
                        + String.format(Locale.US, "%.1fV", (chargingVoltage / 1000f / 1000f));
            }

            String statsLine2 = temp > 0
                    ? String.format(Locale.US, "%.1f°C", (temp / 10f))
                    : "";

            String chargeMode = "Charging";
            int chargingSpeed = bs.getChargingSpeed(mContext);
            int chargingStatus = bs.chargingStatus;

            if (chargingStatus == BatteryManager.BATTERY_STATUS_FULL) {
                chargeMode = "Fully Charged";
            } else if (chargingSpeed == BatteryStatus.CHARGING_OEM) {
                if (mHasVoocCharger) chargeMode = "VOOC Charging";
                else if (mHasWarpCharger) chargeMode = "Warp Charging";
                else if (mHasDashCharger) chargeMode = "Dash Charging";
                else chargeMode = "Charging rapidly";
            } else if (chargingSpeed == BatteryStatus.CHARGING_FAST) {
                chargeMode = "Charging rapidly";
            } else if (chargingSpeed == BatteryStatus.CHARGING_SLOWLY) {
                chargeMode = "Charging slowly";
            }

            String timeRemainingStr = "";
            try {
                if (mBatteryStats != null && charging && chargingStatus != BatteryManager.BATTERY_STATUS_FULL) {
                    long timeMs = mBatteryStats.computeChargeTimeRemaining();
                    if (timeMs > 0) {
                        timeRemainingStr = Formatter.formatShortElapsedTimeRoundingUpToMinutes(mContext, timeMs) + " remaining";
                    }
                }
            } catch (RemoteException ignored) {}

            boolean pulseEnabled = mSettings.isChargingPulseEnabled();
            String finalMode = chargeMode;
            String finalTime = timeRemainingStr;
            String finalStats1 = statsLine1;
            String finalStats2 = statsLine2;

            mMainHandler.post(() -> {
                mRingView.setChargingPulseEnabled(pulseEnabled);
                mRingView.setChargingState(charging, pct, finalMode, finalTime, finalStats1, finalStats2);
            });
        }
    };

    @Inject
    public CutoutProgressController(
            Context context,
            NotifPipeline notifPipeline,
            IBatteryStats batteryStats,
            @Main Handler mainHandler) {
        mContext = context;
        mPipeline = notifPipeline;
        mBatteryStats = batteryStats;
        mMainHandler = mainHandler;
        mSettings = new CutoutProgressSettings(context.getContentResolver(), mainHandler);
        mTracker = new DownloadStateTracker();

        try {
            mHasDashCharger = mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasDashCharger);
            mHasWarpCharger = mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasWarpCharger);
            mHasVoocCharger = mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasVoocCharger);
        } catch (Exception ignored) {}

        try {
            int resId = mContext.getResources().getIdentifier("config_currentInfoDivider", "integer", mContext.getPackageName());
            if (resId == 0) resId = mContext.getResources().getIdentifier("config_currentInfoDivider", "integer", "android");
            if (resId != 0) mCurrentDivider = mContext.getResources().getInteger(resId);
        } catch (Exception ignored) {}
    }

    @Override
    public void start() {
        mRingView = new CutoutRingView(mContext);
        mRingView.applySettings(mSettings);
        bindTrackerToView();
        mSettings.observe(this::onSettingsChanged);
        onSettingsChanged();
    }

    private void onSettingsChanged() {
        mRingView.applySettings(mSettings);
        if (mSettings.isEnabled()) {
            enableFeature();
        } else {
            disableFeature();
        }
    }

    private void enableFeature() {
        attachOverlay();
        registerPipelineListener();
        registerBatteryReceiver();
    }

    private void disableFeature() {
        mTracker.reset();
        if (mListenerRegistered) {
            mPipeline.removeCollectionListener(mNotifListener);
            mListenerRegistered = false;
        }
        detachOverlay();
        unregisterBatteryReceiver();
    }

    private void attachOverlay() {
        if (mOverlayAttached) return;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setTitle("CutoutProgressOverlay");

        WindowManager wm = mContext.getSystemService(WindowManager.class);
        if (wm != null) {
            wm.addView(mRingView, params);
            mOverlayAttached = true;
        }
    }

    private void detachOverlay() {
        if (!mOverlayAttached) return;
        WindowManager wm = mContext.getSystemService(WindowManager.class);
        if (wm != null) {
            wm.removeView(mRingView);
            mOverlayAttached = false;
        }
    }

    private void registerPipelineListener() {
        if (mListenerRegistered) return;
        mListenerRegistered = true;
        mPipeline.addCollectionListener(mNotifListener);
    }

    private void registerBatteryReceiver() {
        if (mBatteryReceiverRegistered) return;
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBatteryReceiver, filter);
        mBatteryReceiverRegistered = true;
    }

    private void unregisterBatteryReceiver() {
        if (!mBatteryReceiverRegistered) return;
        mContext.unregisterReceiver(mBatteryReceiver);
        mBatteryReceiverRegistered = false;
        mMainHandler.post(() -> mRingView.setChargingState(false, 0, null, null, null, null));
    }

    private void bindTrackerToView() {
        mTracker.setOnProgress(progress -> mMainHandler.post(() -> mRingView.setProgress(progress)));
        mTracker.setOnComplete(() -> mMainHandler.post(() -> mRingView.setProgress(100)));
        mTracker.setOnError(() -> mMainHandler.post(() -> mRingView.showError()));
        mTracker.setOnLabelChanged(label -> mMainHandler.post(() -> mRingView.setFilenameHint(label)));
    }
}