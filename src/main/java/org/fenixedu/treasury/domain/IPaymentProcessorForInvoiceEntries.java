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
package org.fenixedu.treasury.domain;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Deprecated
public interface IPaymentProcessorForInvoiceEntries {

    public DebtAccount getDebtAccount();

    public DocumentNumberSeries getDocumentSeriesForPayments();

    public DocumentNumberSeries getDocumentSeriesInterestDebits();

    public Set<Customer> getReferencedCustomers();

    public BigDecimal getPayableAmount();

    public DateTime getPaymentRequestDate();

    public String getPaymentRequestStateDescription();

    public String getPaymentTypeDescription();

    public PaymentMethod getPaymentMethod();

    public Set<InvoiceEntry> getInvoiceEntriesSet();

    public Set<Installment> getInstallmentsSet();

    public String fillPaymentEntryMethodId();

    public SibsOnlinePaymentsGateway getSibsOnlinePaymentsGateway();

    public String getSibsOppwaMerchantTransactionId();

    public String getSibsOppwaTransactionId();

    default public boolean isMbwayRequest() {
        return false;
    }

    default public boolean isPaymentCodeTarget() {
        return false;
    }

    default public boolean isForwardPayment() {
        return false;
    }

    default public boolean payAllDebitEntriesInterests() {
        return false;
    }

