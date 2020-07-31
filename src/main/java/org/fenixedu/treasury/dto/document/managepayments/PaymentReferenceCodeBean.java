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
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class PaymentReferenceCodeBean implements ITreasuryBean {

    private DigitalPaymentPlatform paymentCodePool;
    private List<TreasuryTupleDataSourceBean> paymentCodePoolDataSource;
    private String referenceCode;
    private LocalDate validFrom;
    private LocalDate validTo;
    private BigDecimal maxAmount;
    private BigDecimal minAmount;
    private BigDecimal paymentAmount;
    private boolean isPoolWithFixedAmount;
    private boolean isPoolVariableTimeWindow;
    private boolean usePaymentAmountWithInterests;

    // Several debit entries
    private DebtAccount debtAccount;
    private List<DebitEntry> selectedDebitEntries;

    // MbwayPaymentRequest
    private String phoneNumberCountryPrefix;
    private String phoneNumber;

    public PaymentReferenceCodeBean() {
        this.usePaymentAmountWithInterests = false;
    }

    public PaymentReferenceCodeBean(final DigitalPaymentPlatform digitalPaymentPlatform, final DebtAccount debtAccount) {
        this.paymentCodePool = digitalPaymentPlatform;
        this.debtAccount = debtAccount;
        this.usePaymentAmountWithInterests = false;

        this.paymentCodePoolDataSource =
                DigitalPaymentPlatform.findForSibsPaymentCodeService(debtAccount.getFinantialInstitution())
                        .filter(x -> x.isActive()).map(ISibsPaymentCodePoolService.class::cast)
                        .map(x -> new TreasuryTupleDataSourceBean(x.getExternalId(),
                                String.format("[%s] - %s", x.getEntityReferenceCode(), x.getName())))
                        .collect(Collectors.toList());
    }

    public void updateAmountOnSelectedDebitEntries() {
        this.paymentAmount = this.selectedDebitEntries.stream()
                .map(e -> isUsePaymentAmountWithInterests() ? e.getOpenAmountWithInterests() : e.getOpenAmount())
                .reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);
    }

    public List<DebitEntry> getOpenDebitEntries() {
        return DebitEntry.find(debtAccount).filter(x -> !x.isAnnulled() && TreasuryConstants.isPositive(x.getOpenAmount()))
                .sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID).collect(Collectors.<DebitEntry> toList());
    }

    public DigitalPaymentPlatform getPaymentCodePool() {
        return paymentCodePool;
    }

    public void setPaymentCodePool(DigitalPaymentPlatform value) {
        paymentCodePool = value;
    }

    public List<TreasuryTupleDataSourceBean> getPaymentCodePoolDataSource() {
        return paymentCodePoolDataSource;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String value) {
        referenceCode = value;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate value) {
        validFrom = value;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate value) {
        validTo = value;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal value) {
        maxAmount = value;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal value) {
        minAmount = value;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
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

}
