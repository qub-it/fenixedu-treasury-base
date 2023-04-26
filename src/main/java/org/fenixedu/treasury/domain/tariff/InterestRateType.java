package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public abstract class InterestRateType extends InterestRateType_Base {

    public static Comparator<InterestRateType> COMPARE_BY_NAME = 
            (o1, o2) -> o1.getDescription().getContent().compareTo(o2.getDescription().getContent());
    
    public InterestRateType() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    public void init(LocalizedString description) {
        setDescription(description);
    }
    
    protected void checkRules() {
        if(getDomainRoot() == null) {
            throw new RuntimeException("error.InterestRateType.domainRoot.required");
        }
        
        if(getDescription() == null) {
            throw new RuntimeException("error.InterestRateType.description.required");
        }
        
    }

    public abstract InterestRateBean calculateInterests(DebitEntry debitEntry, LocalDate paymentDate,
            boolean withAllInterestValues);
    
    public abstract InterestRateBean calculateAllInterestsByLockingAtDate(DebitEntry debitEntry, LocalDate lockDate);

    protected TreeMap<LocalDate, BigDecimal> createdInterestEntriesMap(DebitEntry debitEntry) {
        TreeMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();

        for (DebitEntry interestDebitEntry : debitEntry.getInterestDebitEntriesSet()) {
            if (interestDebitEntry.isAnnulled()) {
                continue;
            }

            if (!TreasuryConstants.isPositive(interestDebitEntry.getAvailableAmountForCredit())) {
                continue;
            }

            LocalDate interestEntryDateTime = interestDebitEntry.getEntryDateTime().toLocalDate();
            result.putIfAbsent(interestEntryDateTime, BigDecimal.ZERO);
            result.put(interestEntryDateTime,
                    result.get(interestEntryDateTime).add(interestDebitEntry.getAvailableAmountForCredit()));
        }

        return result;
    }
    
}
