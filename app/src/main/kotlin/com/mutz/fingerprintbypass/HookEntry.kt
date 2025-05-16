package com.mutz.fingerprintbypass

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {
    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android") {
            hookGetSensorPropForInstance(lpparam)
        }
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintSensorConfigurations",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                clazz,
                "getSensorPropForInstance",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as String
                        Log.d(TAG, "getSensorPropForInstance() called with instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as String
                        val props = param.result as? Array<android.hardware.biometrics.fingerprint.SensorProps>

                        if (props == null) {
                            Log.d(TAG, "getSensorPropForInstance() returned null for instance: $instance")
                        } else {
                            Log.d(TAG, "getSensorPropForInstance() returned ${props.size} props for instance: $instance")
                            props.forEachIndexed { index, prop ->
                                Log.d(TAG, "  Prop[$index]: sensorType = ${prop.sensorType}")
                                if (prop.commonProps != null) {
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorId = ${prop.commonProps.sensorId}")
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorStrength = ${prop.commonProps.sensorStrength}")
                                } else {
                                    Log.d(TAG, "  Prop[$index]: commonProps = null")
                                }
                                // Log other relevant fields from prop
                            }
                        }

                        // Log where the data came from (mSensorPropsMap or HAL)
                        if (props == null || props.isEmpty()) {
                            Log.d(TAG, "  Data was retrieved from HAL")
                        } else {
                            Log.d(TAG, "  Data was retrieved from mSensorPropsMap")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: getSensorPropForInstance hook OK")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getSensorPropForInstance hook failed: ${e.message}")
        }
    }
}
