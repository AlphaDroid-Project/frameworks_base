/*
 * Copyright (C) 2019 The Android Open Source Project
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2017-2023 The LineageOS Project
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

package android.provider.settings.backup;

import android.compat.annotation.UnsupportedAppUsage;
import android.provider.Settings;

import com.android.server.display.feature.flags.Flags;

import java.util.ArrayList;
import java.util.List;

/** Information about the system settings to back up */
public class SystemSettings {

    /**
     * Settings to back up.
     *
     * NOTE: Settings are backed up and restored in the order they appear
     *       in this array. If you have one setting depending on another,
     *       make sure that they are ordered appropriately.
     */
    @UnsupportedAppUsage
    public static final String[] SETTINGS_TO_BACKUP = getSettingsToBackUp();

    private static String[] getSettingsToBackUp() {
        List<String> settings = new ArrayList<>(List.of(
                Settings.System.STAY_ON_WHILE_PLUGGED_IN,   // moved to global
                Settings.System.WIFI_USE_STATIC_IP,
                Settings.System.WIFI_STATIC_IP,
                Settings.System.WIFI_STATIC_GATEWAY,
                Settings.System.WIFI_STATIC_NETMASK,
                Settings.System.WIFI_STATIC_DNS1,
                Settings.System.WIFI_STATIC_DNS2,
                Settings.System.BLUETOOTH_DISCOVERABILITY,
                Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT,
                Settings.System.FONT_SCALE,
                Settings.System.DIM_SCREEN,
                Settings.System.SCREEN_OFF_TIMEOUT,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.ADAPTIVE_SLEEP,             // moved to secure
                Settings.System.APPLY_RAMPING_RINGER,
                Settings.System.VIBRATE_INPUT_DEVICES,
                Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                Settings.System.TEXT_AUTO_REPLACE,
                Settings.System.TEXT_AUTO_CAPS,
                Settings.System.TEXT_AUTO_PUNCTUATE,
                Settings.System.TEXT_SHOW_PASSWORD,
                Settings.System.AUTO_TIME,                  // moved to global
                Settings.System.AUTO_TIME_ZONE,             // moved to global
                Settings.System.TIME_12_24,
                Settings.System.DTMF_TONE_WHEN_DIALING,
                Settings.System.DTMF_TONE_TYPE_WHEN_DIALING,
                Settings.System.HEARING_AID,
                Settings.System.TTY_MODE,
                Settings.System.MASTER_MONO,
                Settings.System.MASTER_BALANCE,
                Settings.System.FOLD_LOCK_BEHAVIOR,
                Settings.System.SOUND_EFFECTS_ENABLED,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                Settings.System.POWER_SOUNDS_ENABLED,       // moved to global
                Settings.System.DOCK_SOUNDS_ENABLED,        // moved to global
                Settings.System.LOCKSCREEN_SOUNDS_ENABLED,
                Settings.System.SHOW_WEB_SUGGESTIONS,
                Settings.System.SIP_CALL_OPTIONS,
                Settings.System.SIP_RECEIVE_CALLS,
                Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION,
                Settings.System.POINTER_SPEED,
                Settings.System.POINTER_FILL_STYLE,
                Settings.System.POINTER_STROKE_STYLE,
                Settings.System.POINTER_SCALE,
                Settings.System.VIBRATE_ON,
                Settings.System.VIBRATE_WHEN_RINGING,
                Settings.System.RINGTONE,
                Settings.System.LOCK_TO_APP_ENABLED,
                Settings.System.NOTIFICATION_SOUND,
                Settings.System.ACCELEROMETER_ROTATION,
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                Settings.System.SHOW_BATTERY_PERCENT,
                Settings.System.ALARM_VIBRATION_INTENSITY,
                Settings.System.MEDIA_VIBRATION_INTENSITY,
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                Settings.System.RING_VIBRATION_INTENSITY,
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
                Settings.System.KEYBOARD_VIBRATION_ENABLED,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                Settings.System.DISPLAY_COLOR_MODE_VENDOR_HINT, // must precede DISPLAY_COLOR_MODE
                Settings.System.DISPLAY_COLOR_MODE,
                Settings.System.ALARM_ALERT,
                Settings.System.NOTIFICATION_LIGHT_PULSE,
                Settings.System.WEAR_ACCESSIBILITY_GESTURE_ENABLED,
                Settings.System.CLOCKWORK_BLUETOOTH_SETTINGS_PREF,
                Settings.System.UNREAD_NOTIFICATION_DOT_INDICATOR,
                Settings.System.AUTO_LAUNCH_MEDIA_CONTROLS,
                Settings.System.LOCALE_PREFERENCES,
                Settings.System.MOUSE_REVERSE_VERTICAL_SCROLLING,
                Settings.System.MOUSE_SWAP_PRIMARY_BUTTON,
                Settings.System.TOUCHPAD_POINTER_SPEED,
                Settings.System.TOUCHPAD_NATURAL_SCROLLING,
                Settings.System.TOUCHPAD_TAP_TO_CLICK,
                Settings.System.TOUCHPAD_TAP_DRAGGING,
                Settings.System.TOUCHPAD_RIGHT_CLICK_ZONE,
                Settings.System.CAMERA_FLASH_NOTIFICATION,
                Settings.System.SCREEN_FLASH_NOTIFICATION,
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR,
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
                Settings.System.NOTIFICATION_COOLDOWN_ALL,
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED,
                Settings.System.PREFERRED_REGION,
                Settings.System.HIGH_TOUCH_SENSITIVITY_ENABLE, // Lineage Settings
                Settings.System.HIGH_TOUCH_POLLING_RATE_ENABLE,
                Settings.System.STATUS_BAR_CLOCK,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_LAUNCHER,
                Settings.System.ZEN_ALLOW_LIGHTS,
                Settings.System.STATUS_BAR_AM_PM,
                Settings.System.STATUS_BAR_BATTERY_STYLE,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                Settings.System.INCREASING_RING,
                Settings.System.INCREASING_RING_START_VOLUME,
                Settings.System.INCREASING_RING_RAMP_UP_TIME,
                Settings.System.NAV_BUTTONS,
                Settings.System.NAVIGATION_BAR_MENU_ARROW_KEYS,
                Settings.System.NAVIGATION_BAR_HINT,
                Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                Settings.System.KEY_BACK_LONG_PRESS_ACTION,
                Settings.System.BACK_WAKE_SCREEN,
                Settings.System.VOLUME_UP_AND_DOWN_MUTE,
                Settings.System.MENU_WAKE_SCREEN,
                Settings.System.VOLUME_ANSWER_CALL,
                Settings.System.VOLUME_WAKE_SCREEN,
                Settings.System.KEY_MENU_ACTION,
                Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                Settings.System.KEY_ASSIST_ACTION,
                Settings.System.KEY_ASSIST_LONG_PRESS_ACTION,
                Settings.System.KEY_APP_SWITCH_ACTION,
                Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                Settings.System.KEY_EDGE_LONG_SWIPE_ACTION,
                Settings.System.HOME_WAKE_SCREEN,
                Settings.System.ASSIST_WAKE_SCREEN,
                Settings.System.APP_SWITCH_WAKE_SCREEN,
                Settings.System.CAMERA_WAKE_SCREEN,
                Settings.System.CAMERA_SLEEP_ON_RELEASE,
                Settings.System.CAMERA_LAUNCH,
                Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                Settings.System.TORCH_LONG_PRESS_POWER_GESTURE,
                Settings.System.TORCH_LONG_PRESS_POWER_TIMEOUT,
                Settings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED,
                Settings.System.CHARGING_CONTROL_ENABLED,
                Settings.System.CHARGING_CONTROL_MODE,
                Settings.System.CHARGING_CONTROL_START_TIME,
                Settings.System.CHARGING_CONTROL_TARGET_TIME,
                Settings.System.CHARGING_CONTROL_LIMIT,
                Settings.System.BATTERY_LIGHT_ENABLED,
                Settings.System.BATTERY_LIGHT_FULL_CHARGE_DISABLED,
                Settings.System.BATTERY_LIGHT_PULSE,
                Settings.System.BATTERY_LIGHT_LOW_COLOR,
                Settings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                Settings.System.BATTERY_LIGHT_FULL_COLOR,
                Settings.System.BATTERY_LIGHT_BRIGHTNESS_LEVEL,
                Settings.System.BATTERY_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                Settings.System.ENABLE_MWI_NOTIFICATION,
                Settings.System.PROXIMITY_ON_WAKE,
                Settings.System.DISPLAY_TEMPERATURE_DAY,
                Settings.System.DISPLAY_TEMPERATURE_NIGHT,
                Settings.System.DISPLAY_TEMPERATURE_MODE,
                Settings.System.DISPLAY_AUTO_OUTDOOR_MODE,
                Settings.System.DISPLAY_ANTI_FLICKER,
                Settings.System.DISPLAY_READING_MODE,
                Settings.System.DISPLAY_CABC,
                Settings.System.DISPLAY_COLOR_ENHANCE,
                Settings.System.DISPLAY_AUTO_CONTRAST,
                Settings.System.LIVE_DISPLAY_HINTED,
                Settings.System.DISPLAY_COLOR_ADJUSTMENT,
                Settings.System.DISPLAY_TEMPERATURE_MODE,
                Settings.System.DOUBLE_TAP_SLEEP_GESTURE,
                Settings.System.NAVBAR_LEFT_IN_LANDSCAPE,
                Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                Settings.System.LOCKSCREEN_ROTATION,
                Settings.System.QS_SHOW_BRIGHTNESS_SLIDER,
                Settings.System.VOLBTN_MUSIC_CONTROLS,
                Settings.System.CALL_RECORDING_FORMAT,
                Settings.System.VOLUME_KEY_CURSOR_CONTROL,
                Settings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                Settings.System.NOTIFICATION_LIGHT_SCREEN_ON,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF,
                Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR,
                Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON,
                Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF,
                Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR,
                Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON,
                Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF,
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                Settings.System.NOTIFICATION_LIGHT_COLOR_AUTO,
                Settings.System.NOTIFICATION_LIGHT_PULSE_OVERRIDE,
                Settings.System.AUTO_BRIGHTNESS_ONE_SHOT,
                Settings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK,
                Settings.System.DISPLAY_PICTURE_ADJUSTMENT,
                Settings.System.FORCE_SHOW_NAVBAR,
                Settings.System.CLICK_PARTIAL_SCREENSHOT,
                Settings.System.ENABLE_TASKBAR
        ));
        if (Flags.backUpSmoothDisplayAndForcePeakRefreshRate()) {
            settings.add(Settings.System.PEAK_REFRESH_RATE);
            settings.add(Settings.System.MIN_REFRESH_RATE);
        }
        return settings.toArray(new String[0]);
    }
}
