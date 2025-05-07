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

            public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.transsion.camera")) return;

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CameraCharacteristics",
            lpparam.classLoader,
            "get",
            Object.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object key = param.args[0];
                    Object value = param.getResult();

                    Log.d("CAM_HOOK", "Key: " + key + " (" + (key != null ? key.getClass().getName() : "null") + ")");
                    Log.d("CAM_HOOK", "Value: " + (value != null ? value.getClass().getName() : "null"));

                    // Ganti Boolean dengan dummy int[] kalau perlu
                    if (value instanceof Boolean) {
                        Log.w("CAM_HOOK", "Substituting Boolean with dummy int[] to prevent crash");
                        param.setResult(new int[]{0});
                    }
                }
            }
        );
    }
}