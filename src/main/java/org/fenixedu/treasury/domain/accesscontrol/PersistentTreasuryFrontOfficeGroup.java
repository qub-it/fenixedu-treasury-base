package org.fenixedu.treasury.domain.accesscontrol;

import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.treasury.services.groups.TreasuryFrontOfficeGroup;

import pt.ist.fenixframework.FenixFramework;

public class PersistentTreasuryFrontOfficeGroup extends PersistentTreasuryFrontOfficeGroup_Base {
    
    public PersistentTreasuryFrontOfficeGroup() {
        super();
        
        setDomainRootForPersistentTreasuryFrontOfficeGroup(FenixFramework.getDomainRoot());
    }

    @Override
    public Group toGroup() {
        return TreasuryFrontOfficeGroup.get();
    }

    public static PersistentGroup getInstance() {
        if(FenixFramework.getDomainRoot().getPersistentTreasuryFrontOfficeGroup() == null) {
            new PersistentTreasuryFrontOfficeGroup();
        }
        
        return FenixFramework.getDomainRoot().getPersistentTreasuryFrontOfficeGroup();
    }
    
}
