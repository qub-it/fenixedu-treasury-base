package org.fenixedu.treasury.services.accesscontrol;

import java.util.Set;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.accesscontrol.TreasuryAccessControl;
import org.fenixedu.treasury.services.accesscontrol.spi.ITreasuryAccessControlExtension;

public class TreasuryAccessControlAPI {

    public static void registerExtension(final ITreasuryAccessControlExtension extension) {
        TreasuryAccessControl.getInstance().registerExtension(extension);
    }

    public static void unregisterExtension(final ITreasuryAccessControlExtension extension) {
        TreasuryAccessControl.getInstance().unregisterExtension(extension);
    }

    public static boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        return TreasuryAccessControl.getInstance().isAllowToModifyInvoices(username, finantialInstitution);
    }

    public static boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        return TreasuryAccessControl.getInstance().isAllowToModifySettlements(username, finantialInstitution);
    }

    public static boolean isFrontOfficeMember(final String username) {
        return TreasuryAccessControl.getInstance().isFrontOfficeMember(username);
    }

    public static boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        return TreasuryAccessControl.getInstance().isFrontOfficeMember(username, finantialInstitution);
    }
    
    public static <T> boolean isFrontOfficeMemberWithinContext(final String username, final T context) {
        return TreasuryAccessControl.getInstance().isFrontOfficeMemberWithinContext(username, context);
    }

    public static boolean isBackOfficeMember(final String username) {
        return TreasuryAccessControl.getInstance().isBackOfficeMember(username);
    }

    public static boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        return TreasuryAccessControl.getInstance().isBackOfficeMember(username, finantialInstitution);
    }

    public static boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        return TreasuryAccessControl.getInstance().isBackOfficeMember(username, finantialEntity);
    }
    
    public static <T> boolean isBackOfficeMemberWithinContext(final String username, final T context) {
        return TreasuryAccessControl.getInstance().isBackOfficeMemberWithinContext(username, context);
    }

    public static boolean isManager(final String username) {
        return TreasuryAccessControl.getInstance().isManager(username);
    }

}
