package com.mutz.fingerprintbypass;

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.hook
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.logger
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.java.ClassType
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
public class MainActivity extends YukiHookModule {
    @Override
    public void onHook() {
        YukiHookHelper.encase(config -> {
            config.name("android");
            config.onHook(() -> {
                config.hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl", hook -> {
                    hook.method("isFpHardwareDetected")
                        .replaceToTrue(param -> {
                            Log.i("FingerprintBypass", "isHardwareDetected() dibypass ke true");
                        });
                });
            });
        });
    }
}
