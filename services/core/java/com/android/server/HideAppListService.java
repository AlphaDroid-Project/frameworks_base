/*
 * Copyright (C) 2025 the AxionAOSP Project
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

package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.util.alpha.HideAppListUtils;

import java.util.List;

public class HideAppListService extends SystemService {
    private static final String TAG = "HideAppListService";

    private final Context mContext;

    public HideAppListService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        Slog.i(TAG, "Starting HideAppListService");
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != PHASE_BOOT_COMPLETED) {
            return;
        }

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addDataScheme("package");
        mContext.registerReceiver(new PackageUninstallReceiver(), filter);
    }

    private final class PackageUninstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
                return;
            }

            final String packageName = intent.getData() != null
                    ? intent.getData().getSchemeSpecificPart()
                    : null;
            if (TextUtils.isEmpty(packageName)) {
                return;
            }

            Slog.i(TAG, "Package fully removed: " + packageName);
            removeFromHideAppListForAllUsers(packageName);
        }
    }

    private void removeFromHideAppListForAllUsers(String packageName) {
        final UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager == null) {
            return;
        }

        final List<UserInfo> users = userManager.getUsers();
        for (UserInfo user : users) {
            if (user == null || user.id < UserHandle.USER_SYSTEM) {
                continue;
            }

            if (HideAppListUtils.removePackageForUser(mContext, packageName, user.id)) {
                Slog.i(TAG, "Removed " + packageName + " from hide applist for user " + user.id);
            }
        }
    }
}