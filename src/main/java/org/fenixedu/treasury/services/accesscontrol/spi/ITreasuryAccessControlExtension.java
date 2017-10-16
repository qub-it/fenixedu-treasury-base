package org.fenixedu.treasury.services.accesscontrol.spi;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;

public interface ITreasuryAccessControlExtension {

    public boolean isFrontOfficeMember(final String username);

    public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution);
    
    public boolean isBackOfficeMember(final String username);

    public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution);

    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity);
    
    public boolean isManager(final String username);

    public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution);

    public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution);

}
