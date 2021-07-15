package org.fenixedu.treasury.dto.PaymentPlans;

import java.util.List;

import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;

public enum AddictionsCalculeTypeEnum {
    BEFORE_DEBIT_ENTRY, BY_INSTALLMENT_ENTRY_AMOUNT, AFTER_DEBIT_ENTRY;

    public static List<AddictionsCalculeTypeEnum> getAddictionsCalculeTypesForInterest() {
        return List.of(BY_INSTALLMENT_ENTRY_AMOUNT, AFTER_DEBIT_ENTRY);
    }

    public static List<AddictionsCalculeTypeEnum> getAddictionsCalculeTypesForPenaltyTax() {
        return List.of(BEFORE_DEBIT_ENTRY, AFTER_DEBIT_ENTRY);
    }

    public String getName() {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(TreasuryConstants.BUNDLE,
                "label.AddictionsCalculeTypeEnum." + name());
    }

    public boolean isBeforeDebitEntry() {
        return this.equals(BEFORE_DEBIT_ENTRY);
    }

    public boolean isByInstallmentEntryAmount() {
        return this.equals(BY_INSTALLMENT_ENTRY_AMOUNT);
    }

    public boolean isAfterDebitEntry() {
        return this.equals(AFTER_DEBIT_ENTRY);
    }
}
