/*
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.internal.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.UserHandle;

import android.provider.Settings;

public final class PowerMenuUtils {
    public static boolean isAdvancedRestartPossible(final Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean keyguardLocked = km.inKeyguardRestrictedInputMode() && km.isKeyguardSecure();
        boolean advancedRestartEnabled = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT, 1) == 1;
        boolean advancedRestartSecuredEnabled = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT_SECURED, 1) == 1;
        boolean isPrimaryUser = UserHandle.getCallingUserId() == UserHandle.USER_SYSTEM;

        return advancedRestartEnabled && (!keyguardLocked || advancedRestartSecuredEnabled) && isPrimaryUser;
    }
}
