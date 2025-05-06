package com.mutz.fingerprintbypass

import android.util.Log
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.hookClass
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.highcapable.yukihookapi.hook.xposed.YukiHookModule
import com.highcapable.yukihookapi.hook.xposed.YukiHookHelper

@InjectYukiHookWithXposed
class HookEntry : YukiHookModule(), IYukiHookXposedInit {

   override fun onHook() {
    YukiHookHelper.encase(config = {
        name("android")
        onHook {
            hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl") {
                method {
                    name = "isFpHardwareDetected"
                }.replaceToTrue {
                    Log.i("FingerprintBypass", "isFpHardwareDetected() bypassed!")
                }
            }
        }
    }) // <-- ini penutup encase
}
