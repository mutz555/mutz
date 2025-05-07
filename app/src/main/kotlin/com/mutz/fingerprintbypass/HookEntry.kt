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
                    XposedHelpers.findAndHookMethod(
                        "android.hardware.camera2.CameraCharacteristics",
                        lpparam.classLoader,
                        "get",
                        Object::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val key = param.args[0]
                                val value = param.result
                                
                                Log.d("CAM_HOOK", "Key: $key (${if (key != null) key.javaClass.name else "null"})")
                                Log.d("CAM_HOOK", "Value: ${if (value != null) value.javaClass.name else "null"}")
                                
                                // Ganti Boolean dengan dummy int[] kalau perlu
                                if (value is Boolean) {
                                    Log.w("CAM_HOOK", "Substituting Boolean with dummy int[] to prevent crash")
                                    param.result = intArrayOf(0)
                                }
                            }
                        }
                    )
                    
                    XposedBridge.log("$TAG: Successfully hooked CameraCharacteristics.get()")
                } catch (t: Throwable) {
                    XposedBridge.log("$TAG: Error hooking camera: ${t.message}")
                }
            }
        }
    }
}