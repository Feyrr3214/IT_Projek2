package com.example.itprojek2.controller;

import android.util.Log;
import com.example.itprojek2.BuildConfig;

/**
 * AppLogger — Wrapper logging yang hanya mengaktifkan Log pada mode DEBUG.
 *
 * Di release build (BuildConfig.DEBUG = false), semua log dinonaktifkan
 * untuk mencegah informasi sensitif bocor melalui logcat.
 */
public final class AppLogger {

    private AppLogger() {
        // Utility class — tidak perlu instansiasi
    }

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg);
        }
    }
}
