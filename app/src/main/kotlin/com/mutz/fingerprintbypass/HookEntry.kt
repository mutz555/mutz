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
        XposedBridge.log("$TAG: handleLoadPackage: ${lpparam.packageName}")
        if (lpparam.packageName == "android") {
            hookGetSensorPropForInstance(lpparam)
        }
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: hookGetSensorPropForInstance called")

            val fingerprintSensorConfigurationsClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintSensorConfigurations",
                lpparam.classLoader
            )
            XposedBridge.log("$TAG: fingerprintSensorConfigurationsClass: $fingerprintSensorConfigurationsClass")

            val getSensorPropForInstanceMethod = XposedHelpers.findMethod(
                fingerprintSensorConfigurationsClass,
                "getSensorPropForInstance",
                String::class.java
            )
            XposedBridge.log("$TAG: getSensorPropForInstanceMethod: $getSensorPropForInstanceMethod")

            XposedHelpers.hookMethod(
                getSensorPropForInstanceMethod,
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
                            try {
                                val sensorPropsClass = XposedHelpers.findClass(
                                    "android.hardware.biometrics.fingerprint.SensorProps",
                                    lpparam.classLoader
                                )
                                XposedBridge.log("$TAG: sensorPropsClass: $sensorPropsClass")

                                @Suppress("UNCHECKED_CAST")
                                val props = result as Array<*> // Treat as Array<*> initially
                                val convertedProps = props.map { prop ->
                                    XposedHelpers.newInstance(sensorPropsClass, prop)
                                } as List<*> // Cast to List<*>

                                XposedBridge.log("$TAG: convertedProps size: ${convertedProps.size}")

                                convertedProps.forEachIndexed { index, prop ->
                                    XposedBridge.log("$TAG: Prop[$index]: $prop") // Log the prop object itself

                                    // Access fields using XposedHelpers.getObjectField
                                    val sensorType = XposedHelpers.getObjectField(prop, "sensorType")
                                    Log.d(TAG, "  Prop[$index]: sensorType = $sensorType")

                                    val commonProps = XposedHelpers.getObjectField(prop, "commonProps")
                                    if (commonProps != null) {
                                        val sensorId = XposedHelpers.getObjectField(commonProps, "sensorId")
                                        val sensorStrength = XposedHelpers.getObjectField(commonProps, "sensorStrength")
                                        Log.d(TAG, "  Prop[$index]: commonProps.sensorId = $sensorId")
                                        Log.d(TAG, "  Prop[$index]: commonProps.sensorStrength = $sensorStrength")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: commonProps = null")
                                    }

                                    val sensorLocations = XposedHelpers.getObjectField(prop, "sensorLocations")
                                    if (sensorLocations != null) {
                                        val sensorLocationsSize = XposedHelpers.getObjectField(sensorLocations, "size") // Assuming size is a field
                                        Log.d(TAG, "  Prop[$index]: sensorLocations.size = $sensorLocationsSize")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: sensorLocations = null")
                                    }

                                    val supportsNavigationGestures = XposedHelpers.getObjectField(prop, "supportsNavigationGestures")
                                    val supportsDetectInteraction = XposedHelpers.getObjectField(prop, "supportsDetectInteraction")
                                    val halHandlesDisplayTouches = XposedHelpers.getObjectField(prop, "halHandlesDisplayTouches")
                                    val halControlsIllumination = XposedHelpers.getObjectField(prop, "halControlsIllumination")
                                    Log.d(TAG, "  Prop[$index]: supportsNavigationGestures = $supportsNavigationGestures")
                                    Log.d(TAG, "  Prop[$index]: supportsDetectInteraction = $supportsDetectInteraction")
                                    Log.d(TAG, "  Prop[$index]: halHandlesDisplayTouches = $halHandlesDisplayTouches")
                                    Log.d(TAG, "  Prop[$index]: halControlsIllumination = $halControlsIllumination")

                                    val touchDetectionParameters = XposedHelpers.getObjectField(prop, "touchDetectionParameters")
                                    if (touchDetectionParameters != null) {
                                        Log.d(TAG, "  Prop[$index]: touchDetectionParameters = $touchDetectionParameters")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: touchDetectionParameters = null")
                                    }

                                    // Log other relevant fields from prop
                                }

                                // Log where the data came from (mSensorPropsMap or HAL)
                                if (convertedProps.isEmpty()) {
                                    Log.d(TAG, "  Data was retrieved from HAL")
                                } else {
                                    Log.d(TAG, "  Data was retrieved from mSensorPropsMap")
                                }

                            } catch (e: Throwable) {
                                XposedBridge.log("$TAG: Error processing props: ${e.message}")
                                XposedBridge.log("$TAG: Error: $e")
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
