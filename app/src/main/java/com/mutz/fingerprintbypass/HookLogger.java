package com.mutz.fingerprintbypass;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Utility class to handle logging for hooks
 */
public class HookLogger {
    private static final String TAG = "FingerprintBypass";
    private static final String PREFS_FILE = "com.mutz.fingerprintbypass_preferences";
    private static final String KEY_DEBUG_ENABLED = "debug_logging_enabled";
    private static final String KEY_HOOK_LOG = "hook_log";
    
    private static boolean debugEnabled = false;
    private static XSharedPreferences xPrefs = null;
    
    /**
     * Initializes the logger with xposed preferences
     */
    public static void init() {
        try {
            xPrefs = new XSharedPreferences("com.mutz.fingerprintbypass", PREFS_FILE);
            xPrefs.makeWorldReadable();
            xPrefs.reload();
            debugEnabled = xPrefs.getBoolean(KEY_DEBUG_ENABLED, false);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize HookLogger: " + t.getMessage());
            debugEnabled = true; // In case of error, enable debug by default
        }
    }
    
    /**
     * Logs a debug message if debug is enabled
     * 
     * @param message The message to log
     */
    public static void log(String message) {
        if (debugEnabled) {
            Log.d(TAG, message);
            saveLogToPrefs(message);
        }
    }
    
    /**
     * Logs an error message
     * 
     * @param message The error message to log
     */
    public static void error(String message) {
        Log.e(TAG, message);
        saveLogToPrefs("ERROR: " + message);
    }
    
    /**
     * Logs an error message with throwable details
     * 
     * @param message The error message
     * @param t The throwable/exception
     */
    public static void error(String message, Throwable t) {
        Log.e(TAG, message, t);
        saveLogToPrefs("ERROR: " + message + " - " + t.getMessage());
    }
    
    /**
     * Save log entry to preferences for display in UI
     * 
     * @param message The message to save
     */
    private static void saveLogToPrefs(String message) {
        try {
            if (xPrefs != null) {
                xPrefs.reload();
                String timestamp = android.text.format.DateFormat.format("MM-dd HH:mm:ss", 
                        new java.util.Date()).toString();
                String logEntry = timestamp + " - " + message;
                
                // Since we can't write to XSharedPreferences directly, 
                // we'll do it in the app through MainActivity
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to save log to preferences: " + t.getMessage());
        }
    }
}