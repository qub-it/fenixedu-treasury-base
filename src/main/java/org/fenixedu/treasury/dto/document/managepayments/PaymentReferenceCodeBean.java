/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
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

package org.fenixedu.treasury.dto.document.managepayments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.util.TreasuryConstants;

public class PaymentReferenceCodeBean implements ITreasuryBean {

    private DebitNote debitNote;
    private PaymentCodePool paymentCodePool;
    private List<TreasuryTupleDataSourceBean> paymentCodePoolDataSource;
    private java.lang.String referenceCode;
    private org.joda.time.LocalDate beginDate;
    private org.joda.time.LocalDate endDate;
    private java.math.BigDecimal maxAmount;
    private java.math.BigDecimal minAmount;
    private java.math.BigDecimal paymentAmount;
    private java.math.BigDecimal paymentAmountWithInterst;
    private boolean isPoolWithFixedAmount;
    private boolean isPoolVariableTimeWindow;
    private boolean useCustomPaymentAmount;
    private boolean usePaymentAmountWithInterests;

    // Several debit entries
    private DebtAccount debtAccount;
    private List<DebitEntry> selectedDebitEntries;

    // PaymentPlan Installments
    private List<Installment> selectedInstallments;

    // MbwayPaymentRequest
    private String phoneNumberCountryPrefix;
    private String phoneNumber;

    public PaymentReferenceCodeBean() {
        usePaymentAmountWithInterests = false;
        useCustomPaymentAmount = false;
        selectedDebitEntries = new ArrayList<>();
        selectedInstallments = new ArrayList<>();
    }

    public PaymentReferenceCodeBean(final PaymentCodePool paymentCodePool, final DebtAccount debtAccount) {
        this();
        this.paymentCodePool = paymentCodePool;
        this.debtAccount = debtAccount;

        List<PaymentCodePool> activePools = debtAccount.getFinantialInstitution().getPaymentCodePoolsSet().stream()
                .filter(x -> Boolean.TRUE.equals(x.getActive())).collect(Collectors.toList());
        setPaymentCodePoolDataSource(activePools);
    }

    public void updateAmountOnSelectedDebitEntries() {
        BigDecimal paymentAmountDebitEnries = this.selectedDebitEntries.stream()
                .map(e -> isUsePaymentAmountWithInterests() ? e.getOpenAmountWithInterests() : e.getOpenAmount())
                .reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);
        BigDecimal paymentAmountInstallments =
                this.selectedInstallments.stream().map(e -> e.getOpenAmount()).reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);

        this.paymentAmount = paymentAmountDebitEnries.add(paymentAmountInstallments);
    }

    public List<DebitEntry> getOpenDebitEntries() {
        return DebitEntry.find(debtAccount).filter(x -> !x.isAnnulled() && TreasuryConstants.isPositive(x.getOpenAmount()))
                .sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID).collect(Collectors.<DebitEntry> toList());
    }

    public List<Installment> getOpenInstallments() {
        return debtAccount.getActivePaymentPlansSet().stream().flatMap(i -> i.getSortedOpenInstallments().stream())
                .sorted(Installment.COMPARE_BY_DUEDATE).collect(Collectors.<Installment> toList());
    }

    public PaymentCodePool getPaymentCodePool() {
        return paymentCodePool;
    }

    public void setPaymentCodePool(PaymentCodePool value) {
        paymentCodePool = value;
    }

    public List<TreasuryTupleDataSourceBean> getPaymentCodePoolDataSource() {
        return paymentCodePoolDataSource;
    }

    public void setPaymentCodePoolDataSource(List<PaymentCodePool> value) {
        this.paymentCodePoolDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText("[" + x.getEntityReferenceCode() + "] - " + x.getName());
            return tuple;
        }).collect(Collectors.toList());
    }

    public java.lang.String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(java.lang.String value) {
        referenceCode = value;
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

    public java.math.BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(java.math.BigDecimal value) {
        maxAmount = value;
    }

    public java.math.BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(java.math.BigDecimal value) {
        minAmount = value;
    }

    public DebitNote getDebitNote() {
        return debitNote;
    }

    public void setDebitNote(DebitNote debitNote) {
        this.debitNote = debitNote;
    }

    public java.math.BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(java.math.BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public boolean isUsePaymentAmountWithInterests() {
        return usePaymentAmountWithInterests;
    }

    public void setUsePaymentAmountWithInterests(boolean usePaymentAmountWithInterests) {
        this.usePaymentAmountWithInterests = usePaymentAmountWithInterests;
    }

    public boolean isPoolWithFixedAmount() {
        return isPoolWithFixedAmount;
    }

    public void setPoolWithFixedAmount(boolean poolWithFixedAmount) {
        this.isPoolWithFixedAmount = poolWithFixedAmount;
    }

    public boolean isPoolVariableTimeWindow() {
        return isPoolVariableTimeWindow;
    }

    public void setPoolVariableTimeWindow(boolean isPoolVariableTimeWindow) {
        this.isPoolVariableTimeWindow = isPoolVariableTimeWindow;
    }

    public boolean isUseCustomPaymentAmount() {
        return useCustomPaymentAmount;
    }

    public void setUseCustomPaymentAmount(boolean useCustomPaymentAmount) {
        this.useCustomPaymentAmount = useCustomPaymentAmount;
    }

    public List<DebitEntry> getSelectedDebitEntries() {
        return selectedDebitEntries;
    }

    public void setSelectedDebitEntries(List<DebitEntry> selectedDebitEntries) {
        this.selectedDebitEntries = selectedDebitEntries;
    }

    public String getPhoneNumberCountryPrefix() {
        return this.phoneNumberCountryPrefix;
    };

    public void setPhoneNumberCountryPrefix(String phoneNumberCountryPrefix) {
        this.phoneNumberCountryPrefix = phoneNumberCountryPrefix;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Installment> getSelectedInstallments() {
        return selectedInstallments;
    }

    public void setSelectedInstallments(List<Installment> selectedInstallments) {
        this.selectedInstallments = selectedInstallments;
    }

}
