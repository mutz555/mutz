package com.mutz.fingerprintbypass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity for the application
 */
public class MainActivity extends Activity {
    
    private TextView moduleStatusText;
    private ImageView moduleStatusIcon;
    private Switch moduleEnabledSwitch;
    private Switch debugLoggingSwitch;
    private Button clearLogButton;
    private TextView logTextView;
    
    private PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize preferences manager
        prefsManager = PreferencesManager.getInstance(this);
        
        // Initialize views
        moduleStatusText = findViewById(R.id.module_status_text);
        moduleStatusIcon = findViewById(R.id.module_status_icon);
        moduleEnabledSwitch = findViewById(R.id.module_enabled_switch);
        debugLoggingSwitch = findViewById(R.id.debug_logging_switch);
        clearLogButton = findViewById(R.id.clear_log_button);
        logTextView = findViewById(R.id.log_text_view);
        
        // Set switch states from preferences
        moduleEnabledSwitch.setChecked(prefsManager.isModuleEnabled());
        debugLoggingSwitch.setChecked(prefsManager.isDebugEnabled());
        
        // Check if module is active
        updateModuleStatus();
        
        // Add listeners
        moduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefsManager.setModuleEnabled(isChecked);
                Toast.makeText(MainActivity.this, 
                        getString(isChecked ? R.string.module_enabled : R.string.module_disabled), 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        debugLoggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefsManager.setDebugEnabled(isChecked);
                Toast.makeText(MainActivity.this, 
                        getString(isChecked ? R.string.debug_enabled : R.string.debug_disabled), 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        clearLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefsManager.clearLog();
                updateLogView();
                Toast.makeText(MainActivity.this, R.string.log_cleared, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Hide app icon from launcher if requested
        if (prefsManager.isModuleEnabled()) {
            hideAppFromLauncher();
        }
        
        // Update log view
        updateLogView();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateModuleStatus();
        updateLogView();
    }
    
    /**
     * Update the module status display
     */
    private void updateModuleStatus() {
        boolean isActive = isModuleActive.isModuleActive();
        
        if (isActive) {
            moduleStatusText.setText(R.string.module_status_active);
            moduleStatusIcon.setImageResource(R.drawable.ic_status);
        } else {
            moduleStatusText.setText(R.string.module_status_inactive);
            moduleStatusIcon.setImageResource(R.drawable.ic_status_inactive);
        }
    }
    
    /**
     * Update the log view
     */
    private void updateLogView() {
        String logContent = prefsManager.getLog();
        if (logContent.isEmpty()) {
            logTextView.setText(R.string.no_logs);
        } else {
            logTextView.setText(logContent);
        }
    }
    
    /**
     * Log an entry from external source (used by hook logger)
     * 
     * @param logEntry The log entry
     */
    public static void addLogEntry(String logEntry) {
        // This is called from the hook, but we can't do anything directly
        // It's here for future implementation if needed
    }
    
    /**
     * Hide app from launcher
     */
    private void hideAppFromLauncher() {
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
    
    /**
     * Show app in launcher
     */
    private void showAppInLauncher() {
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        getPackageManager().setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}