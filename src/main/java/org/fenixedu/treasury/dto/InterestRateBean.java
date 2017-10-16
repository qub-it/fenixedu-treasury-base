package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.treasury.domain.tariff.InterestType;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

public class InterestRateBean implements ITreasuryBean, Serializable {

    private static final long serialVersionUID = 1L;

    public static class InterestInformationDetail implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal amount;
        private LocalDate begin;
        private LocalDate end;
        private BigDecimal amountPerUnit;
        private BigDecimal affectedAmount;

        private InterestInformationDetail(final BigDecimal amount, final LocalDate begin, final LocalDate end,
                final BigDecimal amountPerUnit, final BigDecimal affectedAmount) {
            this.amount = amount;
            this.begin = begin;
            this.end = end;
            this.affectedAmount = affectedAmount;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public LocalDate getBegin() {
            return begin;
        }

        public LocalDate getEnd() {
            return end;
        }

        public BigDecimal getAmountPerUnit() {
            return amountPerUnit;
        }
        
        public int getNumberOfDays() {
            return Days.daysBetween(begin, end).getDays() + 1;
        }
        
        public BigDecimal getAffectedAmount() {
            return affectedAmount;
        }
    }

    public static class CreatedInterestEntry implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private LocalDate entryDate;
        private BigDecimal amount;

        private CreatedInterestEntry(final LocalDate entryDate, final BigDecimal amount) {
            this.entryDate = entryDate;
            this.amount = amount;
        }

        public LocalDate getEntryDate() {
            return entryDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    private BigDecimal interestAmount;
    private InterestType interestType;
    private int numberOfDays;
    private int numberOfMonths;
    private String description;

    private List<InterestInformationDetail> interestInformationList = Lists.newArrayList();
    private List<CreatedInterestEntry> createdInterestEntriesList = Lists.newArrayList();

    public InterestRateBean() {
        setInterestType(null);
        setInterestAmount(BigDecimal.ZERO);
        setNumberOfDays(0);
        setNumberOfMonths(0);
    }

    public InterestRateBean(final InterestType interestType) {
        setInterestType(interestType);
    }

    public InterestInformationDetail addDetail(final BigDecimal amount, final LocalDate begin, final LocalDate end,
            final BigDecimal amountPerUnit, final BigDecimal affectedAmount) {
        final InterestInformationDetail detail = new InterestInformationDetail(amount, begin, end, amountPerUnit, affectedAmount);

        interestInformationList.add(detail);

        return detail;
    }

    public CreatedInterestEntry addCreatedInterestEntry(final LocalDate entryDate, final BigDecimal amount) {
        final CreatedInterestEntry entry = new CreatedInterestEntry(entryDate, amount);

        createdInterestEntriesList.add(entry);

        return entry;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal amount) {
        this.interestAmount = amount;
    }

    public InterestType getInterestType() {
        return interestType;
    }

    public void setInterestType(InterestType interestType) {
        this.interestType = interestType;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public int getNumberOfMonths() {
        return numberOfMonths;
    }

    public void setNumberOfMonths(int numberOfMonths) {
        this.numberOfMonths = numberOfMonths;
    }

    public List<InterestInformationDetail> getInterestInformationList() {
        return interestInformationList;
    }

    public List<CreatedInterestEntry> getCreatedInterestEntriesList() {
        return createdInterestEntriesList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
