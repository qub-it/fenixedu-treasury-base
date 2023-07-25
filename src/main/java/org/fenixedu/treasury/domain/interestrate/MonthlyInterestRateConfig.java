package org.fenixedu.treasury.domain.interestrate;

import java.math.BigDecimal;

public interface MonthlyInterestRateConfig {

    BigDecimal getInterestsPercentage(); 
    boolean isPostponePaymentLimitDateToFirstWorkDate();
    boolean isApplyPenaltyInFirstWorkday();
    int getMaximumMonthsToApplyInterests();
    
}
