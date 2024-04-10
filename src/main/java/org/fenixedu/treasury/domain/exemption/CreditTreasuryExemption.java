package org.fenixedu.treasury.domain.exemption;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.FenixFramework;

public class CreditTreasuryExemption extends CreditTreasuryExemption_Base {

    public CreditTreasuryExemption() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected CreditTreasuryExemption(CreditEntry creditEntry, TreasuryExemption treasuryExemption,
            BigDecimal creditedNetExemptedAmount) {
        this();

        setCreditEntry(creditEntry);
        setTreasuryExemption(treasuryExemption);
        setCreditedNetExemptedAmount(creditedNetExemptedAmount);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.domainRoot.required");
        }

        if (getCreditEntry() == null) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.creditEntry.required");
        }

        if (getTreasuryExemption() == null) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.treasuryExemption.required");
        }

        if (getTreasuryExemption().getCreditEntry() != null) {
            throw new TreasuryDomainException(
                    "error.CreditTreasuryExemption.treasuryExemption.from.closed.debitEntry.not.supported");
        }

        if (getCreditedNetExemptedAmount() == null) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.creditedNetExemptedAmount.required");
        }

        if (!TreasuryConstants.isPositive(getCreditedNetExemptedAmount()) || getCreditedNetExemptedAmount().scale() > 2) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.creditedNetExemptedAmount.invalid");
        }

        if (TreasuryConstants.isGreaterThan(getTreasuryExemption().getTotalCreditedNetExemptedAmount(),
                getTreasuryExemption().getNetExemptedAmount())) {
            throw new TreasuryDomainException(
                    "error.CreditTreasuryExemption.totalCreditedNetExemption.greater.than.treasuryExemption.netExemptedAmount");
        }

        // Ensure there is only one CreditTreasuryExemption per TreasuryExemption
        if (getCreditEntry().getCreditTreasuryExemptionsSet().stream()
                .anyMatch(cte -> cte != this && cte.getTreasuryExemption() == getTreasuryExemption())) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.duplicated");
        }

        if (getCreditEntry().getDebitEntry() != getTreasuryExemption().getDebitEntry()) {
            throw new TreasuryDomainException("error.CreditTreasuryExemption.debitEntry.mismatch");
        }
    }

    public void editCreditedNetExemptedAmount(BigDecimal newCreditedNetExemptedAmount) {
        if (getCreditEntry().isProcessedInClosedDebitNote()) {
            throw new RuntimeException("error.CreditTreasuryExemption.creditEntry.is.closed");
        }

        if (getCreditEntry().isAnnulled()) {
            throw new RuntimeException("error.CreditTreasuryExemption.creditEntry.is.annuled");
        }

        super.setCreditedNetExemptedAmount(newCreditedNetExemptedAmount);

        checkRules();
    }

    // Services

    public static CreditTreasuryExemption create(CreditEntry creditEntry, TreasuryExemption treasuryExemption,
            BigDecimal creditedNetExemptedAmount) {
        return new CreditTreasuryExemption(creditEntry, treasuryExemption, creditedNetExemptedAmount);
    }

    public static CreditTreasuryExemption createForImportation(CreditEntry creditEntry, TreasuryExemption treasuryExemption,
            BigDecimal creditedNetExemptedAmount) {
        CreditTreasuryExemption result = new CreditTreasuryExemption();

        result.setCreditEntry(creditEntry);
        result.setTreasuryExemption(treasuryExemption);
        result.setCreditedNetExemptedAmount(creditedNetExemptedAmount);

        return result;
    }

}