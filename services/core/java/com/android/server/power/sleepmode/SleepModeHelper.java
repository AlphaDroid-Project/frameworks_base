/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 AlphaDroid
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

package com.android.server.power.sleepmode;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorPrivacyManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;

import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_OFF;

public class SleepModeHelper {

    private AudioManager mAudioManager;
    private NotificationManager mNotificationManager;
    private WifiManager mWifiManager;
    private SensorPrivacyManager mSensorPrivacyManager;
    private BluetoothAdapter mBluetoothAdapter;
    private int mSubscriptionId;
    private Toast mToast;
    private boolean mSleepModeEnabled;
    private boolean mWifiState;
    private boolean mCellularState;
    private boolean mBluetoothState;
    private int mRingerState;
    private int mZenState;
    private Context mContext;

    private static final String TAG = "SleepModeHelper";
    private static final int SLEEP_NOTIFICATION_ID = 727;
    public static final String SLEEP_MODE_TURN_OFF = "android.intent.action.SLEEP_MODE_TURN_OFF";

    private final Handler mHandler;

    public SleepModeHelper(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mSensorPrivacyManager = (SensorPrivacyManager) mContext.getSystemService(Context.SENSOR_PRIVACY_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        boolean enabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        mHandler = new Handler(Looper.getMainLooper());
        setSleepModeState(enabled);
    }

    private TelephonyManager getTelephonyManager() {
        int subscriptionId = mSubscriptionId;
        // If mSubscriptionId is invalid, get default data sub.
        if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        // If data sub is also invalid, get any active sub.
        if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            int[] activeSubIds = SubscriptionManager.from(mContext).getActiveSubscriptionIdList();
            if (!ArrayUtils.isEmpty(activeSubIds)) {
                subscriptionId = activeSubIds[0];
            }
        }
        return mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subscriptionId);
    }

    private boolean isWifiEnabled() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }
        try {
            return mWifiManager.isWifiEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private void setWifiEnabled(boolean enable) {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }
        try {
            mWifiManager.setWifiEnabled(enable);
        } catch (Exception e) {
        }
    }

    private boolean isBluetoothEnabled() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        try {
            return mBluetoothAdapter.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private void setBluetoothEnabled(boolean enable) {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        try {
            if (enable) mBluetoothAdapter.enable();
            else mBluetoothAdapter.disable();
        } catch (Exception e) {
        }
    }

    private boolean isSensorEnabled() {
        if (mSensorPrivacyManager == null) {
            mSensorPrivacyManager = (SensorPrivacyManager) mContext.getSystemService(Context.SENSOR_PRIVACY_SERVICE);
        }
        try {
            return !mSensorPrivacyManager.isAllSensorPrivacyEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private void setSensorEnabled(boolean enable) {
        if (mSensorPrivacyManager == null) {
            mSensorPrivacyManager = (SensorPrivacyManager) mContext.getSystemService(Context.SENSOR_PRIVACY_SERVICE);
        }
        try {
            mSensorPrivacyManager.setAllSensorPrivacy(!enable);
        } catch (Exception e) {
        }
    }

    private int getZenMode() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        try {
            return mNotificationManager.getZenMode();
        } catch (Exception e) {
            return -1;
        }
    }

    private void setZenMode(int mode) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        try {
            mNotificationManager.setZenMode(mode, null, TAG);
        } catch (Exception e) {
        }
    }

    private int getRingerModeInternal() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        try {
            return mAudioManager.getRingerModeInternal();
        } catch (Exception e) {
            return -1;
        }
    }

    private void setRingerModeInternal(int mode) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        try {
            mAudioManager.setRingerModeInternal(mode);
        } catch (Exception e) {
        }
    }

    private void enable() {
        if (!ActivityManager.isSystemReady()) return;
        // Disable Wi-Fi
        final boolean disableWifi = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_WIFI_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableWifi) {
            mWifiState = isWifiEnabled();
            setWifiEnabled(false);
        }
        // Disable Bluetooth
        final boolean disableBluetooth = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_BLUETOOTH_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableBluetooth) {
            mBluetoothState = isBluetoothEnabled();
            setBluetoothEnabled(false);
        }
        // Disable Mobile Data
        final boolean disableData = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_CELLULAR_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableData) {
            mCellularState = getTelephonyManager().isDataEnabled();
            getTelephonyManager().setDataEnabled(false);
        }
        // Disable Sensors
        final boolean disableSensors = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_SENSORS_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableSensors) {
            setSensorEnabled(false);
        }
        // Set Ringer mode (0: Off, 1: Vibrate, 2:DND: 3:Silent)
        final int ringerMode = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_RINGER_MODE, 0, UserHandle.USER_CURRENT);
        if (ringerMode != 0) {
            mRingerState = getRingerModeInternal();
            mZenState = getZenMode();
            if (ringerMode == 1) {
                setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                setZenMode(ZEN_MODE_OFF);
            } else if (ringerMode == 2) {
                setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS);
            } else if (ringerMode == 3) {
                setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
                setZenMode(ZEN_MODE_OFF);
            }
        }
        showToast(mContext.getResources().getString(R.string.sleep_mode_enabled_toast), Toast.LENGTH_LONG);
        addNotification();
    }

    private void disable() {
        if (!ActivityManager.isSystemReady()) return;
        // Enable Wi-Fi
        final boolean disableWifi = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_WIFI_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableWifi && mWifiState != isWifiEnabled()) {
            setWifiEnabled(mWifiState);
        }
        // Enable Bluetooth
        final boolean disableBluetooth = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_BLUETOOTH_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableBluetooth && mBluetoothState != isBluetoothEnabled()) {
            setBluetoothEnabled(mBluetoothState);
        }
        // Enable Mobile Data
        final boolean disableData = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_CELLULAR_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableData && mCellularState != getTelephonyManager().isDataEnabled()) {
            getTelephonyManager().setDataEnabled(mCellularState);
        }
        // Enable Sensors
        final boolean disableSensors = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_SENSORS_TOGGLE, 1, UserHandle.USER_CURRENT) == 1;
        if (disableSensors) {
            setSensorEnabled(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isSensorEnabled()) {
                        setSensorEnabled(true);
                    }
                }
            }, 1000);
        }
        // Set Ringer mode (0: Off, 1: Vibrate, 2:DND: 3:Silent)
        final int ringerMode = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_RINGER_MODE, 0, UserHandle.USER_CURRENT);
        if (ringerMode != 0 && (mRingerState != getRingerModeInternal() ||
                mZenState != getZenMode())) {
            setRingerModeInternal(mRingerState);
            setZenMode(mZenState);
        }
        showToast(mContext.getResources().getString(R.string.sleep_mode_disabled_toast), Toast.LENGTH_LONG);
        mNotificationManager.cancel(SLEEP_NOTIFICATION_ID);
    }

     private void addNotification() {
        Intent intent = new Intent(SLEEP_MODE_TURN_OFF);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Display a notification
        Notification.Builder builder = new Notification.Builder(mContext, SystemNotificationChannels.SLEEP)
            .setTicker(mContext.getResources().getString(R.string.sleep_mode_notification_title))
            .setContentTitle(mContext.getResources().getString(R.string.sleep_mode_notification_title))
            .setContentText(mContext.getResources().getString(R.string.sleep_mode_notification_content))
            .setSmallIcon(R.drawable.ic_sleep)
            .setWhen(java.lang.System.currentTimeMillis())
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false);
        Notification notification = builder.build();
        mNotificationManager.notify(SLEEP_NOTIFICATION_ID, notification);
    }

    private void showToast(String msg, int duration) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mToast != null) mToast.cancel();
                    mToast = Toast.makeText(mContext, msg, duration);
                    mToast.show();
                } catch (Exception e) {
                }
            }
        });
    }

    private void setSleepModeState(boolean enable) {
        if (enable) {
            enable();
        } else {
            disable();
        }
    }

    public void setSleepModeState(Context context, boolean enable) {
        mContext = context;
        setSleepModeState(enable);
    }
}
