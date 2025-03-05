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
     * {@link com.android.internal.lineage.health.HealthInterface} to access the Health interface.
     *
     * @see android.content.Context#getSystemService
     * @see com.android.internal.lineage.health.HealthInterface
     *
     * @hide
     */
    public static final String LINEAGE_HEALTH_INTERFACE = "lineagehealth";

    /**
     * Features supported by the imported part of Lineage SDK.
     */
    public static class Features {

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the lineage health
         * service utilized by the imported part of lineage sdk and LineageParts.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HEALTH = "org.lineageos.health";
    }
}
