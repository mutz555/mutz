package com.mutz.fingerprintbypass;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedModule implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String TAG = "FingerprintBypass";

    // Core classes
    private static final String FINGERPRINT_STUB = 
        "com.android.server.biometrics.fingerprint.FingerprintServiceStubImpl";
    private static final String FINGERPRINT_AUTH = 
        "com.android.server.biometrics.sensors.fingerprint.FingerprintAuthenticator";
    private static final String BIOMETRIC_SERVICE = 
        "com.android.server.biometrics.sensors.BiometricService";
    private static final String BIOMETRIC_BASE = 
        "com.android.server.biometrics.BiometricServiceBase";
    private static final String BIOMETRIC_WRAP = 
        "com.android.server.biometrics.BiometricServiceWrapper";
    private static final String BIOMETRIC_MANAGER = 
        "android.hardware.biometrics.BiometricManager";
    private static final String BIOMETRIC_PROMPT = 
        "android.hardware.biometrics.BiometricPrompt";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log(TAG + ": initZygote — hooking system methods");

        // Bypass core fingerprint stub
        try {
            Class<?> stub = XposedHelpers.findClass(FINGERPRINT_STUB, null);
            XposedBridge.hookAllMethods(stub, "isHardwareDetected",
                XC_MethodReplacement.returnConstant(true));
            XposedBridge.log(TAG + ": hooked " + FINGERPRINT_STUB + ".isHardwareDetected");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed hook stub → " + t.getMessage());
        }

        // Bypass BiometricManager API
        try {
            Class<?> bm = XposedHelpers.findClass(BIOMETRIC_MANAGER, null);
            XposedBridge.hookAllMethods(bm, "isHardwareDetected",
                XC_MethodReplacement.returnConstant(true));
            XposedBridge.log(TAG + ": hooked " + BIOMETRIC_MANAGER + ".isHardwareDetected");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed hook BiometricManager → " + t.getMessage());
        }

        // Bypass BiometricPrompt.authenticate(...) to always succeed
        try {
            Class<?> bp = XposedHelpers.findClass(BIOMETRIC_PROMPT, null);
            XposedHelpers.findAndHookMethod(bp, "authenticate", 
                android.app.KeyguardManager.class, XC_MethodReplacement.returnConstant(null));
            XposedBridge.log(TAG + ": hooked " + BIOMETRIC_PROMPT + ".authenticate()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed hook BiometricPrompt → " + t.getMessage());
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Log every package that loads
        XposedBridge.log(TAG + ": Loaded pkg → " + lpparam.packageName);

        // Only proceed hooks for system packages where fingerprint runs
        if (!"android".equals(lpparam.packageName)
         && !"com.android.systemui".equals(lpparam.packageName)
         && !"com.android.settings".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": Applying hooks in " + lpparam.packageName);

        // Hook stub impl in app processes if loaded there
        safeHook(lpparam, FINGERPRINT_STUB, "isHardwareDetected", null);

        // Hook error reporting
        safeHook(lpparam, FINGERPRINT_STUB, "getErrorString",
                 int.class, Context.class,
                 new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                int code = (int)param.args[0];
                XposedBridge.log(TAG + ": errorCode=" + code);
                if (code == 7) { // LOCKOUT
                    param.setResult("Operation allowed");
                }
            }
        });

        // Hook authenticator if present
        safeHook(lpparam, FINGERPRINT_AUTH, "canAuthenticate", null);
        safeHook(lpparam, FINGERPRINT_AUTH, "getAvailableSensorCount", null);

        // Hook broader BiometricService classes
        safeHook(lpparam, BIOMETRIC_SERVICE, "isHardwareDetected", null);
        safeHook(lpparam, BIOMETRIC_BASE,    "isHardwareDetected", null);
        safeHook(lpparam, BIOMETRIC_WRAP,    "isHardwareDetected", null);
    }

    /** Helper to wrap findAndHookMethod in try/catch plus logging */
    private void safeHook(XC_LoadPackage.LoadPackageParam lp, 
                          String className, String methodName, 
                          Object... paramTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(
                className, lp.classLoader, methodName, paramTypesAndCallback);
            XposedBridge.log(TAG + ": hooked " + className + "." + methodName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed hook " 
                + className + "." + methodName + " → " + t.getMessage());
        }
    }
}