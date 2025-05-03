package com.mutz.fingerprintbypass.hooks

import android.util.Log
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.xposed.proxy.YukiHookBridge
import com.highcapable.yukihookapi.hook.xposed.proxy.YukiHookModule

class FingerprintBypassHook : YukiHookModule() {
    override fun onHook() = hookPackage {
        name = "android" // Target system server

        onHook {
            // Kelas target
            hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl") {
                
                // Method target
                hookMethod("isFpHardwareDetected") {
                    // Gantikan hasil dengan true
                    replaceToTrue()

                    afterHook {
                        Log.i("FingerprintBypass", "isFpHardwareDetected() telah dibypass ke true")
                    }
                }
            }
        }
    }
}