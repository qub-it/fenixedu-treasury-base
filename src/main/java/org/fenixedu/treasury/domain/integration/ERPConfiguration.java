/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.domain.integration;

import java.util.Collection;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ERPConfiguration extends ERPConfiguration_Base {

    protected ERPConfiguration() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(Series paymentsIntegrationSeries, FinantialInstitution finantialInstitution, String code,
            String externalURL, String username, String password, String implementationClassName,
            Long maxSizeBytesToExportOnline) {
        setActive(false);
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
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.ERPConfiguration.domainRoot.required");
        }

        if (Boolean.TRUE.equals(getActive()) && getPaymentsIntegrationSeries() == null) {
            throw new TreasuryDomainException("error.ERPConfiguration.paymentsIntegrationSeries.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ERPConfiguration.finantialInstitution.required");
        }
    }

    @Atomic
    public void edit(boolean active, Series paymentsIntegrationSeries, String externalURL, String username, String password,
            boolean exportAnnulledRelatedDocuments, boolean exportOnlyRelatedDocumentsPerExport, String implementationClassName,
            Long maxSizeBytesToExportOnline, String erpIdProcess, boolean closeCreditNoteWhenCreated,
            boolean partialReimbursementSupported) {
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
        setCloseCreditNoteWhenCreated(closeCreditNoteWhenCreated);
        setPartialReimbursementSupported(partialReimbursementSupported);

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

    public boolean isAllowFiscalFixWithLegacyDocsExportedLegacyERP() {
        return getAllowFiscalFixWithLegacyDocsExportedLegacyERP();
    }

    public boolean isToCloseCreditNoteWhenCreated() {
        return Boolean.TRUE.equals(getCloseCreditNoteWhenCreated());
    }

    public boolean isPartialReimbursementSupported() {
        return Boolean.TRUE.equals(getPartialReimbursementSupported());
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

    public static ERPConfiguration createEmpty(FinantialInstitution finantialInstitution) {
        ERPConfiguration eRPConfiguration = new ERPConfiguration();
        eRPConfiguration.init(null, finantialInstitution, null, null, null, null, null, 1024l);
        return eRPConfiguration;
    }

    @Atomic
    public static ERPConfiguration create(Series paymentsIntegrationSeries, FinantialInstitution finantialInstitution,
            String code, String externalURL, String username, String password, String implementationClassName,
            Long maxSizeBytesToExportOnline) {
        ERPConfiguration eRPConfiguration = new ERPConfiguration();
        eRPConfiguration.init(paymentsIntegrationSeries, finantialInstitution, code, externalURL, username, password,
                implementationClassName, maxSizeBytesToExportOnline);
        return eRPConfiguration;
    }

    public IERPExternalService getERPExternalServiceImplementation() {
        //#TODO REVIEW this method
        try {
            return (IERPExternalService) Class.forName(getImplementationClassName()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
//                return TreasuryPlataformDependentServicesFactory.implementation().getERPExternalServiceImplementation(this);
    }

}
