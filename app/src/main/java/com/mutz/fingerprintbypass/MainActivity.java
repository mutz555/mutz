package com.mutz.fingerprintbypass;

import android.util.Log;

import com.highcapable.yukihookapi.hook.factory.YukiHookHelper;
import com.highcapable.yukihookapi.template.YukiHookModule;

public class MainActivity extends YukiHookModule {
    @Override
    public void onHook() {
        onApp("android", hook -> {
            hook.hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl", cls -> {
                cls.method("isFpHardwareDetected")
                   .replaceToTrue(param -> {
                       Log.i("FingerprintBypass", "isFpHardwareDetected() dibypass ke true");
                   });
            });
        });
    }
}