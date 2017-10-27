package org.fenixedu.treasury.domain;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

public class FiscalDataUpdateLog extends FiscalDataUpdateLog_Base {

    public FiscalDataUpdateLog() {
        super();
        setBennu(Bennu.getInstance());
    }

    private FiscalDataUpdateLog(final Customer customer, String oldFiscalCountry, String oldFiscalNumber,
            boolean changeFiscalNumberConfirmed, boolean withFinantialDocumentsIntegratedInERP,
            boolean customerInformationMaybeIntegratedWithSuccess,
            boolean customerWithFinantialDocumentsIntegratedInPreviousERP) {
        this();

        this.setCustomer(customer);
        this.setWhenUpdated(new DateTime());
        this.setResponsibleUsername(Authenticate.getUser().getUsername());
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
        
        if(getBennu() == null) {
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

        FiscalDataUpdateLog log = new FiscalDataUpdateLog();

        return log;
    }

}
