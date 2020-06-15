/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.server.lineage.display;

import static com.android.server.lineage.display.LiveDisplayService.ALL_CHANGED;
import static com.android.server.lineage.display.LiveDisplayService.DISPLAY_CHANGED;
import static com.android.server.lineage.display.LiveDisplayService.MODE_CHANGED;
import static com.android.server.lineage.display.LiveDisplayService.TWILIGHT_CHANGED;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.os.Handler;
import android.util.Log;

import com.android.server.lineage.LineageBaseFeature;
import com.android.server.lineage.display.LiveDisplayService.State;
import com.android.server.lineage.display.TwilightTracker.TwilightState;

import java.util.BitSet;

public abstract class LiveDisplayFeature extends LineageBaseFeature {

    protected static final String TAG = "LiveDisplay";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected final boolean mNightDisplayAvailable;

    private State mState;

    public LiveDisplayFeature(Context context, Handler handler) {
        super(context, handler);
        mNightDisplayAvailable = ColorDisplayManager.isNightDisplayAvailable(mContext);
    }

    public abstract boolean getCapabilities(final BitSet caps);

    protected abstract void onUpdate();

    void update(final int flags, final State state) {
        mState = state;
        if ((flags & DISPLAY_CHANGED) != 0) {
            onScreenStateChanged();
        }
        if (((flags & TWILIGHT_CHANGED) != 0) && mState.mTwilight != null) {
            onTwilightUpdated();
        }
        if ((flags & MODE_CHANGED) != 0) {
            onUpdate();
        }
        if (flags == ALL_CHANGED) {
            onSettingsChanged(null);
        }
    }

    protected void onScreenStateChanged() { }

    protected void onTwilightUpdated() { }

    protected final boolean isLowPowerMode() {
        return mState.mLowPowerMode;
    }

    protected final int getMode() {
        return mState.mMode;
    }

    protected final boolean isScreenOn() {
        return mState.mScreenOn;
    }

    protected final TwilightState getTwilight() {
        return mState.mTwilight;
    }

    public final boolean isNight() {
        return mState.mTwilight != null && mState.mTwilight.isNight();
    }
}
