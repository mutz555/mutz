package com.mutz.fingerprintbypass

import android.content.Context
import android.os.IBinder
import android.util.Log // android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Field // Untuk mengakses field secara dinamis

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FingerprintBypass" // TAG untuk logging
        // Nilai yang mungkin untuk persist.vendor.sys.fp.vendor (sesuaikan jika perlu)
        // const val FP_VENDOR_GOODIX = "goodix"
        // const val FP_VENDOR_FPC = "fpc"
    }

    // Jika Anda ingin mencoba spoofing properti nanti
    // private val propsToSpoof = mapOf(
    //    "persist.vendor.sys.fp.vendor" to FP_VENDOR_GOODIX
    // )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: handleLoadPackage for package: ${lpparam.packageName}")

        // Hook umumnya ditargetkan ke proses sistem utama (android) atau aplikasi spesifik
        if (lpparam.packageName == "android") { // system_server
            XposedBridge.log("$TAG: Processing hooks for 'android' package (system_server)")

            // 1. Hook isFpHardwareDetected (untuk memunculkan UI)
            hookIsFpHardwareDetected(lpparam)

            // 2. Hook getSensorPropForInstance (untuk melihat detail SensorProps/HidlFingerprintSensorConfig)
            hookGetSensorPropForInstance(lpparam)

            // 3. Hook MiFxTunnelAidl.getHalData (jika kelas ini ada dan relevan)
            // Ini mungkin spesifik Xiaomi/HyperOS
            hookMiFxTunnelAidlGetHalData(lpparam)

            // 4. Hook ServiceManager.getDeclaredInstances (untuk melihat HAL yang terdaftar)
            hookServiceManagerMethods(lpparam)

            // 5. Hook FingerprintService.filterAvailableHalInstances (untuk melihat HAL yang dipilih)
            hookFingerprintServiceMethods(lpparam)

            // 6. (Opsional) Hook SystemProperties.get untuk spoofing
            // hookSystemPropertiesGet(lpparam)

        } else if (lpparam.packageName == "com.android.settings") {
            // Mungkin ada hook spesifik untuk aplikasi Settings jika diperlukan, tapi biasanya inti ada di 'android'
            XposedBridge.log("$TAG: Processing hooks for 'com.android.settings' package")
            // Contoh: hookIsFpHardwareDetected juga bisa diterapkan di sini jika Settings punya pengecekan sendiri
            // hookIsFpHardwareDetectedForSettings(lpparam) // Anda perlu mendefinisikan fungsi ini
        }
    }

    // 1. Hook isFpHardwareDetected
    private fun hookIsFpHardwareDetected(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook isFpHardwareDetected in FingerprintServiceStubImpl")
            // Nama kelas dari referensi Anda
            val clazz = XposedHelpers.findClassIfExists(
                "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl",
                lpparam.classLoader
            )

            if (clazz == null) {
                XposedBridge.log("$TAG: Class com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl not found.")
                // Coba alternatif jika nama kelas berbeda di ROM Anda
                // val alternativeClazz = XposedHelpers.findClassIfExists("nama.kelas.alternatif.FingerprintManager", lpparam.classLoader)
                // if (alternativeClazz != null) { ... }
                return
            }
            XposedBridge.log("$TAG: Found class: ${clazz.name} for isFpHardwareDetected hook")

            XposedHelpers.findAndHookMethod(
                clazz,
                "isFpHardwareDetected",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        XposedBridge.log("$TAG: isFpHardwareDetected() REPLACED, returning true.")
                        // Log.i(TAG, "isFpHardwareDetected() bypassed!") // Anda bisa menggunakan android.util.Log juga
                        return true
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked isFpHardwareDetected.")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook isFpHardwareDetected: ${e.message}")
            XposedBridge.log(e)
        }
    }

    // 2. Hook getSensorPropForInstance
    private fun hookGetSensorPropForInstance(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.log("$TAG: Trying to hook FingerprintSensorConfigurations.getSensorPropForInstance")
            val className = "android.hardware.fingerprint.FingerprintSensorConfigurations"
            val methodName = "getSensorPropForInstance"

            XposedHelpers.findAndHookMethod(
                className,
                lpparam.classLoader,
                methodName,
                String::class.java, // instance (String)
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        XposedBridge.log("$TAG: Before $methodName for instance: $instance")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.args[0] as? String
                        val result = param.result
                        XposedBridge.log("$TAG: After $methodName for instance: $instance, result type: ${result?.javaClass?.name}")

                        if (result == null) {
                            XposedBridge.log("$TAG: $methodName returned null for instance: $instance")
                            return
                        }

                        if (result is Array<*>) {
                            val propsArray = result
                            XposedBridge.log("$TAG: $methodName result is an Array. Size: ${propsArray.size}")

                            if (propsArray.isEmpty()) {
                                XposedBridge.log("$TAG: $methodName result array is empty.")
                            }

                            propsArray.forEachIndexed { index, propObject: Any? ->
                                if (propObject == null) {
                                    XposedBridge.log("$TAG: Prop[$index] from $methodName is null")
                                    return@forEachIndexed
                                }
                                XposedBridge.log("$TAG:   Processing Prop[$index] from $methodName - Object Type: ${propObject.javaClass.name}")

                                try {
                                    // Mencoba mencatat semua declared fields dari propObject dan superclass-nya
                                    var currentClass: Class<*>? = propObject.javaClass
                                    XposedBridge.log("$TAG:     --- Fields for ${currentClass?.name} (and superclasses) ---")
                                    val loggedFields = mutableSetOf<String>()
                                    while (currentClass != null && currentClass != Object::class.java) {
                                        XposedBridge.log("$TAG:       Fields from class: ${currentClass.name}")
                                        currentClass.declaredFields.forEach { field ->
                                            if (!loggedFields.contains(field.name)) {
                                                loggedFields.add(field.name)
                                                try {
                                                    field.isAccessible = true
                                                    val value = field.get(propObject)
                                                    XposedBridge.log("$TAG:         ${field.name} (${field.type.simpleName}): $value")
                                                } catch (accessEx: IllegalAccessException) {
                                                    XposedBridge.log("$TAG:         ${field.name} (${field.type.simpleName}): <Cannot access: ${accessEx.message}>")
                                                } catch (getEx: Exception) {
                                                    XposedBridge.log("$TAG:         ${field.name} (${field.type.simpleName}): <Error getting value: ${getEx.message}>")
                                                }
                                            }
                                        }
                                        currentClass = currentClass.superclass
                                    }
                                    XposedBridge.log("$TAG:     --- End of Fields for Prop[$index] ---")

                                } catch (e: Throwable) {
                                    XposedBridge.log("$TAG: Error reflecting fields for Prop[$index] (${propObject.javaClass.name}): ${e.message}")
                                    XposedBridge.log(e)
                                }
                            }
                        } else {
                            XposedBridge.log("$TAG: $methodName returned non-array result type: ${result.javaClass.name}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked $className.$methodName")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook FingerprintSensorConfigurations.getSensorPropForInstance: ${e.message}")
            XposedBridge.log(e)
        }
    }

    // 3. Hook MiFxTunnelAidl.getHalData
    private fun hookMiFxTunnelAidlGetHalData(lpparam: XC_LoadPackage.LoadPackageParam) {
        val className = "android.hardware.fingerprint.MiFxTunnelAidl"
        val methodName = "getHalData"
        try {
            val miFxTunnelAidlClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader)
            if (miFxTunnelAidlClass == null) {
                XposedBridge.log("$TAG: $className class not found. Skipping this hook.")
                return
            }
            XposedBridge.log("$TAG: Found $className class. Trying to hook $methodName.")

            XposedHelpers.findAndHookMethod(
                miFxTunnelAidlClass,
                methodName,
                Int::class.javaPrimitiveType, // cmdId (int)
                ByteArray::class.java,       // params (byte[])
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val cmdId = param.args[0] as Int
                        val params = param.args[1] as? ByteArray
                        XposedBridge.log("$TAG: Before $className.$methodName - cmdId: $cmdId, params: ${params?.joinToString { "%02x".format(it) }}")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val cmdId = param.args[0] as Int
                        val resultObject = param.result // Ini adalah HalDataCmdResult
                        if (resultObject != null) {
                            try {
                                val resultCode = XposedHelpers.getIntField(resultObject, "mResultCode")
                                val resultData = XposedHelpers.getObjectField(resultObject, "mResultData") as? ByteArray
                                XposedBridge.log("$TAG: After $className.$methodName for cmdId: $cmdId - ResultCode: $resultCode, ResultData (hex): ${resultData?.joinToString("") { "%02x".format(it) }}")
                                if (resultData != null) {
                                    try {
                                        XposedBridge.log("$TAG:   ResultData as String (UTF-8): ${String(resultData, Charsets.UTF_8)}")
                                    } catch (e: Exception) {
                                        XposedBridge.log("$TAG:   ResultData could not be fully decoded as UTF-8 String.")
                                    }
                                }
                            } catch (fieldEx: Exception) {
                                XposedBridge.log("$TAG: Error accessing fields from HalDataCmdResult: ${fieldEx.message}")
                            }
                        } else {
                            XposedBridge.log("$TAG: After $className.$methodName for cmdId: $cmdId - Result is null")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked $className.$methodName.")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $className.$methodName: ${e.message}")
            XposedBridge.log(e)
        }
    }

    // 4. Hook ServiceManager methods
    private fun hookServiceManagerMethods(lpparam: XC_LoadPackage.LoadPackageParam) {
        val serviceManagerClassName = "android.os.ServiceManager"
        try {
            XposedBridge.log("$TAG: Trying to hook $serviceManagerClassName methods")

            // Hook getDeclaredInstances(String)
            XposedHelpers.findAndHookMethod(
                serviceManagerClassName,
                lpparam.classLoader, // Classloader untuk kelas framework
                "getDeclaredInstances",
                String::class.java, // interfaceDescriptor
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val interfaceDescriptor = param.args[0] as String
                        // Dari FingerprintService.smali, DESCRIPTOR adalah android.hardware.biometrics.fingerprint.IFingerprint
                        if (interfaceDescriptor == "android.hardware.biometrics.fingerprint.IFingerprint") {
                            val instances = param.result as? Array<String>
                            XposedBridge.log("$TAG: ServiceManager.getDeclaredInstances for IFingerprint: ${instances?.contentToString()}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked $serviceManagerClassName.getDeclaredInstances")

            // Hook waitForDeclaredService(String) atau getService(String)
            val serviceGetterMethodName = "waitForDeclaredService" // atau "getService"
            XposedHelpers.findAndHookMethod(
                serviceManagerClassName,
                lpparam.classLoader,
                serviceGetterMethodName,
                String::class.java, // name
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val serviceName = param.args[0] as String
                        if (serviceName.contains("fingerprint", ignoreCase = true) || serviceName.contains("IFingerprint", ignoreCase = true)) {
                            XposedBridge.log("$TAG: ServiceManager.$serviceGetterMethodName called for: $serviceName")
                        }
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val serviceName = param.args[0] as String
                        val binder = param.result as? IBinder
                         if (serviceName.contains("fingerprint", ignoreCase = true) || serviceName.contains("IFingerprint", ignoreCase = true)) {
                            XposedBridge.log("$TAG: ServiceManager.$serviceGetterMethodName for $serviceName returned: $binder")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked $serviceManagerClassName.$serviceGetterMethodName")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $serviceManagerClassName methods: ${e.message}")
            XposedBridge.log(e)
        }
    }

    // 5. Hook FingerprintService.filterAvailableHalInstances
    private fun hookFingerprintServiceMethods(lpparam: XC_LoadPackage.LoadPackageParam) {
        val fingerprintServiceClassName = "com.android.server.biometrics.sensors.fingerprint.FingerprintService"
        val filterMethodName = "filterAvailableHalInstances"
        try {
            val fpServiceClass = XposedHelpers.findClassIfExists(fingerprintServiceClassName, lpparam.classLoader)
            if (fpServiceClass == null) {
                XposedBridge.log("$TAG: $fingerprintServiceClassName class not found. Skipping this hook.")
                return
            }
            XposedBridge.log("$TAG: Found $fingerprintServiceClassName class. Trying to hook $filterMethodName.")

            // Argumen untuk filterAvailableHalInstances adalah FingerprintSensorConfigurations
            val fpSensorConfigsClassName = "android.hardware.fingerprint.FingerprintSensorConfigurations"
            val fpSensorConfigsClass = XposedHelpers.findClassIfExists(fpSensorConfigsClassName, lpparam.classLoader)

            if (fpSensorConfigsClass == null) {
                XposedBridge.log("$TAG: $fpSensorConfigsClassName class not found. Cannot hook $filterMethodName.")
                return
            }

            XposedHelpers.findAndHookMethod(
                fpServiceClass,
                filterMethodName,
                fpSensorConfigsClass, // arg0: FingerprintSensorConfigurations
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val configurations = param.args[0]
                        XposedBridge.log("$TAG: Before $fingerprintServiceClassName.$filterMethodName called with configurations: $configurations")
                        // Anda bisa mencoba mencatat isi mSensorPropsMap dari 'configurations' di sini jika perlu
                        // try {
                        //     val sensorPropsMap = XposedHelpers.getObjectField(configurations, "mSensorPropsMap")
                        //     XposedBridge.log("$TAG:   mSensorPropsMap: $sensorPropsMap")
                        // } catch (e: Exception) {
                        //     XposedBridge.log("$TAG:   Error accessing mSensorPropsMap: ${e.message}")
                        // }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val resultPair = param.result // Ini adalah Pair<String, SensorProps[]>
                        if (resultPair != null) {
                            val instanceName = XposedHelpers.getObjectField(resultPair, "first") as? String
                            val sensorPropsArray = XposedHelpers.getObjectField(resultPair, "second") // Ini Array
                            var propsDetails = "null"
                            if (sensorPropsArray is Array<*>) {
                                propsDetails = sensorPropsArray.joinToString { it?.javaClass?.simpleName ?: "null" }
                            }
                            XposedBridge.log("$TAG: After $fingerprintServiceClassName.$filterMethodName - Selected Instance: $instanceName, Props: $propsDetails")
                        } else {
                            XposedBridge.log("$TAG: After $fingerprintServiceClassName.$filterMethodName - Result is null")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: Successfully hooked $fingerprintServiceClassName.$filterMethodName")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $fingerprintServiceClassName methods: ${e.message}")
            XposedBridge.log(e)
        }
    }

    // 6. (Opsional) Hook SystemProperties.get
    // private fun hookSystemPropertiesGet(lpparam: XC_LoadPackage.LoadPackageParam) {
    //     val systemPropertiesClassName = "android.os.SystemProperties"
    //     try {
    //         XposedBridge.log("$TAG: Trying to hook $systemPropertiesClassName.get")
    //         XposedHelpers.findAndHookMethod(
    //             systemPropertiesClassName,
    //             lpparam.classLoader,
    //             "get",
    //             String::class.java, // key
    //             object : XC_MethodHook() {
    //                 override fun beforeHookedMethod(param: MethodHookParam) {
    //                     val key = param.args[0] as String
    //                     if (propsToSpoof.containsKey(key)) {
    //                         val originalCall = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
    //                         XposedBridge.log("$TAG: SystemProperties.get($key) original value: $originalCall")
    //                         param.result = propsToSpoof[key]
    //                         XposedBridge.log("$TAG:   Spoofed to -> ${propsToSpoof[key]}")
    //                     }
    //                 }
    //             }
    //         )
    //
    //         XposedHelpers.findAndHookMethod(
    //             systemPropertiesClassName,
    //             lpparam.classLoader,
    //             "get",
    //             String::class.java, // key
    //             String::class.java, // def
    //             object : XC_MethodHook() {
    //                 override fun beforeHookedMethod(param: MethodHookParam) {
    //                     val key = param.args[0] as String
    //                     if (propsToSpoof.containsKey(key)) {
    //                         val originalCall = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
    //                         XposedBridge.log("$TAG: SystemProperties.get($key, def) original value: $originalCall")
    //                         param.result = propsToSpoof[key]
    //                         XposedBridge.log("$TAG:   Spoofed to -> ${propsToSpoof[key]}")
    //                     }
    //                 }
    //             }
    //         )
    //         XposedBridge.log("$TAG: Successfully hooked $systemPropertiesClassName.get")
    //     } catch (e: Throwable) {
    //         XposedBridge.log("$TAG: Failed to hook $systemPropertiesClassName.get: ${e.message}")
    //         XposedBridge.log(e)
    //     }
    // }
}