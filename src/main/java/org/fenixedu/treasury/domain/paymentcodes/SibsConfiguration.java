/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: xpto@qub-it.com
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fenixedu.treasury.domain.paymentcodes;

import java.util.Collection;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class SibsConfiguration extends SibsConfiguration_Base {

    protected SibsConfiguration() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected void init(final FinantialInstitution finantialInstitution, final String entityReferenceCode,
            final String sourceInstitutionId, final String destinationInstitutionId) {
        setFinantialInstitution(finantialInstitution);
        setEntityReferenceCode(entityReferenceCode);
        setSourceInstitutionId(sourceInstitutionId);
        setDestinationInstitutionId(destinationInstitutionId);
        checkRules();
    }

    private void checkRules() {
        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.SibsConfiguration.finantialInstitution.required");
        }
    }

    @Atomic
    public void edit(final String entityReferenceCode, final String sourceInstitutionId, final String destinationInstitutionId,
            final String sibsPaymentsBrokerUrl, final String sibsPaymentsBrokerSharedKey) {
        setEntityReferenceCode(entityReferenceCode);
        setSourceInstitutionId(sourceInstitutionId);
        setDestinationInstitutionId(destinationInstitutionId);
        setSibsPaymentsBrokerUrl(sibsPaymentsBrokerUrl);
        setSibsPaymentsBrokerSharedKey(sibsPaymentsBrokerSharedKey);
        
        checkRules();
    }
    
    public boolean isPaymentsBrokerActive() {
        return !Strings.isNullOrEmpty(getSibsPaymentsBrokerUrl()) && !Strings.isNullOrEmpty(getSibsPaymentsBrokerSharedKey());
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {

        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        setDomainRoot(null);
        setFinantialInstitution(null);
        deleteDomainObject();
    }

    @Atomic
    public static SibsConfiguration create(final FinantialInstitution finantialInstitution, final String entityReferenceCode,
            final String sourceInstitutionId, final String destinationInstitutionId) {
        SibsConfiguration sibsConfiguration = new SibsConfiguration();
        sibsConfiguration.init(finantialInstitution, entityReferenceCode, sourceInstitutionId, destinationInstitutionId);
        return sibsConfiguration;
    }

    public boolean isValid() {
        return !Strings.isNullOrEmpty(this.getDestinationInstitutionId())
                && !Strings.isNullOrEmpty(this.getSourceInstitutionId()) && !Strings.isNullOrEmpty(this.getEntityReferenceCode());
    }
}
