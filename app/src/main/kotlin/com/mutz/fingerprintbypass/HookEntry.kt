package com.mutz.fingerprintbypass

import android.util.Log // Pastikan android.util.Log diimpor jika belum
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement // Tambahkan impor ini
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: handleLoadPackage for package: ${lpparam.packageName}") // Sedikit modifikasi log untuk kejelasan

        if (lpparam.packageName == "android") {
            XposedBridge.log("$TAG: Processing hooks for 'android' package")
            hookGetSensorPropForInstance(lpparam)
            hookIsFpHardwareDetected(lpparam) // Panggil fungsi hook yang baru
        }
        // Anda bisa menambahkan case lain di sini jika diperlukan, misalnya:
        // else if (lpparam.packageName == "com.transsion.camera") {
        //     // Panggil hook yang relevan untuk com.transsion.camera dari file referensi Anda
        // }
    }

    // Fungsi hook untuk isFpHardwareDetected (diambil dari referensi Anda)
    private fun hookIsFpHardwareDetected(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook isFpHardwareDetected")
            val clazz = XposedHelpers.findClass(
                "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl", // Nama kelas dari referensi
                lpparam.classLoader
            )
            XposedBridge.log("$TAG: Found class: ${clazz.name} for isFpHardwareDetected hook")

            XposedHelpers.findAndHookMethod(
                clazz, // Menggunakan objek kelas yang sudah ditemukan
                "isFpHardwareDetected", // Nama metode
                // Tidak ada parameter untuk metode isFpHardwareDetected
                object : XC_MethodReplacement() { // Menggunakan XC_MethodReplacement seperti di referensi
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Log.i(TAG, "Bypassed isFpHardwareDetected! Returning true.") // Menggunakan Log.i seperti di referensi
                        XposedBridge.log("$TAG: Executing replaced isFpHardwareDetected, returning true.")
                        return true // Selalu mengembalikan true untuk bypass
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked isFpHardwareDetected.")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook isFpHardwareDetected: ${e.message}")
            XposedBridge.log(e) // Cetak seluruh stack trace error
        }
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook getSensorPropForInstance")

            XposedHelpers.findAndHookMethod(
                "android.hardware.fingerprint.FingerprintSensorConfigurations",
                lpparam.classLoader,
                "getSensorPropForInstance",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        Log.d(TAG, "getSensorPropForInstance() called with instance: $instance")
                        XposedBridge.log("$TAG: Before getSensorPropForInstance for instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        val result = param.result
                        XposedBridge.log("$TAG: After getSensorPropForInstance for instance: $instance, result type: ${result?.javaClass?.name}")

                        if (result == null) {
                            Log.d(TAG, "getSensorPropForInstance() returned null for instance: $instance")
                            return
                        }

                        if (result is Array<*>) {
                            val propsArray = result
                            XposedBridge.log("$TAG: Result is an Array. Size: ${propsArray.size}")

                            if (propsArray.isEmpty()) {
                                Log.d(TAG, "$TAG: getSensorPropForInstance result array is empty.")
                            }

                            propsArray.forEachIndexed { index, propObject: Any? ->
                                if (propObject == null) {
                                    XposedBridge.log("$TAG: Prop[$index]: is null")
                                    return@forEachIndexed
                                }
                                
                                XposedBridge.log("$TAG: Processing Prop[$index]: ${propObject.javaClass.name}")

                                try {
                                    val sensorType = XposedHelpers.getObjectField(propObject, "sensorType")
                                    Log.d(TAG, "  Prop[$index]: sensorType = $sensorType")

                                    val commonProps = XposedHelpers.getObjectField(propObject, "commonProps")
                                    if (commonProps != null) {
                                        val sensorId = XposedHelpers.getObjectField(commonProps, "sensorId")
                                        val sensorStrength = XposedHelpers.getObjectField(commonProps, "sensorStrength")
                                        Log.d(TAG, "  Prop[$index]: commonProps.sensorId = $sensorId")
                                        Log.d(TAG, "  Prop[$index]: commonProps.sensorStrength = $sensorStrength")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: commonProps = null")
                                    }

                                    val sensorLocations = XposedHelpers.getObjectField(propObject, "sensorLocations")
                                    if (sensorLocations != null) {
                                        var sensorLocationsSize = -1
                                        when {
                                            sensorLocations is Collection<*> -> {
                                                sensorLocationsSize = sensorLocations.size
                                            }
                                            sensorLocations.javaClass.isArray -> {
                                                sensorLocationsSize = java.lang.reflect.Array.getLength(sensorLocations)
                                            }
                                            else -> {
                                                try {
                                                    sensorLocationsSize = XposedHelpers.getIntField(sensorLocations, "size")
                                                } catch (e: NoSuchFieldError) {
                                                     Log.w(TAG, "  Prop[$index]: sensorLocations.size field not found, and not a known collection/array. Type: ${sensorLocations.javaClass.name}")
                                                }
                                            }
                                        }
                                        Log.d(TAG, "  Prop[$index]: sensorLocations (type: ${sensorLocations.javaClass.name}), size = $sensorLocationsSize")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: sensorLocations = null")
                                    }

                                    val supportsNavigationGestures = XposedHelpers.getBooleanField(propObject, "supportsNavigationGestures")
                                    val supportsDetectInteraction = XposedHelpers.getBooleanField(propObject, "supportsDetectInteraction")
                                    val halHandlesDisplayTouches = XposedHelpers.getBooleanField(propObject, "halHandlesDisplayTouches")
                                    val halControlsIllumination = XposedHelpers.getBooleanField(propObject, "halControlsIllumination")
                                    Log.d(TAG, "  Prop[$index]: supportsNavigationGestures = $supportsNavigationGestures")
                                    Log.d(TAG, "  Prop[$index]: supportsDetectInteraction = $supportsDetectInteraction")
                                    Log.d(TAG, "  Prop[$index]: halHandlesDisplayTouches = $halHandlesDisplayTouches")
                                    Log.d(TAG, "  Prop[$index]: halControlsIllumination = $halControlsIllumination")

                                    val touchDetectionParameters = XposedHelpers.getObjectField(propObject, "touchDetectionParameters")
                                    if (touchDetectionParameters != null) {
                                        Log.d(TAG, "  Prop[$index]: touchDetectionParameters = $touchDetectionParameters")
                                    } else {
                                        Log.d(TAG, "  Prop[$index]: touchDetectionParameters = null")
                                    }

                                } catch (e: Throwable) {
                                    XposedBridge.log("$TAG: Error processing fields for Prop[$index] (${propObject.javaClass.name}): ${e.message}")
                                    XposedBridge.log(e)
                                }
                            }

                            if (propsArray.isEmpty()) {
                                Log.d(TAG, "  Data potentially from HAL (array was empty)")
                            } else {
                                Log.d(TAG, "  Data potentially from mSensorPropsMap (array had ${propsArray.size} elements)")
                            }

                        } else {
                            Log.w(TAG, "getSensorPropForInstance() returned non-array result type: ${result.javaClass.name}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked getSensorPropForInstance method (using findAndHookMethod)")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook getSensorPropForInstance: ${e.message}")
            XposedBridge.log(e)
        }
    }
}