package org.fenixedu.treasury.services.integration;

public class TreasuryPlataformDependentServicesFactory {

    private static ITreasuryPlatformDependentServices _impl;
    
    public static ITreasuryPlatformDependentServices implementation() {
        return _impl;
    }
    
    public static synchronized void registerImplementation(ITreasuryPlatformDependentServices impl) {
        _impl = impl;
    }
	
}
