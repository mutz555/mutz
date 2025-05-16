package com.mutz.fingerprintbypass

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import android.hardware.biometrics.fingerprint.SensorProps // Pastikan ini benar!

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: handleLoadPackage: ${lpparam.packageName}")
        if (lpparam.packageName == "android") {
            hookGetSensorPropForInstance(lpparam)
        }
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: hookGetSensorPropForInstance called")
            XposedBridge.log("$TAG: ClassLoader: ${lpparam.classLoader}")

            val clazz = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintSensorConfigurations",
                lpparam.classLoader
            )
            XposedBridge.log("$TAG: FingerprintSensorConfigurations class found: $clazz")

            XposedHelpers.findAndHookMethod(
                clazz,
                "getSensorPropForInstance",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        Log.d(TAG, "getSensorPropForInstance() called with instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        val result = param.result
                        XposedBridge.log("$TAG: getSensorPropForInstance result: $result")

                        if (result is Array<*>) {
                            @Suppress("UNCHECKED_CAST") // Tambahkan ini
                            val props = result as Array<android.hardware.biometrics.fingerprint.SensorProps>

                            XposedBridge.log("$TAG: props class: ${props.javaClass.name}") // Tambahkan ini
                            XposedBridge.log("$TAG: props size: ${props.size}")

                            Log.d(TAG, "getSensorPropForInstance() returned ${props.size} props for instance: $instance")
                            props.forEachIndexed { index, prop ->
                                Log.d(TAG, "  Prop[$index]: sensorType = ${prop.sensorType}")

                                if (prop.commonProps != null) {
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorId = ${prop.commonProps.sensorId}")
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorStrength = ${prop.commonProps.sensorStrength}")
                                } else {
                                    Log.d(TAG, "  Prop[$index]: commonProps = null")
                                }

                                if (prop.sensorLocations != null) {
                                    Log.d(TAG, "  Prop[$index]: sensorLocations.size = ${prop.sensorLocations.size}")
                                } else {
                                    Log.d(TAG, "  Prop[$index]: sensorLocations = null")
                                }

                                Log.d(TAG, "  Prop[$index]: supportsNavigationGestures = ${prop.supportsNavigationGestures}")
                                Log.d(TAG, "  Prop[$index]: supportsDetectInteraction = ${prop.supportsDetectInteraction}")
                                Log.d(TAG, "  Prop[$index]: halHandlesDisplayTouches = ${prop.halHandlesDisplayTouches}")
                                Log.d(TAG, "  Prop[$index]: halControlsIllumination = ${prop.halControlsIllumination}")

                                if (prop.touchDetectionParameters != null) {
                                    Log.d(TAG, "  Prop[$index]: touchDetectionParameters = ${prop.touchDetectionParameters}")
                                } else {
                                    Log.d(TAG, "  Prop[$index]: touchDetectionParameters = null")
                                }

                                // Log other relevant fields from prop
                            }

                            // Log where the data came from (mSensorPropsMap or HAL)
                            if (props.isEmpty()) {
                                Log.d(TAG, "  Data was retrieved from HAL")
                            } else {
                                Log.d(TAG, "  Data was retrieved from mSensorPropsMap")
                            }

                        } else if (result == null) {
                            Log.d(TAG, "getSensorPropForInstance() returned null for instance: $instance")
                        } else {
                            Log.w(TAG, "getSensorPropForInstance() returned unexpected result type: ${result.javaClass.name}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: getSensorPropForInstance hook OK")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getSensorPropForInstance hook failed: ${e.message}")
            XposedBridge.log("$TAG: Error: $e")
        }
    }
}
