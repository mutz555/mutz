// File: HookEntry.kt (versi Anda yang akan diperbaiki)
package com.mutz.fingerprintbypass

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers // Pastikan ini ada dan dependensi benar
import de.robv.android.xposed.callbacks.XC_LoadPackage
// import java.lang.reflect.Array // Tidak lagi dibutuhkan jika kita menyederhanakan

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FingerprintBypass"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: handleLoadPackage: ${lpparam.packageName}")
        if (lpparam.packageName == "android") {
            hookGetSensorPropForInstance(lpparam)
        }
        // Anda bisa menambahkan hook lain di sini dari file referensi jika perlu
        // seperti hook untuk "com.transsion.camera"
    }

    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook getSensorPropForInstance")

            // Menggunakan findAndHookMethod untuk konsistensi dengan file referensi
            // dan untuk kemungkinan mengatasi "Unresolved reference: hookMethod"
            // Kita perlu menyediakan nama kelas, classloader, nama method, tipe parameter, dan callback hook.
            XposedHelpers.findAndHookMethod(
                "android.hardware.fingerprint.FingerprintSensorConfigurations", // Nama kelas sebagai String
                lpparam.classLoader,
                "getSensorPropForInstance", // Nama method
                String::class.java, // Tipe parameter (instanceId)
                object : XC_MethodHook() { // Callback hook
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        Log.d(TAG, "getSensorPropForInstance() called with instance: $instance")
                        XposedBridge.log("$TAG: Before getSensorPropForInstance for instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        val result = param.result // Ini adalah `Any?`
                        XposedBridge.log("$TAG: After getSensorPropForInstance for instance: $instance, result type: ${result?.javaClass?.name}")

                        if (result == null) {
                            Log.d(TAG, "getSensorPropForInstance() returned null for instance: $instance")
                            return
                        }

                        // Penanganan Array - ini bagian yang paling mungkin menyebabkan error build terkait tipe
                        if (result is Array<*>) { // Memeriksa apakah result adalah Array
                            val propsArray = result // Kotlin smart cast: result sekarang dikenal sebagai Array<*>
                            XposedBridge.log("$TAG: Result is an Array. Size: ${propsArray.size}")

                            if (propsArray.isEmpty()) {
                                Log.d(TAG, "$TAG: getSensorPropForInstance result array is empty.")
                            }

                            // Memberikan tipe eksplisit untuk parameter lambda
                            propsArray.forEachIndexed { index, propObject: Any? ->
                                if (propObject == null) {
                                    XposedBridge.log("$TAG: Prop[$index]: is null")
                                    return@forEachIndexed // Lanjut ke elemen berikutnya
                                }
                                
                                XposedBridge.log("$TAG: Processing Prop[$index]: ${propObject.javaClass.name}")

                                try {
                                    // Akses fields menggunakan XposedHelpers.getObjectField dari propObject
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
                                            // Periksa apakah ini benar-benar array Java primitif atau objek
                                            // Class.isArray() akan memberi tahu Anda jika itu adalah array
                                            sensorLocations.javaClass.isArray -> {
                                                 // Jika ini adalah array, Anda perlu menggunakan java.lang.reflect.Array.getLength
                                                sensorLocationsSize = java.lang.reflect.Array.getLength(sensorLocations)
                                            }
                                            else -> {
                                                try {
                                                    // Mencoba mengambil field 'size' secara langsung (jarang untuk koleksi standar)
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

                                    // Pastikan Anda menggunakan getBooleanField, getIntField, dll., jika tipe field diketahui
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
                                    XposedBridge.log(e) // Cetak seluruh stack trace error
                                }
                            } // Akhir dari forEachIndexed

                            if (propsArray.isEmpty()) {
                                Log.d(TAG, "  Data potentially from HAL (array was empty)")
                            } else {
                                Log.d(TAG, "  Data potentially from mSensorPropsMap (array had ${propsArray.size} elements)")
                            }

                        } else { // Jika result bukan Array<*>
                            Log.w(TAG, "getSensorPropForInstance() returned non-array result type: ${result.javaClass.name}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked getSensorPropForInstance method (using findAndHookMethod)")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook getSensorPropForInstance: ${e.message}")
            XposedBridge.log(e) // Cetak seluruh stack trace error
        }
    }
}