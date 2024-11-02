/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package lineageos.preference;

import android.content.Context;
import android.util.AttributeSet;

import android.provider.Settings;

public class LineageGlobalSettingListPreference extends SelfRemovingListPreference {

    public LineageGlobalSettingListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LineageGlobalSettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getIntValue(int defValue) {
        return getValue() == null ? defValue : Integer.valueOf(getValue());
    }

    @Override
    protected boolean isPersisted() {
        return Settings.Global.getString(getContext().getContentResolver(),
                getKey()) != null;
    }

    @Override
    protected void putString(String key, String value) {
        Settings.Global.putString(getContext().getContentResolver(), key, value);
    }

    @Override
    protected String getString(String key, String defaultValue) {
        return Settings.Global.getString(getContext().getContentResolver(),
                key, defaultValue);
    }
}
