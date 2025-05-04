package com.mutz.fingerprintbypass;

import android.util.Log;
import com.highcapable.yukihookapi.hook.factory.YukiHookHelper;
import com.highcapable.yukihookapi.template.YukiHookModule;

public class MainActivity extends YukiHookModule {
    @Override
    public void onHook() {
        YukiHookHelper.encase(config -> {
            config.name("android");
            config.onHook(() -> {
                config.hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl", hook -> {
                    hook.method("isHardwareDetected")
                        .replaceToTrue(param -> {
                            Log.i("FingerprintBypass", "isHardwareDetected() dibypass ke true");
                        });
                });
            });
        });
    }
}