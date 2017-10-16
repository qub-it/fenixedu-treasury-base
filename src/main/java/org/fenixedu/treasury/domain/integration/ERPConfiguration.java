/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
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

package org.fenixedu.treasury.domain.integration;

import java.util.Collection;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;

import pt.ist.fenixframework.Atomic;

public class ERPConfiguration extends ERPConfiguration_Base {

    protected ERPConfiguration() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected void init(final Series paymentsIntegrationSeries, final FinantialInstitution finantialInstitution,
            final String code, final String externalURL, final String username, final String password,
            final String implementationClassName, final Long maxSizeBytesToExportOnline) {
        setActive(true);
        setPaymentsIntegrationSeries(paymentsIntegrationSeries);
        setFinantialInstitution(finantialInstitution);
        setCode(code);
        setExternalURL(externalURL);
        setUsername(username);
        setPassword(password);
        setExportAnnulledRelatedDocuments(false);
        setExportOnlyRelatedDocumentsPerExport(false);
        setImplementationClassName(implementationClassName);
        setMaxSizeBytesToExportOnline(maxSizeBytesToExportOnline);
        setCreditsOfLegacyDebitWithoutLegacyInvoiceExportEnabled(false);
        checkRules();
    }

    private void checkRules() {
        if (getPaymentsIntegrationSeries() == null) {
            throw new TreasuryDomainException("error.ERPConfiguration.paymentsIntegrationSeries.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ERPConfiguration.finantialInstitution.required");
        }
    }

    @Atomic
    public void edit(final boolean active, final Series paymentsIntegrationSeries, final String externalURL,
            final String username, final String password, final boolean exportAnnulledRelatedDocuments,
            final boolean exportOnlyRelatedDocumentsPerExport, final String implementationClassName,
            Long maxSizeBytesToExportOnline, final String erpIdProcess) {
        setActive(active);
        setPaymentsIntegrationSeries(paymentsIntegrationSeries);
        setExternalURL(externalURL);
        setUsername(username);
        setPassword(password);
        setExportAnnulledRelatedDocuments(exportAnnulledRelatedDocuments);
        setExportOnlyRelatedDocumentsPerExport(exportOnlyRelatedDocumentsPerExport);
        setImplementationClassName(implementationClassName);
        setMaxSizeBytesToExportOnline(maxSizeBytesToExportOnline);
        setErpIdProcess(erpIdProcess);
        
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    public boolean isDeletable() {
        return true;
    }
    
    public boolean isIntegratedDocumentsExportationEnabled() {
        return getIntegratedDocumentsExportationEnabled();
    }
    
    public boolean isCreditsOfLegacyDebitWithoutLegacyInvoiceExportEnabled() {
        return getCreditsOfLegacyDebitWithoutLegacyInvoiceExportEnabled();
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.ERPConfiguration.cannot.delete");
        }
        setDomainRoot(null);
        setFinantialInstitution(null);
        setPaymentsIntegrationSeries(null);
        deleteDomainObject();
    }

    @Atomic
    public static ERPConfiguration create(final Series paymentsIntegrationSeries,
            final FinantialInstitution finantialInstitution, final String code, final String externalURL, final String username,
            final String password, final String implementationClassName, final Long maxSizeBytesToExportOnline) {
        ERPConfiguration eRPConfiguration = new ERPConfiguration();
        eRPConfiguration.init(paymentsIntegrationSeries, finantialInstitution, code, externalURL, username, password,
                implementationClassName, maxSizeBytesToExportOnline);
        return eRPConfiguration;
    }

    public IERPExternalService getERPExternalServiceImplementation() {
    	return TreasuryPlataformDependentServicesFactory.implementation().getERPExternalServiceImplementation(this);
    }

}
