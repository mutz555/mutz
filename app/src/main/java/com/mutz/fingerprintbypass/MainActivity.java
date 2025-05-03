package com.mutz.fingerprintbypass

import android.app.Application;
import android.util.Log;

import com.highcapable.yukihookapi.hook.factory.HookFactory;
import com.highcapable.yukihookapi.hook.factory.YukiHookHelper;
import com.highcapable.yukihookapi.hook.type.HookParam;
import com.highcapable.yukihookapi.template.YukiHookModule;

public class MainActivity extends YukiHookModule {

    @Override
    public void onHook() {
        YukiHookHelper.encase(config -> {
            config.name("android"); // Target sistem server
            config.onHook(() -> {
                config.hookClass("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl", hook -> {
                    hook.method("isFpHardwareDetected")
                        .replaceToTrue(param -> {
                            Log.i("FingerprintBypass", "isFpHardwareDetected() telah dibypass ke true");
                        });
                });
            });
        });
    }
}