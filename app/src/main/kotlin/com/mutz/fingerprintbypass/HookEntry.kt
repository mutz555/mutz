package com.mutz.fingerprintbypass

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
                    // Hook CameraManager.getCameraIdList
                    XposedHelpers.findAndHookMethod(
                        CameraManager::class.java,
                        "getCameraIdList",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val result = param.result as? Array<String>
                                Log.i(TAG, "getCameraIdList() hooked: ${result?.contentToString()}")
                            }
                        }
                    )

                    // Hook CameraCharacteristics.get
                    XposedHelpers.findAndHookMethod(
                        CameraCharacteristics::class.java,
                        "get",
                        CameraCharacteristics.Key::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val key = param.args[0]
                                Log.i(TAG, "CameraCharacteristics.get() called with key: $key")

                                val keyStr = key.toString()
                                if (
                                    keyStr.contains("available", ignoreCase = true) ||
                                    keyStr.contains("cameraid.role", ignoreCase = true)
                                ) {
                                    Log.i(TAG, "Bypassing CameraCharacteristics.get() with TRUE for key: $key")
                                    param.result = true
                                }
                            }
                        }
                    )

                    XposedBridge.log("$TAG: CameraManager & CameraCharacteristics hooked in com.transsion.camera")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG: Error hooking camera API: ${e.message}")
                }
            }
        }
    }
}