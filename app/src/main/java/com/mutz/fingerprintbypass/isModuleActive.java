package com.mutz.fingerprintbypass;

/**
 * Class to check if the Xposed module is active
 * This class will be hooked by the module to return true
 */
public class isModuleActive {
    
    /**
     * Returns true if the module is active
     * 
     * @return true if module is active, false otherwise
     */
    public static boolean isModuleActive() {
        // This will return false by default
        // When the module is active, this method will be hooked to return true
        return false;
    }
}