package com.android.internal.util.alpha;

public final class AlphaConstants {

    private AlphaConstants() {}

    public static final String WALLPAPER_BLUR_TARGET_PROP = "persist.sys.wallpaper.blur_target";
    public static final String WALLPAPER_BLUR_FILTER_PROP = "persist.sys.wallpaper.blur_filter";
    public static final String WALLPAPER_DIM_TARGET_PROP = "persist.sys.wallpaper.dim_target";
    public static final String WALLPAPER_DIM_LEVEL_PROP = "persist.sys.wallpaper.dim_level";

    public static final int WALLPAPER_TARGET_DISABLED = 0;
    public static final int WALLPAPER_TARGET_BOTH = 1;
    public static final int WALLPAPER_TARGET_LOCKSCREEN = 2;
    public static final int WALLPAPER_TARGET_HOMESCREEN = 3;

    public static final int FROSTED_BLUR = 0;
    public static final int GLASS_BLUR = 1;

}
