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
package org.fenixedu.treasury.domain.payments;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.PaymentMethodReference;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.payments.integration.IPaymentRequestState;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.FenixFramework;

public abstract class PaymentRequest extends PaymentRequest_Base {

    public PaymentRequest() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setRequestDate(new DateTime());
        setResponsibleUsername(TreasuryConstants.getAuthenticatedUsername());
    }

    protected void init(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount, PaymentMethod paymentMethod) {

        // Ensure debit entries have payable amount
        for (DebitEntry debitEntry : debitEntries) {
            if (!TreasuryConstants.isPositive(debitEntry.getOpenAmount())) {
                throw new TreasuryDomainException("error.PaymentRequest.debit.entry.open.amount.must.be.greater.than.zero");
            }
        }

        for (Installment installment : installments) {
            if (!TreasuryConstants.isPositive(installment.getOpenAmount())) {
                throw new TreasuryDomainException("error.PaymentRequest.debit.entry.open.amount.must.be.greater.than.zero");
            }
        }

        if (getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        // ANIL 2024-06-03
        //
        // Check if the debts are blocking for backoffice
        debitEntries.forEach(de -> {

            if (TreasuryDebtProcessMainService.isBlockingPaymentInBackoffice(de)) {
                if (!TreasuryDebtProcessMainService.getBlockingPaymentReasonsForBackoffice(de).isEmpty()) {
                    LocalizedString reason =
                            TreasuryDebtProcessMainService.getBlockingPaymentReasonsForBackoffice(de).iterator().next();

                    throw new RuntimeException(reason.getContent());
                } else {
                    throw new TreasuryDomainException("error.PaymentRequest.not.possible.to.create.payment.request");
                }
            }

        });

        installments.forEach(installment -> {

            installment.getInstallmentEntriesSet().stream().map(de -> de.getDebitEntry()).forEach(de -> {

                if (TreasuryDebtProcessMainService.isBlockingPaymentInBackoffice(de)) {
                    if (!TreasuryDebtProcessMainService.getBlockingPaymentReasonsForBackoffice(de).isEmpty()) {
                        LocalizedString reason =
                                TreasuryDebtProcessMainService.getBlockingPaymentReasonsForBackoffice(de).iterator().next();

                        throw new RuntimeException(reason.getContent());
                    } else {
                        throw new TreasuryDomainException("error.PaymentRequest.not.possible.to.create.payment.request");
                    }
                }

            });

        });

        setDigitalPaymentPlatform(platform);
        setFinantialEntity(platform.getFinantialEntity());
        setDebtAccount(debtAccount);
        getDebitEntriesSet().addAll(debitEntries);
        getInstallmentsSet().addAll(installments);
        setPayableAmount(payableAmount);
        setPaymentMethod(paymentMethod);
    }

    protected void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.PaymentRequest.domainRoot.required");
        }

        if (getDigitalPaymentPlatform() == null) {
            throw new TreasuryDomainException("error.PaymentRequest.digitalPaymentPlatform.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.PaymentRequest.debtAccount.required");
        }

        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.PaymentRequest.paymentMethod.required");
        }

        if (!StringUtils.isEmpty(getMerchantTransactionId())
                && findBySibsGatewayMerchantTransactionId(getMerchantTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.sibsGatewayMerchantTransactionId.not.unique");
        }

        if (!StringUtils.isEmpty(getTransactionId()) && findBySibsGatewayTransactionId(getTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.sibsGatewayTransactionId.not.unique");
        }

        for (DebitEntry debitEntry : getDebitEntriesSet()) {

            // Ensure all debit entries are the same debt account
            if (debitEntry.getDebtAccount() != getDebtAccount()) {
                throw new TreasuryDomainException("error.PaymentRequest.debit.entry.not.same.debt.account");
            }
        }

        if (getFinantialEntity() == null) {
            throw new TreasuryDomainException("error.PaymentRequest.finantialEntity.required");
        }

        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(getDebitEntriesSet());
    }

    public LocalDate getDueDate() {
        if (super.getPaymentDueDate() != null) {
            return super.getPaymentDueDate();
        }

        Set<LocalDate> map = getDebitEntriesSet().stream().filter(d -> !d.isAnnulled()).map(InvoiceEntry::getDueDate)
                .collect(Collectors.toSet());
        map.addAll(getInstallmentsSet().stream().filter(i -> i.getPaymentPlan().getState().isOpen()).map(Installment::getDueDate)
                .collect(Collectors.toSet()));
        return map.stream().sorted().findFirst().orElse(null);
    }

    public String fillPaymentEntryMethodId() {
        Optional<PaymentMethodReference> methodReference = PaymentMethodReference
                .findUniqueActiveAndForDigitalPayments(getPaymentMethod(), getDebtAccount().getFinantialInstitution());

        if (methodReference.isPresent()) {
            return methodReference.get().buildPaymentReferenceId(this);
        }

        return null;
    }

    public abstract IPaymentRequestState getCurrentState();

    public abstract boolean isInCreatedState();

    public abstract boolean isInRequestedState();

    public abstract boolean isInPaidState();

    public abstract boolean isInAnnuledState();

    public Set<Customer> getReferencedCustomers() {
        return getReferencedCustomers(getDebitEntriesSet(), getInstallmentsSet());
    }

    public Set<Product> getReferencedProducts() {
        return getDebitEntriesSet().stream().map(d -> d.getProduct()).collect(Collectors.toSet());
    }

    protected boolean payAllDebitEntriesInterests() {
        return false;
    }

    public Set<SettlementNote> internalProcessPaymentInNormalPaymentMixingLegacyInvoices(BigDecimal paidAmount,
            DateTime paymentDate, String originDocumentNumber, String comments,
            Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction) {

        SettlementNoteBean bean =
                SettlementNoteBean.createForPaymentRequestProcessPayment(this, paymentDate, paidAmount, originDocumentNumber);

        SettlementNote settlementNote = SettlementNote.createSettlementNote(bean);

        settlementNote.setDocumentObservations(comments);

        if (settlementNote.getAdvancedPaymentCreditNote() != null) {
            settlementNote.getAdvancedPaymentCreditNote().setDocumentObservations(comments);
        }

        final Map<String, String> paymentEntryPropertiesMap = additionalPropertiesMapFunction.apply(this);

        PaymentEntry paymentEntry = settlementNote.getPaymentEntriesSet().iterator().next();
        paymentEntryPropertiesMap.putAll(paymentEntry.getPropertiesMap());

        paymentEntry.editPropertiesMap(paymentEntryPropertiesMap);

        return Sets.newHashSet(settlementNote);
    }

    // TODO: Test thoroughly
    public Set<SettlementNote> internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(BigDecimal amount,
            DateTime paymentDate, String originDocumentNumber, String comments,
            Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction) {
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            throw new RuntimeException("invalid call");
        }

        // Check if invoiceEntriesToPay have mixed invoice entries of certified in legacy erp and not
        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(getDebitEntriesSet());

        // If all invoice entries are not exported in legacy ERP then invoke the internalProcessPaymentInNormalPaymentMixingLegacyInvoices
        if (getDebitEntriesSet().stream()
                .allMatch(e -> e.getFinantialDocument() == null || !e.getFinantialDocument().isExportedInLegacyERP())) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(amount, paymentDate, originDocumentNumber, comments,
                    additionalPropertiesMapFunction);
        }

        final TreeSet<InvoiceEntry> sortedInvoiceEntriesToPay = Sets.newTreeSet(InvoiceEntry.COMPARE_BY_AMOUNT_AND_DUE_DATE);
        sortedInvoiceEntriesToPay.addAll(getDebitEntriesSet());

        //Process the payment of pending invoiceEntries
        //1. Find the InvoiceEntries
        //2. Create the SEttlementEntries and the SEttlementNote
        //2.1 create the "InterestRate entries"
        //3. Close the SettlementNote
        //4. If there is money for more, create a "pending" payment (CreditNote) in different settlement note for being used later
        //6. Create a SibsTransactionDetail
        BigDecimal availableAmount = amount;

        DocumentNumberSeries docNumberSeriesForPayments =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), getFinantialEntity());

        final SettlementNote settlementNote = SettlementNote.create(getFinantialEntity(), getDebtAccount(),
                docNumberSeriesForPayments, new DateTime(), paymentDate, originDocumentNumber, null);

        settlementNote.setDocumentObservations(comments);

        //######################################
        //1. Find the InvoiceEntries
        //2. Create the SEttlementEntries and the SEttlementNote
        //######################################

        if (getReferencedCustomers().size() == 1) {
            for (final InvoiceEntry entry : sortedInvoiceEntriesToPay) {
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal amountToPay = entry.getOpenAmount();
                if (amountToPay.compareTo(BigDecimal.ZERO) > 0) {
                    if (entry.isDebitNoteEntry()) {
                        DebitEntry debitEntry = (DebitEntry) entry;

                        if (debitEntry.getFinantialDocument().isPreparing()) {
                            debitEntry.getFinantialDocument().closeDocument();
                        }

                        //check if the amount to pay in the Debit Entry
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        if (debitEntry.getOpenAmount().equals(amountToPay)) {
                            //######################################
                            //2.1 create the "InterestRate entries"
                            //######################################
                            List<InterestRateBean> undebitedInterestRateBeansList =
                                    debitEntry.calculateUndebitedInterestValue(paymentDate.toLocalDate());
                            for (InterestRateBean calculateUndebitedInterestValue : undebitedInterestRateBeansList) {
                                if (TreasuryConstants.isPositive(calculateUndebitedInterestValue.getInterestAmount())) {
                                    DateTime whenInterestDebitEntryDateTime = calculateUndebitedInterestValue
                                            .getInterestDebitEntryDateTime() != null ? calculateUndebitedInterestValue
                                                    .getInterestDebitEntryDateTime() : paymentDate;

                                    debitEntry.createInterestRateDebitEntry(calculateUndebitedInterestValue,
                                            whenInterestDebitEntryDateTime, null);
                                }
                            }
                        }

                        SettlementEntry.create(entry, settlementNote, amountToPay, entry.getDescription(), paymentDate, true);

                        //Update the amount to Pay
                        availableAmount = availableAmount.subtract(amountToPay);

                    } else if (entry.isCreditNoteEntry()) {
                        SettlementEntry.create(entry, settlementNote, entry.getOpenAmount(), entry.getDescription(), paymentDate,
                                true);
                        //update the amount to Pay
                        availableAmount = availableAmount.add(amountToPay);
                    }
                } else {
                    //Ignore since the "open amount" is ZERO
                }
            }
        }

        if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            availableAmount = BigDecimal.ZERO;
        }

        //######################################
        //3. Close the SettlementNote
        //######################################

        final Map<String, String> paymentEntryPropertiesMap = additionalPropertiesMapFunction.apply(this);

        PaymentEntry.create(getPaymentMethod(), settlementNote, amount.subtract(availableAmount), fillPaymentEntryMethodId(),
                paymentEntryPropertiesMap);
        settlementNote.closeDocument();

        final Set<SettlementNote> result = Sets.newHashSet(settlementNote);

        //###########################################################################################
        //4. If there is money for more, create a "pending" payment (CreditNote) for being used later
        // which must be settled in different SettlementNote so the advancepayment can be integrated and
        // use with new certified invoices
        //###########################################################################################

        //if "availableAmount" still exists, then we must create a "pending Payment" or "CreditNote"
        if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
            final SettlementNote advancedPaymentSettlementNote = SettlementNote.create(getFinantialEntity(), getDebtAccount(),
                    docNumberSeriesForPayments, new DateTime(), paymentDate, originDocumentNumber, null);

            advancedPaymentSettlementNote.setDocumentObservations(comments);

            final String advancedPaymentCreditNoteComments =
                    String.format("%s [%s]", TreasuryConstants.treasuryBundleI18N("label.SettlementNote.advancedpayment")
                            .getContent(TreasuryConstants.DEFAULT_LANGUAGE), paymentDate.toString(TreasuryConstants.DATE_FORMAT));

            advancedPaymentSettlementNote.createAdvancedPaymentCreditNote(availableAmount, advancedPaymentCreditNoteComments,
                    originDocumentNumber);
            advancedPaymentSettlementNote.getAdvancedPaymentCreditNote().setDocumentObservations(comments);

            PaymentEntry.create(getPaymentMethod(), advancedPaymentSettlementNote, availableAmount, fillPaymentEntryMethodId(),
                    paymentEntryPropertiesMap);
            advancedPaymentSettlementNote.closeDocument();

            // Mark both documents as alread exported in legacy ERP
            advancedPaymentSettlementNote.setExportedInLegacyERP(true);
            advancedPaymentSettlementNote.setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
            advancedPaymentSettlementNote.getAdvancedPaymentCreditNote().setExportedInLegacyERP(true);
            advancedPaymentSettlementNote.getAdvancedPaymentCreditNote()
                    .setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));

            result.add(advancedPaymentSettlementNote);
        }

        return result;

    }

    public static Set<Customer> getReferencedCustomers(Set<DebitEntry> debitEntrySet, Set<Installment> installments) {
        final Set<Customer> result = Sets.newHashSet();

        for (final InvoiceEntry entry : debitEntrySet) {
            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).isForPayorDebtAccount()) {
                result.add(((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(entry.getDebtAccount().getCustomer());
        }

        for (final Installment entry : installments) {
            result.addAll(entry.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry())
                    .map(deb -> (deb.getFinantialDocument() != null && ((Invoice) deb.getFinantialDocument())
                            .isForPayorDebtAccount()) ? ((Invoice) deb.getFinantialDocument()).getPayorDebtAccount()
                                    .getCustomer() : deb.getDebtAccount().getCustomer())
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    public List<DebitEntry> getOrderedDebitEntries() {
        final List<DebitEntry> result = new ArrayList<DebitEntry>();
        getDebitEntriesSet().stream().collect(Collectors.toCollection(() -> result));
        Collections.sort(result, InvoiceEntry.COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION);

        if (result.size() != getDebitEntriesSet().size()) {
            throw new RuntimeException(
                    "error.PaymentEntry.getOrderedDebitEntries.ordered.result.not.equal.to.getDebitEntriesSet");
        }

        return result;
    }

    public List<Installment> getOrderedInstallments() {
        final List<Installment> result = new ArrayList<Installment>();
        getInstallmentsSet().stream().collect(Collectors.toCollection(() -> result));
        Collections.sort(result, Installment.COMPARE_BY_DUEDATE);

        return result;
    }

    // TODO ANIL 2025-07-11
    // The order should not be reversed
    public List<? extends PaymentRequestLog> getOrderedPaymentLogs() {
        return getPaymentRequestLogsSet().stream().sorted(PaymentRequestLog.COMPARE_BY_CREATION_DATE.reversed())
                .collect(Collectors.toList());
    }

    public void delete() {
    }

    public BigDecimal getRemainingAmountInDebt(SibsPaymentRequest p) {
        return p.getDebitEntriesSet().stream().map(d -> d.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(p.getInstallmentsSet().stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public Set<PaymentRequest> getPaymentRequestsOfAssociatedDebtsInPaidStateIncludingSelf() {
        Set<PaymentRequest> result = new HashSet<>();

        getDebitEntriesSet().stream().flatMap(de -> de.getPaymentRequestsSet().stream())
                .filter(pr -> pr.isInPaidState())
                .collect(Collectors.toCollection(() -> result));

        getInstallmentsSet().stream().flatMap(de -> de.getPaymentRequestsSet().stream())
                .filter(pr -> pr.isInPaidState())
                .collect(Collectors.toCollection(() -> result));

        return result;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */

    // @formatter:on

    public static Stream<? extends PaymentRequest> findAll() {
        return FenixFramework.getDomainRoot().getPaymentRequestsSet().stream();
    }

    public static Stream<? extends PaymentRequest> findBySibsGatewayMerchantTransactionId(String merchantTransactionId) {
        return findAll().filter(p -> merchantTransactionId.equalsIgnoreCase(p.getMerchantTransactionId()));
    }

    public static Stream<? extends PaymentRequest> findBySibsGatewayTransactionId(String transactionId) {
        return findAll().filter(p -> transactionId.equalsIgnoreCase(p.getTransactionId()));
    }

    public static Optional<? extends PaymentRequest> findUniqueBySibsGatewayTransactionId(String transactionId) {
        return findBySibsGatewayTransactionId(transactionId).findAny();
    }

    public abstract String getUiDescription();

}