    default public Set<SettlementNote> internalProcessPaymentInNormalPaymentMixingLegacyInvoices(String username,
            BigDecimal amount, DateTime paymentDate, String originDocumentNumber, String comments,
            Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction) {

        final TreeSet<InvoiceEntry> sortedInvoiceEntriesToPay = Sets.newTreeSet(InvoiceEntry.COMPARE_BY_AMOUNT_AND_DUE_DATE);
        sortedInvoiceEntriesToPay.addAll(invoiceEntriesToPay);

        final TreeSet<InstallmentEntry> sortedInstallmentEntryToPay =
                Sets.newTreeSet(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR);

        sortedInstallmentEntryToPay.addAll(
                installmentsToPay.stream().flatMap(i -> i.getInstallmentEntriesSet().stream()).collect(Collectors.toSet()));

        // Process the payment of pending invoiceEntries
        // 1. Find the InvoiceEntries
        // 2. Create the SEttlementEntries and the SEttlementNote
        // 2.1 create the "InterestRate entries"
        // 2.2 if there is pending amount, pay the interest rate
        // 3. If there is pending amount, try to pay a Pending DebitEntries
        // 3.1 create the interestRate entries for pending debit entries
        // 3.2 if there is pending amount, try to pay interesrtRate for pending debit
        // entries
        // 4. If there is money for more, create a "pending" payment (CreditNote) for
        // being used later
        // 5. Close the SettlementNote
        // 6. Create a SibsTransactionDetail
        BigDecimal availableAmount = amount;

        List<DebitEntry> interestRateEntries = new ArrayList<DebitEntry>();
        DebtAccount referenceDebtAccount = this.getDebtAccount();
        DocumentNumberSeries docNumberSeries = this.getDocumentSeriesForPayments();
        SettlementNote settlementNote =
                SettlementNote.create(referenceDebtAccount, docNumberSeries, new DateTime(), paymentDate, comments, null);

        // ######################################
        // 1. Find the InvoiceEntries
        // 2. Create the SEttlementEntries and the SEttlementNote
        // ######################################

        if (getReferencedCustomers().size() == 1) {
            for (InvoiceEntry entry : sortedInvoiceEntriesToPay) {
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal amountToPay = entry.getOpenAmount();
                if (amountToPay.compareTo(BigDecimal.ZERO) > 0) {
                    if (entry.isDebitNoteEntry()) {
                        DebitEntry debitEntry = (DebitEntry) entry;

                        if (debitEntry.getFinantialDocument() == null) {
                            final DocumentNumberSeries documentNumberSeries =
                                    DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                            getDebtAccount().getFinantialInstitution()).get();
                            final DebitNote debitNote =
                                    DebitNote.create(debitEntry.getDebtAccount(), documentNumberSeries, new DateTime());
                            debitNote.addDebitNoteEntries(Lists.newArrayList(debitEntry));
                        }

                        if (debitEntry.getFinantialDocument().isPreparing()) {
                            debitEntry.getFinantialDocument().closeDocument();
                        }

                        // check if the amount to pay in the Debit Entry exceeds the availableAmount
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        if (debitEntry.getOpenAmount().equals(amountToPay)) {
                            if (payAllDebitEntriesInterests()) {
                                for (DebitEntry interestDebitEntry : debitEntry.getInterestDebitEntriesSet()) {
                                    if (interestDebitEntry.isAnnulled()) {
                                        continue;
                                    }

                                    if (!interestDebitEntry.isInDebt()) {
                                        continue;
                                    }

                                    if (sortedInvoiceEntriesToPay.contains(interestDebitEntry)) {
                                        continue;
                                    }

                                    interestRateEntries.add(interestDebitEntry);
                                }
                            }
                            // ######################################
                            // 2.1 create the "InterestRate entries"
                            // ######################################
                            InterestRateBean calculateUndebitedInterestValue =
                                    debitEntry.calculateUndebitedInterestValue(paymentDate.toLocalDate());
                            if (TreasuryConstants.isPositive(calculateUndebitedInterestValue.getInterestAmount())) {
                                DebitEntry interestDebitEntry = debitEntry.createInterestRateDebitEntry(
                                        calculateUndebitedInterestValue, paymentDate, Optional.<DebitNote> empty());
                                interestRateEntries.add(interestDebitEntry);
                            }
                        }

                        SettlementEntry settlementEntry = SettlementEntry.create(entry, settlementNote, amountToPay,
                                entry.getDescription(), paymentDate, true);

                        InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(settlementEntry);

                        // Update the amount to Pay
                        availableAmount = availableAmount.subtract(amountToPay);

                    } else if (entry.isCreditNoteEntry()) {
                        SettlementEntry.create(entry, settlementNote, entry.getOpenAmount(), entry.getDescription(), paymentDate,
                                true);
                        // update the amount to Pay
                        availableAmount = availableAmount.add(amountToPay);
                    }
                } else {
                    // Ignore since the "open amount" is ZERO
                }
            }
            
            for (InstallmentEntry entry : sortedInstallmentEntryToPay) {
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal installmentAmountToPay = entry.getOpenAmount();
                if (installmentAmountToPay.compareTo(BigDecimal.ZERO) > 0) {
                    DebitEntry debitEntry = entry.getDebitEntry();

                    if (debitEntry.getFinantialDocument() == null) {
                        final DocumentNumberSeries documentNumberSeries =
                                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                        getDebtAccount().getFinantialInstitution()).get();
                        final DebitNote debitNote =
                                DebitNote.create(debitEntry.getDebtAccount(), documentNumberSeries, new DateTime());
                        debitNote.addDebitNoteEntries(Lists.newArrayList(debitEntry));
                    }

                    if (debitEntry.getFinantialDocument().isPreparing()) {
                        debitEntry.getFinantialDocument().closeDocument();
                    }

                    // check if the amount to pay in the installment entry exceeds the available amount
                    if (installmentAmountToPay.compareTo(availableAmount) > 0) {
                        installmentAmountToPay = availableAmount;
                    }
                    SettlementEntry settlementEntry = settlementNote.getSettlementEntryByDebitEntry(debitEntry);
                    if (settlementEntry == null) {
                        settlementEntry = SettlementEntry.create(debitEntry, settlementNote, installmentAmountToPay,
                                debitEntry.getDescription(), paymentDate, false);
                    } else {
                        settlementEntry.setAmount(settlementEntry.getAmount().add(installmentAmountToPay));
                    }
                    InstallmentSettlementEntry.create(entry, settlementEntry, installmentAmountToPay);
                    entry.getInstallment().getPaymentPlan().tryClosePaymentPlanByPaidOff();

                    // Update the amount to Pay
                    availableAmount = availableAmount.subtract(installmentAmountToPay);

                } else {
                    // Ignore since the "open amount" is ZERO
                }
            }
            // ######################################
            // 2.2 if there is pending amount, pay the interest rate
            // ######################################
            // if we created interestRateEntries then we must close them in a document and
            // try to pay with availableAmount
            if (interestRateEntries.size() > 0) {
                // Create a DebitNote for the Interests DebitEntries
                DebitNote interestNote =
                        DebitNote.create(referenceDebtAccount, this.getDocumentSeriesInterestDebits(), paymentDate);
                for (DebitEntry interestEntry : interestRateEntries) {
                    if(interestEntry.getFinantialDocument() != null) {
                        if(interestEntry.getFinantialDocument().isPreparing()) {
                            interestEntry.getFinantialDocument().closeDocument();
                        }
                    } else {
                        interestEntry.setFinantialDocument(interestNote);
                    }
                }
                
                interestNote.closeDocument();

                // if "availableAmount" still exists, then we must check if there is any
                // InterestRate to pay
                if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
                    for (DebitEntry interestEntry : interestRateEntries) {
                        // Check if there is enough amount to Pay
                        if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            break;
                        }

                        BigDecimal amountToPay = interestEntry.getOpenAmount();
                        // check if the amount to pay in the debit entry exceeds the available amount
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        SettlementEntry newSettlementEntry = SettlementEntry.create(interestEntry, settlementNote, amountToPay,
                                interestEntry.getDescription(), paymentDate, true);
                        // Update the amount to Pay
                        availableAmount = availableAmount.subtract(amountToPay);
                    }
                }
                interestRateEntries.clear();
            }
        }

        // ######################################
        // 4. If there is money for more, create a "pending" payment (CreditNote) for
        // being used later
        // ######################################

        // if "availableAmount" still exists, then we must create a "pending Payment" or
        // "CreditNote"
        if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
            settlementNote.createAdvancedPaymentCreditNote(availableAmount,
                    treasuryBundle("label.PaymentCodeTarget.advancedpayment") + comments + "-" + originDocumentNumber,
                    originDocumentNumber);
        }

        // ######################################
        // 5. Close the SettlementNote
        // ######################################

        final Map<String, String> paymentEntryPropertiesMap = additionalPropertiesMapFunction.apply(this);

        PaymentEntry.create(getPaymentMethod(), settlementNote, amount, fillPaymentEntryMethodId(), paymentEntryPropertiesMap);
        settlementNote.closeDocument();

        return Sets.newHashSet(settlementNote);
    }

    default public Set<SettlementNote> internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            final Set<InvoiceEntry> invoiceEntriesToPay, final Set<Installment> installmentsToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction) {
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            throw new RuntimeException("invalid call");
        }

        // Check if invoiceEntriesToPay have mixed invoice entries of certified in
        // legacy erp and not
        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(invoiceEntriesToPay);

        // If all invoice entries are not exported in legacy ERP then invoke the
        // internalProcessPaymentInNormalPaymentMixingLegacyInvoices
        if (invoiceEntriesToPay.stream()
                .allMatch(e -> e.getFinantialDocument() == null || !e.getFinantialDocument().isExportedInLegacyERP())) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(username, amount, paymentDate, sibsTransactionId,
                    comments, invoiceEntriesToPay, installmentsToPay, additionalPropertiesMapFunction);
        }

        final TreeSet<InvoiceEntry> sortedInvoiceEntriesToPay = Sets.newTreeSet(InvoiceEntry.COMPARE_BY_AMOUNT_AND_DUE_DATE);
        sortedInvoiceEntriesToPay.addAll(invoiceEntriesToPay);

        final TreeSet<InstallmentEntry> sortedInstallmentEntryToPay =
                Sets.newTreeSet(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR);

        sortedInstallmentEntryToPay.addAll(
                installmentsToPay.stream().flatMap(i -> i.getInstallmentEntriesSet().stream()).collect(Collectors.toSet()));

        // Process the payment of pending invoiceEntries
        // 1. Find the InvoiceEntries
        // 2. Create the SEttlementEntries and the SEttlementNote
        // 2.1 create the "InterestRate entries"
        // 3. Close the SettlementNote
        // 4. If there is money for more, create a "pending" payment (CreditNote) in
        // different settlement note for being used later
        // 6. Create a SibsTransactionDetail
        BigDecimal availableAmount = amount;

        final DebtAccount referenceDebtAccount = this.getDebtAccount();
        final DocumentNumberSeries docNumberSeries = this.getDocumentSeriesForPayments();
        final SettlementNote settlementNote =
                SettlementNote.create(referenceDebtAccount, docNumberSeries, new DateTime(), paymentDate, comments, null);

        // ######################################
        // 1. Find the InvoiceEntries
        // 2. Create the SEttlementEntries and the SEttlementNote
        // ######################################

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

                        // check if the amount to pay in the Debit Entry
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        if (debitEntry.getOpenAmount().equals(amountToPay)) {
                            // ######################################
                            // 2.1 create the "InterestRate entries"
                            // ######################################
                            InterestRateBean calculateUndebitedInterestValue =
                                    debitEntry.calculateUndebitedInterestValue(paymentDate.toLocalDate());
                            if (TreasuryConstants.isPositive(calculateUndebitedInterestValue.getInterestAmount())) {
                                debitEntry.createInterestRateDebitEntry(calculateUndebitedInterestValue, paymentDate,
                                        Optional.<DebitNote> empty());
                            }
                        }

                        SettlementEntry settlementEntry = SettlementEntry.create(entry, settlementNote, amountToPay, entry.getDescription(), paymentDate, true);

                        InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(settlementEntry);
                        
                        // Update the amount to Pay
                        availableAmount = availableAmount.subtract(amountToPay);

                    } else if (entry.isCreditNoteEntry()) {
                        SettlementEntry.create(entry, settlementNote, entry.getOpenAmount(), entry.getDescription(), paymentDate,
                                true);
                        // update the amount to Pay
                        availableAmount = availableAmount.add(amountToPay);
                    }
                } else {
                    // Ignore since the "open amount" is ZERO
                }
            }
        }

        for (InstallmentEntry entry : sortedInstallmentEntryToPay) {
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal installmentAmountToPay = entry.getOpenAmount();
            if (installmentAmountToPay.compareTo(BigDecimal.ZERO) > 0) {
                DebitEntry debitEntry = entry.getDebitEntry();

                if (debitEntry.getFinantialDocument().isPreparing()) {
                    debitEntry.getFinantialDocument().closeDocument();
                }

                // check if the amount to pay in the installment entry exceeds the available amount
                if (installmentAmountToPay.compareTo(availableAmount) > 0) {
                    installmentAmountToPay = availableAmount;
                }
                
                SettlementEntry settlementEntry = settlementNote.getSettlementEntryByDebitEntry(debitEntry);
                if (settlementEntry == null) {
                    settlementEntry = SettlementEntry.create(debitEntry, settlementNote, installmentAmountToPay,
                            debitEntry.getDescription(), paymentDate, false);
                } else {
                    settlementEntry.setAmount(settlementEntry.getAmount().add(installmentAmountToPay));
                }
                
                InstallmentSettlementEntry.create(entry, settlementEntry, installmentAmountToPay);
                entry.getInstallment().getPaymentPlan().tryClosePaymentPlanByPaidOff();

                // Update the amount to Pay
                availableAmount = availableAmount.subtract(installmentAmountToPay);

            } else {
                // Ignore since the "open amount" is ZERO
            }
        }
        
        if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            availableAmount = BigDecimal.ZERO;
        }

        // ######################################
        // 3. Close the SettlementNote
        // ######################################

        final Map<String, String> paymentEntryPropertiesMap = additionalPropertiesMapFunction.apply(this);

        PaymentEntry.create(getPaymentMethod(), settlementNote, amount.subtract(availableAmount), fillPaymentEntryMethodId(),
                paymentEntryPropertiesMap);
        settlementNote.closeDocument();

        final Set<SettlementNote> result = Sets.newHashSet(settlementNote);

        // ###########################################################################################
        // 4. If there is money for more, create a "pending" payment (CreditNote) for
        // being used later
        // which must be settled in different SettlementNote so the advancepayment can
        // be integrated and
        // use with new certified invoices
        // ###########################################################################################

        // if "availableAmount" still exists, then we must create a "pending Payment" or
        // "CreditNote"
        if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
            final SettlementNote advancedPaymentSettlementNote =
                    SettlementNote.create(referenceDebtAccount, docNumberSeries, new DateTime(), paymentDate, comments, null);

            advancedPaymentSettlementNote.createAdvancedPaymentCreditNote(availableAmount,
                    treasuryBundle("label.PaymentCodeTarget.advancedpayment") + comments + "-" + sibsTransactionId,
                    sibsTransactionId);

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

    public static Set<Customer> getReferencedCustomers(final Set<InvoiceEntry> invoiceEntrySet,
            final Set<Installment> installments) {
        final Set<Customer> result = Sets.newHashSet();
        for (final InvoiceEntry entry : invoiceEntrySet) {
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

}
