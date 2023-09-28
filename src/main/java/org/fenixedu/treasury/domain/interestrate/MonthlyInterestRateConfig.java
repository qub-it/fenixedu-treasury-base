package org.fenixedu.treasury.domain.interestrate;

import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

public interface MonthlyInterestRateConfig {

    BigDecimal getInterestsPercentage(); 
    boolean isPostponePaymentLimitDateToFirstWorkDate();
    boolean isApplyPenaltyInFirstWorkday();
    int getMaximumMonthsToApplyInterests();

    LocalDate getOverridenFirstDayToApplyInterests(int month);
    
    LocalizedString getInterestDebitEntryFormat();
}
