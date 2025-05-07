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
        private val spoofProps = mapOf(
            "ro.product.model" to "Infinix HOT 40 Pro",
            "ro.product.manufacturer" to "Infinix",
            "ro.product.device" to "X6837",
            "ro.product.name" to "Infinix-X6837",
            "ro.build.product" to "Infinix-X6837",
            "ro.product.brand" to "Infinix",
            "ro.build.fingerprint" to "Infinix/Infinix-X6837/INFINIX:14/UKQ-14.0.0/240411:user/release-keys"
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                try {
                    val clazz = XposedHelpers.findClass(
                        "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl",
                        lpparam.classLoader
                    )

                    XposedHelpers.findAndHookMethod(
                        clazz, "isFpHardwareDetected",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any {
                                Log.i(TAG, "isFpHardwareDetected() bypassed!")
                                return true
                            }
                        }
                    )

                    XposedBridge.log("$TAG: Successfully hooked fingerprint service")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Failed fingerprint hook: ${e.message}")
                }
            }

            "com.transsion.camera" -> {
                try {
                    XposedHelpers.findAndHookMethod(
                        "android.os.SystemProperties",
                        lpparam.classLoader,
                        "get",
                        String::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val key = param.args[0] as String
                                if (spoofProps.containsKey(key)) {
                                    param.result = spoofProps[key]
                                    Log.i(TAG, "Spoofed $key -> ${spoofProps[key]}")
                                }
                            }
                        }
                    )

                    XposedBridge.log("$TAG: Spoofed SystemProperties for Transsion camera")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Failed spoofing camera props: ${e.message}")
                }
            }
        }
    }
}