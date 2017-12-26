package com.github.faucamp.simplertmp;

import android.util.Log;

/**
 * Created by huangmeng on 2017/12/25.
 */

public class Logger {

    public static void d(String tag, String msg) {
        //Log.d(tag, msg);
        System.out.println("[D]" + tag + ":" + msg);
    }

    public static void e(String tag, String msg) {
        //Log.e(tag, msg);
        System.out.println("[E]" + tag + ":" + msg);
    }

    public static void d(String tag, String msg, Exception e) {
        //Log.d(tag, msg, e);
        System.out.println("[D]" + tag + ":" + msg + "\n cause:" + e.getMessage());
    }

    public static void e(String tag, String msg, Exception e) {
        //Log.e(tag, msg, e);
        System.out.println("[E]" + tag + ":" + msg + "\n cause:" + e.getMessage());
    }
}
