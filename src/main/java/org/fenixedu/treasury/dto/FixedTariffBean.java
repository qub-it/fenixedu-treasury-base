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
