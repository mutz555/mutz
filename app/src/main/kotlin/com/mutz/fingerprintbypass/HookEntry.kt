package com.mutz.fingerprintbypass

import android.os.Build
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

    private val propsToSpoof = mapOf(
        "ro.build.product" to "Infinix_X6837",
        "ro.product.device" to "Infinix-X6837",
        "ro.product.model" to "Infinix X6837",
        "ro.product.name" to "Infinix_X6837",
        "ro.product.brand" to "Infinix",
        "ro.product.manufacturer" to "Infinix",
        "ro.board.platform" to "mt6789",
        "ro.build.fingerprint" to "Infinix/X6837-GL/Infinix-X6837:14/TP1A.220624.014/240221:user/release-keys"
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                // Hook fingerprint hardware detection
                try {
                    val clazz = XposedHelpers.findClass(
                        "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl",
                        lpparam.classLoader
                    )
                    XposedHelpers.findAndHookMethod(
                        clazz,
                        "isFpHardwareDetected",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any {
                                Log.i(TAG, "Bypassed isFpHardwareDetected!")
                                return true
                            }
                        }
                    )
                    XposedBridge.log("$TAG: Fingerprint hook OK")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Fingerprint hook failed: ${e.message}")
                }
            }

            "com.transsion.camera" -> {
                // Hook Build fields
                try {
                    listOf(
                        "DEVICE" to "Infinix-X6837",
                        "MODEL" to "Infinix X6837",
                        "PRODUCT" to "Infinix_X6837",
                        "BRAND" to "Infinix",
                        "MANUFACTURER" to "Infinix",
                        "FINGERPRINT" to "Infinix/X6837-GL/Infinix-X6837:14/TP1A.220624.014/240221:user/release-keys"
                    ).forEach { (field, value) ->
                        XposedHelpers.setStaticObjectField(Build::class.java, field, value)
                        Log.i(TAG, "Spoofed Build.$field -> $value")
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Build spoof failed: ${e.message}")
                }

                // Hook SystemProperties.get
                try {
                    XposedHelpers.findAndHookMethod(
                        "android.os.SystemProperties",
                        lpparam.classLoader,
                        "get",
                        String::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val key = param.args[0] as String
                                if (propsToSpoof.containsKey(key)) {
                                    param.result = propsToSpoof[key]
                                    Log.i(TAG, "Spoofed SystemProperties.get($key) -> ${propsToSpoof[key]}")
                                }
                            }
                        }
                    )
                    XposedBridge.log("$TAG: SystemProperties spoof OK")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: SystemProperties hook failed: ${e.message}")
                }

                // Optional camera characteristics log/debug
                try {
                    XposedHelpers.findAndHookMethod(
                        "android.hardware.camera2.CameraCharacteristics",
                        lpparam.classLoader,
                        "get",
                        Object::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val key = param.args[0]
                                val value = param.result
                                Log.i(TAG, "CameraCharacteristics.get($key) = ${value?.javaClass?.name ?: "null"}")
                            }
                        }
                    )
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: CameraCharacteristics hook failed: ${e.message}")
                }
            }
        }
    }
}