package org.fenixedu.treasury.domain.accesscontrol;

import java.util.stream.Stream;

import pt.ist.fenixframework.FenixFramework;

public class TreasuryAccessControlConfiguration extends TreasuryAccessControlConfiguration_Base {
    
    public TreasuryAccessControlConfiguration() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
        
        if(findAll().count() > 1) {
            throw new RuntimeException("error.TreasuryAccessControlConfiguration.already.exists");
        }
        
        setAccessControlByAcademicAuthorizations(true);
        setAccessControlByBennuDynamicGroups(true);
        setAccessControlByStandardPermissions(true);
        setAccessControlByUnitBasedPermissions(true);
    }

    public void edit(boolean academicAuthorizations, boolean bennuDynamicGroups, boolean standardPermissions,
            boolean unitBasedPermissions) {
        
        setAccessControlByAcademicAuthorizations(academicAuthorizations);
        setAccessControlByBennuDynamicGroups(bennuDynamicGroups);
        setAccessControlByStandardPermissions(standardPermissions);
        setAccessControlByUnitBasedPermissions(unitBasedPermissions);
    }
    
    /* Services */
    
    public static Stream<TreasuryAccessControlConfiguration> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryAccessControlConfigurationSet().stream();
    }
    
    public static TreasuryAccessControlConfiguration getInstance() {
        return FenixFramework.getDomainRoot().getTreasuryAccessControlConfigurationSet().stream().findFirst().orElse(null);
    }
    
    public static TreasuryAccessControlConfiguration create() {
        return new TreasuryAccessControlConfiguration();
    }
    
    public static TreasuryAccessControlConfiguration create(boolean academicAuthorizations, boolean bennuDynamicGroups, boolean standardPermissions,
            boolean unitBasedPermissions) {
        TreasuryAccessControlConfiguration configuration = new TreasuryAccessControlConfiguration();
        
        configuration.setAccessControlByAcademicAuthorizations(academicAuthorizations);
        configuration.setAccessControlByBennuDynamicGroups(bennuDynamicGroups);
        configuration.setAccessControlByStandardPermissions(standardPermissions);
        configuration.setAccessControlByUnitBasedPermissions(unitBasedPermissions);
        
        return configuration;
    }
    
    public static boolean isAccessControlByAcademicAuthorizations() {
        if(getInstance() != null) {
            return Boolean.TRUE.equals(getInstance().getAccessControlByAcademicAuthorizations());
        }

        return true;
    }
    
    public static boolean isAccessControlByBennuDynamicGroups() {
        if(getInstance() != null) {
            return Boolean.TRUE.equals(getInstance().getAccessControlByBennuDynamicGroups());
        }

        return true;
    }
    
    public static boolean isAccessControlByStandardPermissions() {
        if(getInstance() != null) {
            return Boolean.TRUE.equals(getInstance().getAccessControlByStandardPermissions());
        }

        return true;
    }
    
    public static boolean isAccessControlByUnitBasedPermissions() {
        if(getInstance() != null) {
            return Boolean.TRUE.equals(getInstance().getAccessControlByUnitBasedPermissions());
        }

        return true;
    }
    
}
