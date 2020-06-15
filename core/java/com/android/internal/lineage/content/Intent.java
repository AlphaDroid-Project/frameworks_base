/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.lineage.content;

import android.Manifest;

/**
 * LineageOS specific intent definition class.
 */
public class Intent {

    /**
     * Broadcast action: perform any initialization required for LineageHW services.
     * Runs when the service receives the signal the device has booted, but
     * should happen before {@link android.content.Intent#ACTION_BOOT_COMPLETED}.
     *
     * Requires {@link android.Manifest.permission#HARDWARE_ABSTRACTION_ACCESS}.
     * @hide
     */
    public static final String ACTION_INITIALIZE_LINEAGE_HARDWARE =
            "lineageos.intent.action.INITIALIZE_LINEAGE_HARDWARE";

    /**
     * Broadcast action: notify SystemUI that LiveDisplay service has finished initialization.
     * @hide
     */
    public static final String ACTION_INITIALIZE_LIVEDISPLAY =
            "lineageos.intent.action.INITIALIZE_LIVEDISPLAY";
}
