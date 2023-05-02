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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateType;

public class FixedTariffInterestRateBean implements ITreasuryBean {

    private InterestRateType interestRateType;
    private List<TreasuryTupleDataSourceBean> interestTypeDataSource;
    private int numberOfDaysAfterDueDate;
    private boolean applyInFirstWorkday;
    private int maximumDaysToApplyPenalty;
    private BigDecimal interestFixedAmount;
    private BigDecimal rate;

    public InterestRateType getInterestRateType() {
        return interestRateType;
    }

    public void setInterestRateType(InterestRateType value) {
        interestRateType = value;
    }

    public List<TreasuryTupleDataSourceBean> getInterestTypeDataSource() {
        return interestTypeDataSource;
    }

    public void setInterestTypeDataSource(List<org.fenixedu.treasury.domain.tariff.InterestRateType> value) {
        this.interestTypeDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.toString());
            tuple.setText(x.getDescription().getContent());
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
        TreasurySettings.getInstance().getAvailableInterestRateTypesSet().stream() //
            .sorted(InterestRateType.COMPARE_BY_NAME) //
            .map(type -> new TreasuryTupleDataSourceBean(type.getExternalId(), type.getDescription().getContent())) //
            .collect(Collectors.toCollection(() -> this.interestTypeDataSource));
    }

    public FixedTariffInterestRateBean(InterestRate interestRate) {
        this();
        this.setInterestRateType(interestRate.getInterestRateType());
        this.setNumberOfDaysAfterDueDate(interestRate.getNumberOfDaysAfterDueDate());
        this.setApplyInFirstWorkday(interestRate.getApplyInFirstWorkday());
        this.setMaximumDaysToApplyPenalty(interestRate.getMaximumDaysToApplyPenalty());
        this.setInterestFixedAmount(interestRate.getInterestFixedAmount());
        this.setRate(interestRate.getRate());
    }
}
