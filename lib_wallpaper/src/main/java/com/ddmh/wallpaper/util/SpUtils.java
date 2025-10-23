package com.ddmh.wallpaper.util;

import android.content.Context;
import android.content.SharedPreferences;


public class SpUtils {
    private static SharedPreferences spUtils;

    private static SharedPreferences getSp(Context context) {
        if (spUtils == null) {
            spUtils = context.getSharedPreferences("wallpaper_sp", Context.MODE_PRIVATE);
        }
        return spUtils;
    }

    public static void putString(Context context, String key, String value) {
        getSp(context).edit().putString(key, value).apply();
    }


    public static String getString(Context context, String key) {
        return getSp(context).getString(key, "");
    }

    public static void putBoolean(Context context, String key, boolean value) {
        getSp(context).edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return getSp(context).getBoolean(key, defValue);
    }
}
