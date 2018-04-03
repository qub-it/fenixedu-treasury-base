package org.fenixedu.treasury.domain.accesscontrol;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.services.accesscontrol.spi.ITreasuryAccessControlExtension;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TreasuryAccessControl {

    private static TreasuryAccessControl _instance = null;

    private List<ITreasuryAccessControlExtension> extensions = Collections.synchronizedList(Lists.newArrayList());

    private TreasuryAccessControl() {
    }

    public boolean isFrontOfficeMember() {
    	final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
    	
        return isFrontOfficeMember(services.getLoggedUsername());
    }

    public boolean isBackOfficeMember() {
    	final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

    	return isBackOfficeMember(services.getLoggedUsername());
    }

    public boolean isFrontOfficeMember(FinantialInstitution finantialInstitution) {
    	final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

    	return isFrontOfficeMember(services.getLoggedUsername(), finantialInstitution);
    }

    public boolean isBackOfficeMember(FinantialInstitution finantialInstitution) {
    	final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

    	return isBackOfficeMember(services.getLoggedUsername(), finantialInstitution);
    }

    public boolean isManager() {
    	final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

    	return isManager(services.getLoggedUsername());
    }

    public boolean isFrontOfficeMember(final String username) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isFrontOfficeMember(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isFrontOfficeMember(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public <T> boolean isFrontOfficeMemberWithinContext(final String username, final T context) {
        for (ITreasuryAccessControlExtension<T> iTreasuryAccessControlExtension : extensions) {
            if(!iTreasuryAccessControlExtension.isContextObjectApplied(context)) {
                continue;
            }
            
            if (iTreasuryAccessControlExtension.isFrontOfficeMemberWithinContext(username, context)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isBackOfficeMember(final String username) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isBackOfficeMember(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isBackOfficeMember(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isBackOfficeMember(username, finantialEntity)) {
                return true;
            }
        }

        return false;
    }
    
    public <T> boolean isBackOfficeMemberWithinContext(final String username, final T context) {
        for (ITreasuryAccessControlExtension<T> iTreasuryAccessControlExtension : extensions) {
            if(!iTreasuryAccessControlExtension.isContextObjectApplied(context)) {
                continue;
            }
            
            if (iTreasuryAccessControlExtension.isBackOfficeMemberWithinContext(username, context)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isManager(final String username) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isManager(username)) {
                return true;
            }
        }

        return false;
    }

    public void registerExtension(final ITreasuryAccessControlExtension extension) {
        extensions.add(extension);
    }

    public void unregisterExtension(final ITreasuryAccessControlExtension extension) {
        extensions.add(extension);
    }

    public synchronized static TreasuryAccessControl getInstance() {
        if (_instance == null) {
            _instance = new TreasuryAccessControl();
        }

        return _instance;
    }

    public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isAllowToModifyInvoices(username, finantialInstitution) == false) {
                return false;
            }
        }

        return true;
    }

    public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension iTreasuryAccessControlExtension : extensions) {
            if (iTreasuryAccessControlExtension.isAllowToModifySettlements(username, finantialInstitution) == false) {
                return false;
            }
        }

        return true;
    }

}
