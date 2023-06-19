package com.pedro.sample.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Iterator;

public class SharedPreferencesUtil {
    static SharedPreferences preferences = null;

    public static void initPreferences(Context context) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    /**
     * 해시 값 저장하기
     */
    public static void setValue(Context context, HashMap<String,String> valueHash) {
        if (preferences == null) {
            initPreferences(context);
        }

        SharedPreferences.Editor editor = preferences.edit();

        Iterator<String> iter = valueHash.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            String value = valueHash.get(key);
            editor.putString(key, value);
        }

        editor.commit();
    }

    /**
     * 문자열 값 저장하기
     */
    public static void setValue(Context context, String key, String value) {
        if (preferences == null) {
            initPreferences(context);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * 문자열 값 가져오기
     */
    public static String getValue(Context context, String key) {
        if (preferences == null) {
            if (context != null) {
                initPreferences(context);
            }
        }
        return preferences.getString(key, "");
    }

}