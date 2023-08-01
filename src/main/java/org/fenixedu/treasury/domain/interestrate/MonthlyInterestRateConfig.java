package org.fenixedu.treasury.domain.interestrate;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

public interface MonthlyInterestRateConfig {

    BigDecimal getInterestsPercentage(); 
    boolean isPostponePaymentLimitDateToFirstWorkDate();
    boolean isApplyPenaltyInFirstWorkday();
    int getMaximumMonthsToApplyInterests();

    LocalDate getOverridenFirstDayToApplyInterests(int month);
}
