package org.fenixedu.treasury.services.accesscontrol.spi;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;

import com.google.common.reflect.TypeToken;

public interface ITreasuryAccessControlExtension<T> {

    
    
    default public boolean isFrontOfficeMember(final String username) {
        return false;
    }

    default public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        return false;
    }
    
    default public boolean isFrontOfficeMemberWithinContext(final String username, final T context) {
        return false;
    }

    default public boolean isBackOfficeMember(final String username) {
        return false;
    }

    default public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        return false;
    }

    default public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        return false;
    }
    
    default public boolean isBackOfficeMemberWithinContext(final String username, final T context) {
        return false;
    }

    default public boolean isManager(final String username) {
        return false;
    }
    
    default public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        return false;
    }

    default public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        return false;
    }
    
    default public boolean isContextObjectApplied(final Object context) {
        final TypeToken<T> typeToken = new TypeToken<T>(getClass()){};
        return context.getClass().isAssignableFrom(typeToken.getRawType());
    }
}
