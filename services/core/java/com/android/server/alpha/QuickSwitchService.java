/*
 * Copyright (C) 2023 The RisingOS Android Project
 * Copyright (C) 2023-2024 AlphaDroid Project
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

package com.android.server.alpha;

import static android.os.Build.IS_ENG;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import com.android.server.ServiceThread;
import com.android.server.SystemService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class QuickSwitchService extends SystemService {

    private static final boolean DEBUG = true || IS_ENG;

    private static final String LAUNCHER3 = "com.android.launcher3";
    private static final String NEXUS_LAUNCHER = "com.google.android.apps.nexuslauncher";
    private static final String LAWNCHAIR = "app.lawnchair";

    private static final List<String> LAUNCHER_PACKAGES = List.of(LAUNCHER3, NEXUS_LAUNCHER, LAWNCHAIR);

    private static final String DEFAULT_LAUNCHER_PROP = "persist.sys.default_launcher";
    private static final String LAWNCHAIR_OVERLAY = "app.lawnchair.overlay";

    private static final List <String> WALLPAPER_PICKER_PACKAGES = List.of(
        "com.android.wallpaper",
        "com.android.wallpaper.picker.overlay.android",
        "com.android.wallpaper.picker.overlay.settings",
        "com.android.customization.themes"
    );

    private static final List<String> WALLPAPER_PICKER_GOOGLE_PACKAGES = List.of(
        "com.google.android.aicore",
        "com.google.android.apps.aiwallpapers",
        "com.google.android.apps.customization.pixel",
        "com.google.android.apps.emojiwallpaper",
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.wallpaper.overlay.android",
        "com.google.android.apps.wallpaper.overlay.settings",
        "com.google.android.apps.wallpaper.pixel",
        "com.google.android.wallpaper.effects",
        "com.google.pixel.livewallpaper"
    );

    private static final List<String> NEXUS_LAUNCHER_OVERLAYS = List.of(
        "com.google.nexus.launcher.overlay.android",
        "com.google.nexus.launcher.overlay.systemui"
    );

    private static final String TAG = "QuickSwitchService";
    private static final int THREAD_PRIORITY_DEFAULT = android.os.Process.THREAD_PRIORITY_DEFAULT;

    private final Context mContext;
    private final IPackageManager mPM;
    private final IUserManager mUM;
    private final ContentResolver mResolver;
    private final String mOpPackageName;

    private ServiceThread mWorker;
    private Handler mHandler;

    public static boolean shouldHide(int userId, String packageName) {
        return packageName != null && isDisabledPackage(packageName);
    }

    public static ParceledListSlice<PackageInfo> recreatePackageList(
            int userId, ParceledListSlice<PackageInfo> list) {
        List<PackageInfo> appList = list.getList();
        appList.removeIf(info -> isDisabledPackage(info.packageName));
        return new ParceledListSlice<>(appList);
    }

    public static List<ApplicationInfo> recreateApplicationList(
            int userId, List<ApplicationInfo> list) {
        List<ApplicationInfo> appList = new ArrayList<>(list);
        appList.removeIf(info -> isDisabledPackage(info.packageName));
        return appList;
    }

    private void updateStateForUser(int userId) {

        List<String> enabledPackages = new ArrayList<>();
        List<String> disabledPackages = new ArrayList<>();

        int i = SystemProperties.getInt(DEFAULT_LAUNCHER_PROP, 0);
        if (i < 0 || i >= LAUNCHER_PACKAGES.size()) {
            Log.e(TAG, "Invalid Launcher");
            return;
        }

        // handle launchers
        String defaultLauncher = LAUNCHER_PACKAGES.get(i);
        for (String launcher : LAUNCHER_PACKAGES) {
            if (launcher.equals(defaultLauncher)) {
                enabledPackages.add(defaultLauncher);
            }
            else {
                disabledPackages.add(launcher);
            }
        }

        // handle relatives
        if (defaultLauncher.equals(LAUNCHER3)) {
            enabledPackages.addAll(WALLPAPER_PICKER_PACKAGES);
            disabledPackages.addAll(WALLPAPER_PICKER_GOOGLE_PACKAGES);
            disabledPackages.addAll(NEXUS_LAUNCHER_OVERLAYS);
            disabledPackages.add(LAWNCHAIR_OVERLAY);
        } else if (defaultLauncher.equals(LAWNCHAIR)) {
            enabledPackages.add(LAWNCHAIR_OVERLAY);
            enabledPackages.addAll(WALLPAPER_PICKER_PACKAGES);
            disabledPackages.addAll(WALLPAPER_PICKER_GOOGLE_PACKAGES);
            disabledPackages.addAll(NEXUS_LAUNCHER_OVERLAYS);
        } else if (defaultLauncher.equals(NEXUS_LAUNCHER)) {
            enabledPackages.addAll(WALLPAPER_PICKER_GOOGLE_PACKAGES);
            enabledPackages.addAll(NEXUS_LAUNCHER_OVERLAYS);
            disabledPackages.addAll(WALLPAPER_PICKER_PACKAGES);
            disabledPackages.add(LAWNCHAIR_OVERLAY);
        }

        for (String pkg : disabledPackages) {
            updateLauncherComponentsState(userId, pkg, false);
            if (DEBUG) Log.d(TAG, "disabling " + pkg + "... done");
        }
        for (String pkg: enabledPackages) {
            updateLauncherComponentsState(userId, pkg, true);
            if (DEBUG) Log.d(TAG, "enabling " + pkg + "... done");
        }
    }

    private void updateLauncherComponentsState(int userId, String packageName, boolean enable) {
        try {
            mPM.setApplicationEnabledSetting(packageName,
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                          : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    0, userId, mOpPackageName);
        } catch (IllegalArgumentException ignored) {
            Log.e(TAG, (enable ? "enabling " : "disabling ") + packageName + "... not found");
        }
        catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    private static boolean isDisabledPackage(String packageName) {
        int defaultLauncher = SystemProperties.getInt(DEFAULT_LAUNCHER_PROP, 0);
        for (int i = 0; i < LAUNCHER_PACKAGES.size(); i++) {
            if (i != defaultLauncher) {
                String launcher = LAUNCHER_PACKAGES.get(i);
                if (launcher.equals(packageName)
                        || getDisabledLauncherPackages(launcher).equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> getDisabledLauncherPackages(String launcher) {
        List<String> disabledPackages = new ArrayList<>();

        if (launcher.equals(LAUNCHER3)) {
            disabledPackages.addAll(WALLPAPER_PICKER_GOOGLE_PACKAGES);
            disabledPackages.addAll(NEXUS_LAUNCHER_OVERLAYS);
            disabledPackages.add(LAWNCHAIR_OVERLAY);
        } else if (launcher.equals(LAWNCHAIR)) {
            disabledPackages.addAll(WALLPAPER_PICKER_GOOGLE_PACKAGES);
            disabledPackages.addAll(NEXUS_LAUNCHER_OVERLAYS);
        } else if (launcher.equals(NEXUS_LAUNCHER)) {
            disabledPackages.addAll(WALLPAPER_PICKER_PACKAGES);
            disabledPackages.add(LAWNCHAIR_OVERLAY);
        }
        return disabledPackages;
    }

    private void initForUser(int userId) {
        if (userId < 0)
            return;
        updateStateForUser(userId);
    }

    private void init() {
        try {
            for (UserInfo user : mUM.getUsers(false, false, false)) {
                initForUser(user.id);
            }
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_REMOVED);
        mContext.registerReceiver(new UserReceiver(), filter,
                android.Manifest.permission.MANAGE_USERS, mHandler);
    }

    @Override
    public void onStart() {
        mWorker = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mWorker.start();
        mHandler = new Handler(mWorker.getLooper());
        init();
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        }
    }

    public QuickSwitchService(Context context) {
        super(context);
        mContext = context;
        mResolver = context.getContentResolver();
        mPM = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUM = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
        mOpPackageName = context.getOpPackageName();
    }

    private final class UserReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
            if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                initForUser(userId);
            }
        }
    }
}
