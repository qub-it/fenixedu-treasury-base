/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fenixedu.treasury.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestType;

public class FixedTariffInterestRateBean implements ITreasuryBean {

    private org.fenixedu.treasury.domain.tariff.InterestType interestType;
    private List<TreasuryTupleDataSourceBean> interestTypeDataSource;
    private int numberOfDaysAfterDueDate;
    private boolean applyInFirstWorkday;
    private int maximumDaysToApplyPenalty;
    private int maximumMonthsToApplyPenalty;
    private java.math.BigDecimal interestFixedAmount;
    private java.math.BigDecimal rate;

    public org.fenixedu.treasury.domain.tariff.InterestType getInterestType() {
        return interestType;
    }

    public void setInterestType(org.fenixedu.treasury.domain.tariff.InterestType value) {
        interestType = value;
    }

    public List<TreasuryTupleDataSourceBean> getInterestTypeDataSource() {
        return interestTypeDataSource;
    }

    public void setInterestTypeDataSource(List<org.fenixedu.treasury.domain.tariff.InterestType> value) {
        this.interestTypeDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.toString());
            tuple.setText(x.getDescriptionI18N().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public int getNumberOfDaysAfterDueDate() {
        return numberOfDaysAfterDueDate;
    }

    public void setNumberOfDaysAfterDueDate(int value) {
        numberOfDaysAfterDueDate = value;
    }

    public boolean getApplyInFirstWorkday() {
        return applyInFirstWorkday;
    }

    public void setApplyInFirstWorkday(boolean value) {
        applyInFirstWorkday = value;
    }

    public int getMaximumDaysToApplyPenalty() {
        return maximumDaysToApplyPenalty;
    }

    public void setMaximumDaysToApplyPenalty(int value) {
        maximumDaysToApplyPenalty = value;
    }

    public int getMaximumMonthsToApplyPenalty() {
        return maximumMonthsToApplyPenalty;
    }

    public void setMaximumMonthsToApplyPenalty(int value) {
        maximumMonthsToApplyPenalty = value;
    }

    public java.math.BigDecimal getInterestFixedAmount() {
        return interestFixedAmount;
    }

    public void setInterestFixedAmount(java.math.BigDecimal value) {
        interestFixedAmount = value;
    }

    public java.math.BigDecimal getRate() {
        return rate;
    }

    public void setRate(java.math.BigDecimal value) {
        rate = value;
    }

    public FixedTariffInterestRateBean() {
        this.interestTypeDataSource = new ArrayList<TreasuryTupleDataSourceBean>();
        for (InterestType type : InterestType.findAll()) {
            TreasuryTupleDataSourceBean typeBean = new TreasuryTupleDataSourceBean();
            typeBean.setId(type.toString());
            typeBean.setText(type.getDescriptionI18N().getContent());
            this.interestTypeDataSource.add(typeBean);
        }
    }

    public FixedTariffInterestRateBean(InterestRate interestRate) {
        this();
        this.setInterestType(interestRate.getInterestType());
        this.setNumberOfDaysAfterDueDate(interestRate.getNumberOfDaysAfterDueDate());
        this.setApplyInFirstWorkday(interestRate.getApplyInFirstWorkday());
        this.setMaximumDaysToApplyPenalty(interestRate.getMaximumDaysToApplyPenalty());
        this.setMaximumMonthsToApplyPenalty(interestRate.getMaximumMonthsToApplyPenalty());
        this.setInterestFixedAmount(interestRate.getInterestFixedAmount());
        this.setRate(interestRate.getRate());
    }
}