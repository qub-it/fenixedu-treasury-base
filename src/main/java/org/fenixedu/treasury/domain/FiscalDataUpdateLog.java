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
package org.fenixedu.treasury.domain;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

public class FiscalDataUpdateLog extends FiscalDataUpdateLog_Base {

    public FiscalDataUpdateLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private FiscalDataUpdateLog(final Customer customer, String oldFiscalCountry, String oldFiscalNumber,
            boolean changeFiscalNumberConfirmed, boolean withFinantialDocumentsIntegratedInERP,
            boolean customerInformationMaybeIntegratedWithSuccess,
            boolean customerWithFinantialDocumentsIntegratedInPreviousERP) {
        this();

        this.setCustomer(customer);
        this.setWhenUpdated(new DateTime());
        this.setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
        this.setOldFiscalCountry(oldFiscalCountry);
        this.setOldFiscalNumber(oldFiscalNumber);

        this.setUpdatedFiscalCountry(customer.getFiscalCountry());
        this.setUpdatedFiscalNumber(customer.getFiscalNumber());

        this.setChangeFiscalNumberConfirmed(changeFiscalNumberConfirmed);
        this.setWithFinantialDocumentsIntegratedInERP(withFinantialDocumentsIntegratedInERP);
        this.setCustomerInformationMaybeIntegratedWithSuccess(customerInformationMaybeIntegratedWithSuccess);
        this.setCustomerWithFinantialDocumentsIntegratedInPreviousERP(customerWithFinantialDocumentsIntegratedInPreviousERP);

        checkRules();
    }

    private void checkRules() {
        
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.bennu.required");
        }
        
        if(getCustomer() == null) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.customer.required");
        }

        if(getWhenUpdated() == null) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.whenUpdated.required");
        }

        if(Strings.isNullOrEmpty(getResponsibleUsername())) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.responsibleUsername.required");
        }

        if(Strings.isNullOrEmpty(getUpdatedFiscalCountry())) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.updatedFiscalCountry.required");
        }

        if(Strings.isNullOrEmpty(getUpdatedFiscalNumber())) {
            throw new TreasuryDomainException("error.FiscalDataUpdateLog.updatedFiscalNumber.required");
        }
        
    }
    
    public static FiscalDataUpdateLog create(final Customer customer, String oldFiscalCountry, String oldFiscalNumber,
            boolean changeFiscalNumberConfirmed, boolean withFinantialDocumentsIntegratedInERP,
            boolean customerInformationMaybeIntegratedWithSuccess,
            boolean customerWithFinantialDocumentsIntegratedInPreviousERP) {

        final FiscalDataUpdateLog log = new FiscalDataUpdateLog(customer, oldFiscalCountry, oldFiscalNumber,
                changeFiscalNumberConfirmed, withFinantialDocumentsIntegratedInERP,
                customerInformationMaybeIntegratedWithSuccess,
                customerWithFinantialDocumentsIntegratedInPreviousERP); 

        return log;
    }

}
