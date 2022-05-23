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
package org.fenixedu.treasury.dto.document.managepayments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
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
        this.usePaymentAmountWithInterests = false;
    }

    public PaymentReferenceCodeBean(final DigitalPaymentPlatform digitalPaymentPlatform, final DebtAccount debtAccount) {
        this();
        this.paymentCodePool = digitalPaymentPlatform;
        this.debtAccount = debtAccount;

        this.paymentCodePoolDataSource =
                DigitalPaymentPlatform.findForSibsPaymentCodeService(debtAccount.getFinantialInstitution())
                        .filter(x -> x.isActive()).map(ISibsPaymentCodePoolService.class::cast)
                        .map(x -> new TreasuryTupleDataSourceBean(x.getExternalId(),
                                String.format("[%s] - %s", x.getEntityReferenceCode(), x.getName())))
                        .collect(Collectors.toList());
    }

    public BigDecimal getExtraAmount(DebitEntry debitEntry) {

        PaymentPenaltyEntryBean penaltyTax =
                PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(debitEntry, LocalDate.now());

        BigDecimal penaltyTaxAmount = penaltyTax != null ? penaltyTax.getSettledAmount() : BigDecimal.ZERO;

        return debitEntry.getOpenAmountWithInterests().add(penaltyTaxAmount);
    }

    public void updateAmountOnSelectedDebitEntries() {

        BigDecimal paymentAmountDebitEnries = this.selectedDebitEntries.stream()
                .map(e -> isUsePaymentAmountWithInterests() ? getExtraAmount(e) : e.getOpenAmount()).reduce((a, c) -> a.add(c))
                .orElse(BigDecimal.ZERO);
        BigDecimal paymentAmountInstallments =
                this.selectedInstallments.stream().map(e -> e.getOpenAmount()).reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);

        this.paymentAmount = paymentAmountDebitEnries.add(paymentAmountInstallments);
    }

    public List<DebitEntry> getOpenDebitEntries() {
        return DebitEntry.find(debtAccount) //
                .filter(x -> !x.isAnnulled() && TreasuryConstants.isPositive(x.getOpenAmount())) //
                .filter(x -> !TreasuryDebtProcessMainService.isBlockingPaymentInFrontend(x)) //
                .sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID) //
                .collect(Collectors.<DebitEntry> toList());
    }

    public List<Installment> getOpenInstallments() {
        return debtAccount.getActivePaymentPlansSet().stream() //
                .flatMap(i -> i.getSortedOpenInstallments().stream()) //
                .filter(i -> !someEntriesIsBlockedInFrontendPayment(i)) //
                .sorted(Installment.COMPARE_BY_DUEDATE) //
                .collect(Collectors.<Installment> toList());
    }

    private boolean someEntriesIsBlockedInFrontendPayment(Installment installment) {
        return installment.getInstallmentEntriesSet().stream()
                .anyMatch(e -> TreasuryDebtProcessMainService.isBlockingPaymentInFrontend(e.getDebitEntry()));
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
