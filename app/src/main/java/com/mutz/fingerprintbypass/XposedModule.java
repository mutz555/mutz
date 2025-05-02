package com.mutz.fingerprintbypass;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed Module to bypass fingerprint hardware check in HyperOS
 */
public class XposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    
    // Target classes and methods
    private static final String FINGERPRINT_SERVICE_STUB_IMPL = "com.android.server.biometrics.fingerprint.FingerprintServiceStubImpl";
    private static final String FINGERPRINT_AUTHENTICATOR = "com.android.server.biometrics.sensors.fingerprint.FingerprintAuthenticator";
    private static final String FINGERPRINT_MANAGER = "android.hardware.fingerprint.FingerprintManager";
    
    // Packages to hook
    private static final String[] SUPPORTED_PACKAGES = {
        "android",
        "com.android.systemui",
        "com.android.settings"
    };
    
    private boolean initialized = false;
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // Initialize the logger
        HookLogger.init();
        HookLogger.log("Fingerprint Bypass module initializing in Zygote");
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!initialized) {
            initialized = true;
            HookLogger.log("Fingerprint Bypass module initialized");
        }
        
        // Only hook packages we're interested in
        boolean packageSupported = false;
        for (String pkg : SUPPORTED_PACKAGES) {
            if (lpparam.packageName.equals(pkg)) {
                packageSupported = true;
                break;
            }
        }
        
        if (packageSupported) {
            HookLogger.log("Hooking package: " + lpparam.packageName);
            
            try {
                // Hook isHardwareDetected method
                hookFingerprintService(lpparam);
                
                // Hook additional methods if needed
                hookFingerprintAuthenticator(lpparam);
                
                // HyperOS specific hooks
                hookHyperOSFingerprint(lpparam);
                
                HookLogger.log("Successfully hooked fingerprint methods in " + lpparam.packageName);
            } catch (Throwable t) {
                HookLogger.error("Error hooking fingerprint methods: " + t.getMessage(), t);
            }
        }

        // Hook for our own app to check if module is working
        if (lpparam.packageName.equals("com.mutz.fingerprintbypass")) {
            XposedHelpers.findAndHookMethod(
                    "com.mutz.fingerprintbypass.isModuleActive", 
                    lpparam.classLoader,
                    "isModuleActive", 
                    XC_MethodReplacement.returnConstant(true));
            HookLogger.log("Self-hook for isModuleActive successful");
        }
    }
    
    /**
     * Hooks the FingerprintServiceStubImpl class to bypass hardware detection
     */
    private void hookFingerprintService(XC_LoadPackage.LoadPackageParam lpparam) {
        // Main hook for isHardwareDetected
        XposedHelpers.findAndHookMethod(
                FINGERPRINT_SERVICE_STUB_IMPL,
                lpparam.classLoader,
                "isHardwareDetected",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        HookLogger.log("Intercepted isHardwareDetected call, returning true");
                        return true;
                    }
                });
        
        // Hook the error reporting method to catch more details
        XposedHelpers.findAndHookMethod(
                FINGERPRINT_SERVICE_STUB_IMPL,
                lpparam.classLoader,
                "getErrorString",
                int.class,
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int errorCode = (int) param.args[0];
                        HookLogger.log("Fingerprint error code intercepted: " + errorCode);
                        
                        // If error code is related to hardware detection, modify it
                        if (errorCode == 7) { // FINGERPRINT_ERROR_LOCKOUT
                            HookLogger.log("Intercepted FINGERPRINT_ERROR_LOCKOUT, allowing operation");
                            param.setResult("Fingerprint operation allowed");
                        }
                    }
                });
    }
    
    /**
     * Hooks the FingerprintAuthenticator class for additional bypass
     */
    private void hookFingerprintAuthenticator(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Try to hook the canAuthenticate method if it exists
            XposedHelpers.findAndHookMethod(
                    FINGERPRINT_AUTHENTICATOR,
                    lpparam.classLoader,
                    "canAuthenticate",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            HookLogger.log("Intercepted canAuthenticate call, returning true");
                            return true;
                        }
                    });
            
            // Hook the getAvailableSensorCount method if it exists
            XposedHelpers.findAndHookMethod(
                    FINGERPRINT_AUTHENTICATOR,
                    lpparam.classLoader,
                    "getAvailableSensorCount",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            HookLogger.log("Intercepted getAvailableSensorCount call, returning 1");
                            return 1;
                        }
                    });
        } catch (Throwable t) {
            // It's okay if this hook fails, as the class or method might not exist in all versions
            HookLogger.log("Could not hook FingerprintAuthenticator methods: " + t.getMessage());
        }
    }
    
    /**
     * Hooks specific to HyperOS fingerprint functionality
     */
    private void hookHyperOSFingerprint(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook public FingerprintManager API
            XposedHelpers.findAndHookMethod(
                    FINGERPRINT_MANAGER,
                    lpparam.classLoader,
                    "isHardwareDetected",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            HookLogger.log("Intercepted FingerprintManager.isHardwareDetected, returning true");
                            return true;
                        }
                    });
            
            // Additional hooks for HyperOS-specific biometric classes
            // This may require adjustments based on specific HyperOS version
            String[] hyperosClasses = {
                "com.android.server.biometrics.sensors.BiometricService",
                "com.android.server.biometrics.BiometricServiceBase",
                "com.android.server.biometrics.BiometricServiceWrapper"
            };
            
            for (String className : hyperosClasses) {
                try {
                    Class<?> clazz = lpparam.classLoader.loadClass(className);
                    XposedHelpers.findAndHookMethod(
                            className,
                            lpparam.classLoader,
                            "isHardwareDetected",
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    HookLogger.log("Intercepted " + className + ".isHardwareDetected, returning true");
                                    return true;
                                }
                            });
                    HookLogger.log("Successfully hooked " + className);
                } catch (ClassNotFoundException | NoSuchMethodError e) {
                    // Class or method not found, just continue with next class
                    HookLogger.log("Class or method not found: " + className);
                } catch (Throwable t) {
                    HookLogger.error("Error hooking " + className + ": " + t.getMessage(), t);
                }
            }
            
        } catch (Throwable t) {
            HookLogger.log("Could not hook HyperOS specific methods: " + t.getMessage());
        }
    }
}
