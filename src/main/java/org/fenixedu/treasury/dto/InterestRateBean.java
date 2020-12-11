/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.Interval;
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
        private BigDecimal interestRate;

        private InterestInformationDetail(BigDecimal amount, LocalDate begin, LocalDate end,
                BigDecimal amountPerUnit, BigDecimal affectedAmount, BigDecimal interestRate) {
            this.amount = amount;
            this.begin = begin;
            this.end = end;
            this.affectedAmount = affectedAmount;
            this.interestRate = interestRate;
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
        
        public BigDecimal getInterestRate() {
            return interestRate;
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
            final BigDecimal amountPerUnit, final BigDecimal affectedAmount, final BigDecimal interestRate) {
        final InterestInformationDetail detail = new InterestInformationDetail(amount, begin, end, amountPerUnit, affectedAmount, interestRate);

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
