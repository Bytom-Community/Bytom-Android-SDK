package com.io.wallet.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hwj on 2018/8/26.
 */

public class SpUtil {
    private static String mName = "bytom_datas";
    private static SharedPreferences.Editor mEditor;
    private static Context mContext;

    public static void init(Context context) {
        mContext = context;
    }

    /**
     * 设置SharePreferenceName 默认是bytom_datas
     *
     * @param name
     */
    public static void setSharePreferenceName(String name) {
        mName = name;
    }

    /**
     * put string val
     *
     * @param key
     * @param val
     */
    public static void putString(String key, String val) {
        put(key, val);
    }

    /**
     * get string val
     *
     * @param key
     * @return
     */
    public static String getString(String key) {
        return (String) get(key, "");
    }

    /**
     * get string val
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static String getString(String key, String defaultVal) {
        return (String) get(key, defaultVal);
    }

    /**
     * put int val
     *
     * @param key
     * @param val
     */
    public static void putInt(String key, int val) {
        put(key, val);
    }

    /**
     * get int val
     *
     * @param key
     * @return
     */
    public static int getInt(String key) {
        return (int) get(key, 0);
    }

    /**
     * get int val
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static int getInt(String key, int defaultVal) {
        return (int) get(key, defaultVal);
    }

    /**
     * put bool val
     *
     * @param key
     * @param val
     */
    public static void putBool(String key, Boolean val) {
        put(key, val);
    }

    /**
     * get bool val
     *
     * @param key
     * @return
     */
    public static boolean getBool(String key) {
        return (boolean) get(key, false);
    }

    /**
     * get bool val
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static boolean getBool(String key, Boolean defaultVal) {
        return (boolean) get(key, defaultVal);
    }

    /**
     * put float val
     *
     * @param key
     * @param val
     */
    public static void putFloat(String key, Float val) {
        put(key, val);
    }

    /**
     * get float val
     *
     * @param key
     * @return
     */
    public static float getFloat(String key) {
        return (float) get(key, 0.0f);
    }

    /**
     * get float val
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static float getFloat(String key, Float defaultVal) {
        return (float) get(key, defaultVal);
    }

    /**
     * put long int val
     *
     * @param key
     * @param val
     */
    public static void putLong(String key, Long val) {
        put(key, val);
    }

    /**
     * get long val
     *
     * @param key
     * @return
     */
    public static long getLong(String key) {
        return (long) get(key, 0L);
    }

    /**
     * get long val
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static long getLong(String key, Long defaultVal) {
        return (long) get(key, defaultVal);
    }

    /**
     * 移除某个key值已经对应的值
     *
     * @param key
     */
    public static void remove(String key) {
        if (mEditor == null) {
            setEditor();
        }
        mEditor.remove(key);
        mEditor.commit();
    }

    /**
     * 清除所有的数据
     */
    public static void clear() {
        if (mEditor == null) {
            setEditor();
        }
        mEditor.clear();
        mEditor.commit();
    }

    /**
     * 查询某个key是否存在
     *
     * @param key
     * @return
     */
    public static boolean contains(String key) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(mName, Context.MODE_PRIVATE);
        return sharedPreferences.contains(key);
    }

    private static void setEditor() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(mName, Context.MODE_PRIVATE);
        mEditor = sharedPreferences.edit();
    }

    private static Object get(String key, Object defaultObject) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(mName, Context.MODE_PRIVATE);

        if (defaultObject instanceof String) {
            return sharedPreferences.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sharedPreferences.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sharedPreferences.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sharedPreferences.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sharedPreferences.getLong(key, (Long) defaultObject);
        } else {
            return sharedPreferences.getString(key, null);
        }
    }

    private static void put(String key, Object object) {
        if (mEditor == null) {
            setEditor();
        }
        if (object instanceof String) {
            mEditor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            mEditor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            mEditor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            mEditor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            mEditor.putLong(key, (Long) object);
        } else {
            mEditor.putString(key, object.toString());
        }
        mEditor.commit();
    }

}
