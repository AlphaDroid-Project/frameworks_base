/*
 * SPDX-FileCopyrightText: 2015-2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.validators;

import android.text.TextUtils;
import android.util.ArraySet;

import java.util.Arrays;
import java.util.regex.Pattern;

class DelimitedListValidator implements Validator {
    private final ArraySet<String> mValidValueSet;
    private final String mDelimiter;
    private final boolean mAllowEmptyList;

    public DelimitedListValidator(String[] validValues, String delimiter,
                                  boolean allowEmptyList) {
        mValidValueSet = new ArraySet<String>(Arrays.asList(validValues));
        mDelimiter = delimiter;
        mAllowEmptyList = allowEmptyList;
    }
    @Override
    public boolean validate(String value) {
        ArraySet<String> values = new ArraySet<String>();
        if (!TextUtils.isEmpty(value)) {
            final String[] array = TextUtils.split(value, Pattern.quote(mDelimiter));
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                values.add(item);
            }
        }
        if (values.size() > 0) {
            values.removeAll(mValidValueSet);
            // values.size() will be non-zero if it contains any values not in
            // mValidValueSet
            return values.size() == 0;
        } else if (mAllowEmptyList) {
            return true;
        }
        return false;
    }
}
