/*
 * Copyright (C) 2017 The OmniROM project
 * Copyright (C) 2022-2026 crDroid Android project
 * Copyright (C) 2026 AxionOS Project
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

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.alpha.tiles.dialog.WeatherDialogManager;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.keyguard.ui.clocks.ClockData;
import com.android.systemui.plugins.keyguard.ui.clocks.ClockWeatherData;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.quicklook.QuickLookClient;
import com.android.systemui.res.R;
import com.android.systemui.shared.clocks.WeatherUtils;

import javax.inject.Inject;

/**
 * Quick Settings weather tile. Data comes from the AxQuickLook privileged service via {@link
 * QuickLookClient} (same pipeline as the Axion lockscreen clock), including OmniJaws vs Google
 * weather resolution inside AxQuickLook.
 */
public class WeatherTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "weather";
    private static final String AX_QUICK_LOOK_PACKAGE = "com.android.axion.quicklook";
    private static final String AX_QUICK_LOOK_SETTINGS_ACTION =
            "com.android.axion.quicklook.SETTINGS";

    private static final String TAG = "WeatherTile";
    private static final boolean DEBUG = false;

    private static final int WEATHER_ICON_DP = 32;

    private final ActivityStarter mActivityStarter;
    private final QuickLookClient mQuickLookClient;
    private final WeatherDialogManager mWeatherDialogManager;
    private final Handler mMainHandler;

    /** Non-null only when AxQuickLook reports usable weather. */
    @Nullable private ClockWeatherData mWeather;

    private final QuickLookClient.Callback mQuickLookCallback =
            new QuickLookClient.Callback() {
                @Override
                public void onClockDataChanged(ClockData data) {
                    ClockWeatherData w = data.getWeather();
                    mWeather = isMeaningfulWeather(w) ? w : null;
                    refreshState();
                }
            };

    @Inject
    public WeatherTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            QuickLookClient quickLookClient,
            WeatherDialogManager weatherDialogManager) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mActivityStarter = activityStarter;
        mQuickLookClient = quickLookClient;
        mWeatherDialogManager = weatherDialogManager;
        mMainHandler = mainHandler;
    }

    private static boolean isMeaningfulWeather(@Nullable ClockWeatherData w) {
        if (w == null) return false;
        boolean hasTemp = w.getTemp() != null && !w.getTemp().isEmpty();
        boolean hasCondition = w.getCondition() != null && !w.getCondition().isEmpty();
        return hasTemp || hasCondition || w.getConditionCode() != 0;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ALPHA;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (DEBUG) Log.d(TAG, "setListening " + listening);
        if (listening) {
            mQuickLookClient.addCallback(mQuickLookCallback);
        } else {
            mQuickLookClient.removeCallback(mQuickLookCallback);
        }
    }

    @Override
    protected void handleDestroy() {
        mQuickLookClient.removeCallback(mQuickLookCallback);
        super.handleDestroy();
    }

    @Override
    public boolean isAvailable() {
        try {
            mContext.getPackageManager().getPackageInfo(AX_QUICK_LOOK_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        // handleClick runs on the background looper; dialog must be shown on the main thread.
        mMainHandler.post(() -> mWeatherDialogManager.create(expandable));
    }

    @Override
    public Intent getLongClickIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
                "com.android.settings.Settings$LockScreenSettingsActivity");
        return intent;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean hasWeather = mWeather != null;
        state.value = hasWeather;
        state.state = hasWeather ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.icon = ResourceIcon.get(R.drawable.ic_qs_weather);
        state.label = mContext.getResources().getString(R.string.omnijaws_label_default);
        state.secondaryLabel = mContext.getResources().getString(R.string.omnijaws_service_unknown);

        if (!hasWeather || mWeather == null) {
            return;
        }

        int iconPx = (int) (mContext.getResources().getDisplayMetrics().density * WEATHER_ICON_DP + 0.5f);
        Bitmap bmp = WeatherUtils.INSTANCE.resolveWeatherBitmap(mContext, mWeather, iconPx);
        if (bmp != null) {
            Drawable d = new BitmapDrawable(mContext.getResources(), bmp);
            state.icon = new DrawableIcon(d);
        }

        String city = mWeather.getCity();
        if (city != null && !city.isEmpty()) {
            state.label = city;
        }

        String tempPart = mWeather.getFormattedTemp();
        String cond = localizeCondition(mWeather.getCondition());
        state.secondaryLabel = tempPart + " · " + cond;
    }

    /**
     * Maps English-ish OmniJaws/Google condition phrases to localized summaries where we have
     * matching cr_strings (legacy OmniJaws tile behavior).
     */
    private String localizeCondition(String condition) {
        if (condition == null || condition.isEmpty()) {
            return "";
        }
        String lower = condition.toLowerCase();
        if (lower.contains("cloud")) {
            return mContext.getResources().getString(R.string.weather_condition_clouds);
        } else if (lower.contains("rain")) {
            return mContext.getResources().getString(R.string.weather_condition_rain);
        } else if (lower.contains("clear")) {
            return mContext.getResources().getString(R.string.weather_condition_clear);
        } else if (lower.contains("storm")) {
            return mContext.getResources().getString(R.string.weather_condition_storm);
        } else if (lower.contains("snow")) {
            return mContext.getResources().getString(R.string.weather_condition_snow);
        } else if (lower.contains("wind")) {
            return mContext.getResources().getString(R.string.weather_condition_wind);
        } else if (lower.contains("mist")) {
            return mContext.getResources().getString(R.string.weather_condition_mist);
        }
        return condition;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getResources().getString(R.string.omnijaws_label_default);
    }
}
