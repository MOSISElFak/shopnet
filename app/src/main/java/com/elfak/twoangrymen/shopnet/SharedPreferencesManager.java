package com.elfak.twoangrymen.shopnet;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by Bencun on 6/1/2017.
 */

public class SharedPreferencesManager {
    private final static String PREFERENCES_FILE = "appSettings";
    public final static String STR_CURRENT_TOKEN = "CURRENT_TOKEN";
    public final static String STR_KORIME = "KORIME";
    private static SharedPreferences settings = null;

    private SharedPreferencesManager(){}

    public static void init(Context c){
        if(settings == null)
            settings = c.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static String getPreferenceString(String pref){
        if(settings.contains(pref))
            return settings.getString(pref, null);
        return null;
    }

    public static int setPreferenceString(String pref, String val){
        if(settings == null) return 0;
        SharedPreferences.Editor e = settings.edit();
        e.putString(pref, val);
        e.commit();
        return 1;
    }
}
