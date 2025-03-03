package com.android.internal.util.alpha;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HideAppListUtils {

    private HideAppListUtils() {
    }

    public static Set<String> getAppsForUser(Context context, int userId) {
        if (context == null) {
            return new HashSet<>();
        }
        return getAppsForUser(context.getContentResolver(), userId);
    }

    public static Set<String> getAppsForUser(ContentResolver cr, int userId) {
        if (cr == null || userId < 0) {
            return new HashSet<>();
        }

        final String apps;
        try {
            apps = Settings.Secure.getStringForUser(
                    cr, Settings.Secure.HIDE_APPLIST, userId);
        } catch (IllegalStateException e) {
            return new HashSet<>();
        }

        if (TextUtils.isEmpty(apps) || ",".equals(apps)) {
            return new HashSet<>();
        }

        return new HashSet<>(Arrays.asList(apps.split(",")));
    }

    public static boolean containsForUser(Context context, String packageName, int userId) {
        if (context == null || TextUtils.isEmpty(packageName) || userId < 0) {
            return false;
        }
        return containsForUser(context.getContentResolver(), packageName, userId);
    }

    public static boolean containsForUser(ContentResolver cr, String packageName, int userId) {
        if (cr == null || TextUtils.isEmpty(packageName) || userId < 0) {
            return false;
        }
        return getAppsForUser(cr, userId).contains(packageName);
    }

    public static void addAppForUser(Context context, String packageName, int userId) {
        if (context == null || TextUtils.isEmpty(packageName) || userId < 0) {
            return;
        }

        final Set<String> apps = getAppsForUser(context, userId);
        if (apps.add(packageName)) {
            putAppsForUser(context, apps, userId);
        }
    }

    public static void removeAppForUser(Context context, String packageName, int userId) {
        if (context == null || TextUtils.isEmpty(packageName) || userId < 0) {
            return;
        }

        final Set<String> apps = getAppsForUser(context, userId);
        if (apps.remove(packageName)) {
            putAppsForUser(context, apps, userId);
        }
    }

    public static void setAppsForUser(Context context, Set<String> apps, int userId) {
        if (context == null || userId < 0) {
            return;
        }
        putAppsForUser(context, apps != null ? apps : new HashSet<>(), userId);
    }

    public static boolean removePackageForUser(Context context, String packageName, int userId) {
        if (context == null || TextUtils.isEmpty(packageName) || userId < 0) {
            return false;
        }

        final Set<String> apps = getAppsForUser(context, userId);
        if (!apps.remove(packageName)) {
            return false;
        }

        putAppsForUser(context, apps, userId);
        return true;
    }

    private static void putAppsForUser(Context context, Set<String> apps, int userId) {
        Settings.Secure.putStringForUser(
                context.getContentResolver(),
                Settings.Secure.HIDE_APPLIST,
                String.join(",", apps),
                userId);
    }
}