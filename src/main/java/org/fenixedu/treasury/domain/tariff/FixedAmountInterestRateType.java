package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.Map.Entry;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class FixedAmountInterestRateType extends FixedAmountInterestRateType_Base {

    public FixedAmountInterestRateType() {
        super();
    }

    public FixedAmountInterestRateType(LocalizedString description) {
        this();

        super.init(description);
    }

    protected void checkRules() {
        super.checkRules();
    }

    @Override
    public InterestRateBean calculateInterests(DebitEntry debitEntry, LocalDate paymentDate, boolean withAllInterestValues) {
        return calculateForFixedAmount(debitEntry, withAllInterestValues);
    }

    @Override
    public InterestRateBean calculateAllInterestsByLockingAtDate(DebitEntry debitEntry, LocalDate lockDate) {
        return calculateForFixedAmount(debitEntry, true);
    }

    private InterestRateBean calculateForFixedAmount(DebitEntry debitEntry, boolean withAllInterestValues) {
        InterestRate interestRate = debitEntry.getInterestRate();

        final InterestRateBean result = new InterestRateBean(interestRate.getInterestType());
        BigDecimal totalInterestAmount = Currency.getValueWithScale(interestRate.getInterestFixedAmount());

        if (!withAllInterestValues) {
            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntriesMap(debitEntry).entrySet()) {
                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());
                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
            }
        }

        if (TreasuryConstants.isNegative(totalInterestAmount)) {
            totalInterestAmount = BigDecimal.ZERO;
        }

        result.setInterestAmount(Currency.getValueWithScale(totalInterestAmount));
        return result;
    }

    public static FixedAmountInterestRateType create(LocalizedString description) {
        return new FixedAmountInterestRateType(description);
    }
}
