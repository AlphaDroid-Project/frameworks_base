/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.alpha.style;

import android.text.TextUtils;

/**
 * User-tunable parameters for UI styles.
 * Shared between SystemUI and Settings.
 *
 * Serialized format: "saturation;lightness;opacity;strength;angle"
 * Example: "1.0;1.0;1.0;1.0;0.0"
 */
public final class UserStyleSettings {

    public static final UserStyleSettings DEFAULT = new UserStyleSettings();

    private final float saturation;
    private final float lightness;
    private final float opacity;
    private final float strength;
    private final float angle;

    public UserStyleSettings() {
        this(1.0f, 1.0f, 1.0f, 1.0f, 0f);
    }

    public UserStyleSettings(float saturation, float lightness, float opacity,
            float strength, float angle) {
        this.saturation = saturation;
        this.lightness = lightness;
        this.opacity = opacity;
        this.strength = strength;
        this.angle = angle;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getLightness() {
        return lightness;
    }

    public float getOpacity() {
        return opacity;
    }

    public float getStrength() {
        return strength;
    }

    public float getAngle() {
        return angle;
    }

    @Override
    public String toString() {
        return saturation + ";" + lightness + ";" + opacity + ";" + strength + ";" + angle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStyleSettings that = (UserStyleSettings) o;
        return Float.compare(that.saturation, saturation) == 0
                && Float.compare(that.lightness, lightness) == 0
                && Float.compare(that.opacity, opacity) == 0
                && Float.compare(that.strength, strength) == 0
                && Float.compare(that.angle, angle) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(saturation);
        result = 31 * result + Float.floatToIntBits(lightness);
        result = 31 * result + Float.floatToIntBits(opacity);
        result = 31 * result + Float.floatToIntBits(strength);
        result = 31 * result + Float.floatToIntBits(angle);
        return result;
    }

    /**
     * Parse settings from serialized string.
     *
     * @param data Serialized string in format "sat;light;opac;str;angle"
     * @return Parsed settings or DEFAULT if parsing fails
     */
    public static UserStyleSettings fromString(String data) {
        if (TextUtils.isEmpty(data)) {
            return DEFAULT;
        }

        try {
            String[] parts = data.split(";");
            float sat = parts.length > 0 ? Float.parseFloat(parts[0]) : 1.0f;
            float light = parts.length > 1 ?  Float.parseFloat(parts[1]) : 1.0f;
            float opac = parts.length > 2 ?  Float.parseFloat(parts[2]) : 1.0f;
            float str = parts.length > 3 ? Float.parseFloat(parts[3]) : 1.0f;
            float ang = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            return new UserStyleSettings(sat, light, opac, str, ang);
        } catch (NumberFormatException e) {
            return DEFAULT;
        }
    }
}
