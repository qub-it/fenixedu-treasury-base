package org.fenixedu.treasury.domain.accesscontrol;

import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.treasury.services.groups.TreasuryManagersGroup;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PersistentTreasuryManagersGroup extends PersistentTreasuryManagersGroup_Base {
    
    public PersistentTreasuryManagersGroup() {
        super();
        
        setDomainRootForPersistentTreasuryManagersGroup(FenixFramework.getDomainRoot());
    }

    @Override
    public Group toGroup() {
        return TreasuryManagersGroup.get();
    }

    @Atomic
    public static PersistentGroup getInstance() {
        if(FenixFramework.getDomainRoot().getPersistentTreasuryManagersGroup() == null) {
            new PersistentTreasuryManagersGroup();
        }
        
        return FenixFramework.getDomainRoot().getPersistentTreasuryManagersGroup();
    }
}
