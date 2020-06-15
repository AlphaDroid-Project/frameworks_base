/*
 * SPDX-FileCopyrightText: 2015, The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.lineage.app;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class LineageContextConstants {

    /**
     * @hide
     */
    private LineageContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link com.android.internal.lineage.hardware.LineageHardwareManager} to manage the extended
     * hardware features of the device.
     *
     * @see android.content.Context#getSystemService
     * @see com.android.internal.lineage.hardware.LineageHardwareManager
     *
     * @hide
     */
    public static final String LINEAGE_HARDWARE_SERVICE = "lineagehardware";

    /**
     * Manages display color adjustments
     *
     * @hide
     */
    public static final String LINEAGE_LIVEDISPLAY_SERVICE = "lineagelivedisplay";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link com.android.internal.lineage.health.HealthInterface} to access the Health interface.
     *
     * @see android.content.Context#getSystemService
     * @see com.android.internal.lineage.health.HealthInterface
     *
     * @hide
     */
    public static final String LINEAGE_HEALTH_INTERFACE = "lineagehealth";

    /**
     * Features supported by the imported Lineage SDK.
     */
    public static class Features {
        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the hardware abstraction
         * framework service utilized by the imported lineage sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HARDWARE_ABSTRACTION = "org.lineageos.hardware";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the LiveDisplay service
         * utilized by the imported lineage sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String LIVEDISPLAY = "org.lineageos.livedisplay";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the lineage health
         * service utilized by the imported lineage sdk and LineageParts.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HEALTH = "org.lineageos.health";
    }
}
