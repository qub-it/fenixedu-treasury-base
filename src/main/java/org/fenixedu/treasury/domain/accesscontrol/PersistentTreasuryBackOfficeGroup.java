package org.fenixedu.treasury.domain.accesscontrol;

import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.treasury.services.groups.TreasuryBackOfficeGroup;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PersistentTreasuryBackOfficeGroup extends PersistentTreasuryBackOfficeGroup_Base {
    
    public PersistentTreasuryBackOfficeGroup() {
        super();
        setDomainRootForPersistentTreasuryBackOfficeGroup(FenixFramework.getDomainRoot());
    }

    @Override
    public Group toGroup() {
        return TreasuryBackOfficeGroup.get();
    }

    @Atomic
    public static PersistentGroup getInstance() {
        if(FenixFramework.getDomainRoot().getPersistentTreasuryBackOfficeGroup() == null) {
            new PersistentTreasuryBackOfficeGroup();
        }
        
        return FenixFramework.getDomainRoot().getPersistentTreasuryBackOfficeGroup();
    }
}
