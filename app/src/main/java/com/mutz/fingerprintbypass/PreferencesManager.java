package com.mutz.fingerprintbypass;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages preferences for the application
 */
public class PreferencesManager {
    private static final String PREFS_FILE = "com.mutz.fingerprintbypass_preferences";
    private static final String KEY_MODULE_ENABLED = "module_enabled";
    private static final String KEY_DEBUG_ENABLED = "debug_logging_enabled";
    private static final String KEY_HOOK_LOG = "hook_log";
    
    private static PreferencesManager instance = null;
    private SharedPreferences prefs;
    
    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_WORLD_READABLE);
    }
    
    /**
     * Gets the PreferencesManager instance
     * 
     * @param context Application context
     * @return PreferencesManager instance
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }
    
    /**
     * Check if module is enabled
     * 
     * @return true if module is enabled
     */
    public boolean isModuleEnabled() {
        return prefs.getBoolean(KEY_MODULE_ENABLED, true);
    }
    
    /**
     * Set module enabled
     * 
     * @param enabled true to enable module
     */
    public void setModuleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MODULE_ENABLED, enabled).apply();
    }
    
    /**
     * Check if debug logging is enabled
     * 
     * @return true if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return prefs.getBoolean(KEY_DEBUG_ENABLED, false);
    }
    
    /**
     * Set debug logging enabled
     * 
     * @param enabled true to enable debug logging
     */
    public void setDebugEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEBUG_ENABLED, enabled).apply();
    }
    
    /**
     * Add log entry
     * 
     * @param logEntry The log entry to add
     */
    public void addLogEntry(String logEntry) {
        String currentLog = prefs.getString(KEY_HOOK_LOG, "");
        // Keep log at reasonable size by capping at 10KB and removing oldest entries
        if (currentLog.length() > 10000) {
            // Remove oldest entries (before the last few newlines)
            int cutIndex = currentLog.indexOf("\n", currentLog.length() / 2);
            if (cutIndex > 0) {
                currentLog = currentLog.substring(cutIndex);
            } else {
                currentLog = "";
            }
        }
        
        // Append new log entry with a newline
        String newLog = currentLog + logEntry + "\n";
        prefs.edit().putString(KEY_HOOK_LOG, newLog).apply();
    }
    
    /**
     * Get the current log
     * 
     * @return The current log
     */
    public String getLog() {
        return prefs.getString(KEY_HOOK_LOG, "");
    }
    
    /**
     * Clear the log
     */
    public void clearLog() {
        prefs.edit().putString(KEY_HOOK_LOG, "").apply();
    }
}