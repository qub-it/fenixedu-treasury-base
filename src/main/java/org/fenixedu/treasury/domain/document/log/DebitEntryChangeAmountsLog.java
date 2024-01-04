package org.fenixedu.treasury.domain.document.log;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class DebitEntryChangeAmountsLog extends DebitEntryChangeAmountsLog_Base {

    public DebitEntryChangeAmountsLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());

        setChangeDate(new DateTime());
        setResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    public DebitEntryChangeAmountsLog(String reason) {
        this();

        setReason(reason);
    }

    public static DebitEntryChangeAmountsLog log(DebitEntry debitEntry, String changeContext, String reason) {
        if (StringUtils.isEmpty(changeContext)) {
            throw new IllegalArgumentException("error.DebitEntryChangeAmountsLog.changeContext.required");
        }

        DebitEntryChangeAmountsLog log = new DebitEntryChangeAmountsLog(reason);

        log.setDebitEntry(debitEntry);
        log.setDebitEntryCode(debitEntry.getCode());
        log.setChangeContext(changeContext);
        log.setOldUnitAmount(debitEntry.getAmount());
        log.setOldQuantity(debitEntry.getQuantity());
        log.setOldVatRate(debitEntry.getVatRate());
        log.setOldNetAmount(debitEntry.getNetAmount());
        log.setOldExemptedAmount(debitEntry.getNetExemptedAmount());
        log.setOldVatAmount(debitEntry.getVatAmount());
        log.setOldAmountWithVat(debitEntry.getAmountWithVat());

        return log;
    }

}
