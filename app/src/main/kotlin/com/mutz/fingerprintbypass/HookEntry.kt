package com.mutz.fingerprintbypass

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {
    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                try {
                    val fingerprintServiceStubImpl = XposedHelpers.findClass(
                        "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl",
                        lpparam.classLoader
                    )

                    XposedHelpers.findAndHookMethod(
                        fingerprintServiceStubImpl,
                        "isFpHardwareDetected",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any {
                                Log.i(TAG, "isFpHardwareDetected() bypassed!")
                                return true
                            }
                        }
                    )

                    XposedBridge.log("$TAG: Successfully hooked FingerprintServiceStubImpl")
                } catch (t: Throwable) {
                    XposedBridge.log("$TAG: Error hooking fingerprint service: ${t.message}")
                }
            }

            "com.transsion.camera" -> {
                try {
                    val systemProperties = XposedHelpers.findClass(
                        "android.os.SystemProperties",
                        lpparam.classLoader
                    )

                    XposedHelpers.findAndHookMethod(
                        systemProperties,
                        "get",
                        String::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val key = param.args[0] as String
                                Log.i(TAG, "SystemProperties.get($key) called")

                                if (key.contains("ro.tran.version", ignoreCase = true)) {
                                    Log.i(TAG, "Spoofing SystemProperties.get($key) -> 8.1.0")
                                    param.result = "8.1.0"
                                }
                            }
                        }
                    )

                    XposedBridge.log("$TAG: Successfully hooked SystemProperties.get()")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Failed to hook SystemProperties.get(): ${e.message}")
                }
            }
        }
    }
}