package com.app.util;

import android.util.Log;

/**
 * Created by admin on 2016/1/5.
 */

public class MyLog {
    public static int logLevel = Log.INFO;//控制是否输出Log

    public static void i(String tag, String msg) {
        if (logLevel <= Log.INFO)
            android.util.Log.i(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (logLevel <= Log.ERROR)
            android.util.Log.e(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (logLevel <= Log.DEBUG)
            android.util.Log.d(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (logLevel <= Log.VERBOSE)
            android.util.Log.v(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (logLevel <= Log.WARN)
            android.util.Log.w(tag, msg);
    }
}
