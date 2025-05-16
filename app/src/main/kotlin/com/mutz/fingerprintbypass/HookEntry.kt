package com.mutz.fingerprintbypass

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers // Pastikan ini ada!
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Array // Untuk Array.getLength

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: handleLoadPackage: ${lpparam.packageName}")
        if (lpparam.packageName == "android") {
            // Pertimbangkan untuk hook kelas yang lebih spesifik jika memungkinkan,
            // "android" sangat luas dan dapat memperlambat startup.
            // Namun, untuk FingerprintManager atau BiometricPrompt, ini mungkin diperlukan.
            hookGetSensorPropForInstance(lpparam)
        }
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook getSensorPropForInstance") // (modified log)

            val fingerprintSensorConfigurationsClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintSensorConfigurations", // Pastikan kelas ini masih ada di versi Android target
                lpparam.classLoader
            )
            XposedBridge.log("$TAG: Found FingerprintSensorConfigurations class: $fingerprintSensorConfigurationsClass")

            val getSensorPropForInstanceMethod = XposedHelpers.findMethodExact( // Gunakan findMethodExact untuk lebih presisi
                fingerprintSensorConfigurationsClass,
                "getSensorPropForInstance",
                String::class.java // Parameter metode adalah String (instanceId)
            )
            XposedBridge.log("$TAG: Found getSensorPropForInstance method: $getSensorPropForInstanceMethod")

            XposedHelpers.hookMethod(
                getSensorPropForInstanceMethod, // (method to hook)
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String // 
                        Log.d(TAG, "getSensorPropForInstance() called with instance: $instance")
                        XposedBridge.log("$TAG: Before getSensorPropForInstance for instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String // 
                        val result = param.result
                        XposedBridge.log("$TAG: After getSensorPropForInstance for instance: $instance, result type: ${result?.javaClass?.name}")

                        if (result == null) {
                            Log.d(TAG, "getSensorPropForInstance() returned null for instance: $instance") // 
                            return
                        }

                        if (result !is Array<*>) {
                            Log.w(TAG, "getSensorPropForInstance() returned unexpected result type: ${result.javaClass.name}") // 
                            return
                        }
                        
                        XposedBridge.log("$TAG: Result is an Array<*>. Size: ${result.size}")

                        // Tidak perlu mencari 'sensorPropsClass' jika 'result' sudah berisi objek-objek yang diinginkan.
                        // Jika Anda perlu memvalidasi tipe elemen, Anda bisa melakukannya di dalam loop.
                        // val sensorPropsClass = XposedHelpers.findClass(
                        //     "android.hardware.biometrics.fingerprint.SensorProps", // 
                        //     lpparam.classLoader
                        // )
                        // XposedBridge.log("$TAG: sensorPropsClass: $sensorPropsClass")

                        // KEMUNGKINAN PERBAIKAN UTAMA:
                        // Jika 'result' sudah merupakan array dari objek properti sensor yang sebenarnya,
                        // Anda tidak perlu menggunakan XposedHelpers.newInstance.
                        // Langsung iterasi 'result' dan akses field dari setiap elemen.
                        val propsArray = result as Array<Any?> // Kita asumsikan elemennya adalah Any?

                        if (propsArray.isEmpty()) {
                            Log.d(TAG, "$TAG: getSensorPropForInstance result array is empty.")
                        }

                        propsArray.forEachIndexed { index, propObject ->
                            if (propObject == null) {
                                XposedBridge.log("$TAG: Prop[$index]: is null")
                                return@forEachIndexed // Lanjut ke elemen berikutnya
                            }
                            
                            XposedBridge.log("$TAG: Processing Prop[$index]: ${propObject.javaClass.name}")

                            try {
                                // Akses fields menggunakan XposedHelpers.getObjectField dari propObject
                                val sensorType = XposedHelpers.getObjectField(propObject, "sensorType") // 
                                Log.d(TAG, "  Prop[$index]: sensorType = $sensorType") // 

                                val commonProps = XposedHelpers.getObjectField(propObject, "commonProps")
                                if (commonProps != null) {
                                    val sensorId = XposedHelpers.getObjectField(commonProps, "sensorId") // 
                                    val sensorStrength = XposedHelpers.getObjectField(commonProps, "sensorStrength")
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorId = $sensorId") // 
                                    Log.d(TAG, "  Prop[$index]: commonProps.sensorStrength = $sensorStrength")
                                } else {
                                    Log.d(TAG, "  Prop[$index]: commonProps = null") // 
                                }

                                val sensorLocations = XposedHelpers.getObjectField(propObject, "sensorLocations") // 
                                if (sensorLocations != null) {
                                    var sensorLocationsSize = -1
                                    // PERBAIKAN untuk mendapatkan ukuran sensorLocations:
                                    when {
                                        sensorLocations is Collection<*> -> {
                                            sensorLocationsSize = sensorLocations.size
                                             // Atau jika perlu refleksi: XposedHelpers.callMethod(sensorLocations, "size") as Int
                                        }
                                        sensorLocations.javaClass.isArray -> {
                                            sensorLocationsSize = Array.getLength(sensorLocations)
                                        }
                                        else -> {
                                            // Coba sebagai field jika memang ada (kurang umum untuk koleksi)
                                            try {
                                                sensorLocationsSize = XposedHelpers.getIntField(sensorLocations, "size")
                                            } catch (e: NoSuchFieldError) {
                                                 Log.w(TAG, "  Prop[$index]: sensorLocations.size field not found, and not a known collection/array. Type: ${sensorLocations.javaClass.name}")
                                            }
                                        }
                                    }
                                    Log.d(TAG, "  Prop[$index]: sensorLocations (type: ${sensorLocations.javaClass.name}), size = $sensorLocationsSize") // (modified log)
                                } else {
                                    Log.d(TAG, "  Prop[$index]: sensorLocations = null") // 
                                }

                                val supportsNavigationGestures = XposedHelpers.getBooleanField(propObject, "supportsNavigationGestures") // (asumsi boolean)
                                val supportsDetectInteraction = XposedHelpers.getBooleanField(propObject, "supportsDetectInteraction")
                                val halHandlesDisplayTouches = XposedHelpers.getBooleanField(propObject, "halHandlesDisplayTouches") // (asumsi boolean)
                                val halControlsIllumination = XposedHelpers.getBooleanField(propObject, "halControlsIllumination")
                                Log.d(TAG, "  Prop[$index]: supportsNavigationGestures = $supportsNavigationGestures")
                                Log.d(TAG, "  Prop[$index]: supportsDetectInteraction = $supportsDetectInteraction") // 
                                Log.d(TAG, "  Prop[$index]: halHandlesDisplayTouches = $halHandlesDisplayTouches")
                                Log.d(TAG, "  Prop[$index]: halControlsIllumination = $halControlsIllumination") // 

                                val touchDetectionParameters = XposedHelpers.getObjectField(propObject, "touchDetectionParameters")
                                if (touchDetectionParameters != null) {
                                    Log.d(TAG, "  Prop[$index]: touchDetectionParameters = $touchDetectionParameters") // 
                                } else {
                                    Log.d(TAG, "  Prop[$index]: touchDetectionParameters = null") // 
                                }
                                // Log other fields if needed

                            } catch (e: Throwable) {
                                XposedBridge.log("$TAG: Error processing fields for Prop[$index] (${propObject.javaClass.name}): ${e.message}") // 
                                XposedBridge.log(e) // Log stacktrace
                            }
                        }

                        // Logika untuk sumber data , 
                        // Logika ini mungkin perlu disesuaikan berdasarkan pemahaman Anda tentang bagaimana
                        // metode asli menentukan sumber datanya. Hanya memeriksa apakah array kosong mungkin tidak cukup.
                        if (propsArray.isEmpty()) {
                            Log.d(TAG, "  Data potentially from HAL (array was empty)")
                        } else {
                            Log.d(TAG, "  Data potentially from mSensorPropsMap (array had ${propsArray.size} elements)")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked getSensorPropForInstance method") // (modified log)

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook getSensorPropForInstance: ${e.message}")
            XposedBridge.log(e) // Log stacktrace untuk debugging
        }
    }
}