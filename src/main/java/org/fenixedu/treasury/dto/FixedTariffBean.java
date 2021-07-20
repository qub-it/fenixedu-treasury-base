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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.joda.time.LocalDate;

public class FixedTariffBean implements ITreasuryBean {

    private FinantialInstitution finantialInstitution;
    private Product product;
    private List<TreasuryTupleDataSourceBean> productDataSource;
    private VatType vatType;
    private List<TreasuryTupleDataSourceBean> vatTypeDataSource;
    private FinantialEntity finantialEntity;
    private List<TreasuryTupleDataSourceBean> finantialEntityDataSource;
    private java.math.BigDecimal amount;
    private org.joda.time.LocalDate beginDate;
    private org.joda.time.LocalDate endDate;
    private org.fenixedu.treasury.domain.tariff.DueDateCalculationType dueDateCalculationType;
    private List<TreasuryTupleDataSourceBean> dueDateCalculationTypeDataSource;
    private org.joda.time.LocalDate fixedDueDate;
    private int numberOfDaysAfterCreationForDueDate;
    private boolean applyInterests;
    private FixedTariffInterestRateBean interestRate;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product value) {
        product = value;
    }

    public List<TreasuryTupleDataSourceBean> getProductDataSource() {
        return productDataSource;
    }

    public void setProductDataSource(List<Product> value) {
        this.productDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.getName().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public FixedTariffInterestRateBean getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(InterestRate value) {
        interestRate = new FixedTariffInterestRateBean(value);
    }

    public FinantialEntity getFinantialEntity() {
        return finantialEntity;
    }

    public void setFinantialEntity(FinantialEntity value) {
        finantialEntity = value;
    }

    public List<TreasuryTupleDataSourceBean> getFinantialEntityDataSource() {
        return finantialEntityDataSource;
    }

    public void setFinantialEntityDataSource(List<FinantialEntity> value) {
        this.finantialEntityDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.getName().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal value) {
        amount = value;
    }

    public org.joda.time.LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(org.joda.time.LocalDate value) {
        beginDate = value;
    }

    public org.joda.time.LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(org.joda.time.LocalDate value) {
        endDate = value;
    }

    public org.fenixedu.treasury.domain.tariff.DueDateCalculationType getDueDateCalculationType() {
        return dueDateCalculationType;
    }

    public void setDueDateCalculationType(org.fenixedu.treasury.domain.tariff.DueDateCalculationType value) {
        dueDateCalculationType = value;
    }

    public org.joda.time.LocalDate getFixedDueDate() {
        return fixedDueDate;
    }

    public void setFixedDueDate(org.joda.time.LocalDate value) {
        if (value != null) {
            fixedDueDate = value;
        } else {
            fixedDueDate = new LocalDate();
        }
    }

    public int getNumberOfDaysAfterCreationForDueDate() {
        return numberOfDaysAfterCreationForDueDate;
    }

    public void setNumberOfDaysAfterCreationForDueDate(int value) {
        numberOfDaysAfterCreationForDueDate = value;
    }

    public boolean getApplyInterests() {
        return applyInterests;
    }

    public void setApplyInterests(boolean value) {
        applyInterests = value;
    }

    public List<TreasuryTupleDataSourceBean> getDueDateCalculationTypeDataSource() {
        return dueDateCalculationTypeDataSource;
    }

    public void setDueDateCalculationTypeDataSource(List<org.fenixedu.treasury.domain.tariff.DueDateCalculationType> value) {
        this.dueDateCalculationTypeDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.toString());
            tuple.setText(x.getDescriptionI18N().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public FixedTariffBean() {
        this.interestRate = new FixedTariffInterestRateBean();
        this.setApplyInterests(false);
        this.setDueDateCalculationType(DueDateCalculationType.NO_DUE_DATE);
        List<DueDateCalculationType> dueDates = new ArrayList<DueDateCalculationType>();
        for (DueDateCalculationType dueDate : DueDateCalculationType.values()) {
            dueDates.add(dueDate);
        }
        this.setDueDateCalculationTypeDataSource(dueDates);
        this.setFixedDueDate(new LocalDate());
    }

    public FixedTariffBean(FixedTariff fixedTariff) {
        this();
        this.setProduct(fixedTariff.getProduct());
        this.setApplyInterests(fixedTariff.getApplyInterests());
        if (fixedTariff.getInterestRate() != null) {
            this.setInterestRate(fixedTariff.getInterestRate());
            this.setApplyInterests(true);
        } else {
            this.setApplyInterests(false);
        }
        this.setFinantialEntity(fixedTariff.getFinantialEntity());
        this.setAmount(fixedTariff.getAmount());
        this.setBeginDate(fixedTariff.getBeginDate().toLocalDate());
        this.setEndDate(fixedTariff.getEndDate().toLocalDate());
        this.setDueDateCalculationType(fixedTariff.getDueDateCalculationType());
        this.setFixedDueDate(fixedTariff.getFixedDueDate());
        this.setNumberOfDaysAfterCreationForDueDate(fixedTariff.getNumberOfDaysAfterCreationForDueDate());

        this.setFinantialEntityDataSource(fixedTariff.getFinantialEntity().getFinantialInstitution().getFinantialEntitiesSet()
                .stream().collect(Collectors.toList()));
        List<DueDateCalculationType> dueDates = new ArrayList<DueDateCalculationType>();
        for (DueDateCalculationType dueDate : DueDateCalculationType.values()) {
            dueDates.add(dueDate);
        }
        this.setDueDateCalculationTypeDataSource(dueDates);
        this.setFinantialInstitution(fixedTariff.getFinantialEntity().getFinantialInstitution());

    }

    public FinantialInstitution getFinantialInstitution() {
        return finantialInstitution;
    }

    public void setFinantialInstitution(FinantialInstitution finantialInstitution) {
        this.finantialInstitution = finantialInstitution;
    }

}
