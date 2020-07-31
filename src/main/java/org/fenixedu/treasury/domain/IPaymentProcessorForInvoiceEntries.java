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
    
    default public Set<SettlementNote> internalProcessPaymentInNormalPaymentMixingLegacyInvoices(String username,
            BigDecimal amount, DateTime paymentDate, String originDocumentNumber, String comments,
            Set<InvoiceEntry> invoiceEntriesToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction) {

        final TreeSet<InvoiceEntry> sortedInvoiceEntriesToPay = Sets.newTreeSet(InvoiceEntry.COMPARE_BY_AMOUNT_AND_DUE_DATE);
        sortedInvoiceEntriesToPay.addAll(invoiceEntriesToPay);

        //Process the payment of pending invoiceEntries
        //1. Find the InvoiceEntries
        //2. Create the SEttlementEntries and the SEttlementNote
        //2.1 create the "InterestRate entries"
        //2.2 if there is pending amount, pay the interest rate
        //3. If there is pending amount, try to pay a Pending DebitEntries
        //3.1 create the interestRate entries for pending debit entries
        //3.2 if there is pending amount, try to pay interesrtRate for pending debit entries
        //4. If there is money for more, create a "pending" payment (CreditNote) for being used later
        //5. Close the SettlementNote
        //6. Create a SibsTransactionDetail
        BigDecimal availableAmount = amount;

        List<DebitEntry> interestRateEntries = new ArrayList<DebitEntry>();
        DebtAccount referenceDebtAccount = this.getDebtAccount();
        DocumentNumberSeries docNumberSeries = this.getDocumentSeriesForPayments();
        SettlementNote settlementNote =
                SettlementNote.create(referenceDebtAccount, docNumberSeries, new DateTime(), paymentDate, comments, null);

        //######################################
        //1. Find the InvoiceEntries
        //2. Create the SEttlementEntries and the SEttlementNote
        //######################################

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

                        //check if the amount to pay in the Debit Entry 
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        if (debitEntry.getOpenAmount().equals(amountToPay)) {
                            //######################################
                            //2.1 create the "InterestRate entries"
                            //######################################
                            InterestRateBean calculateUndebitedInterestValue =
                                    debitEntry.calculateUndebitedInterestValue(paymentDate.toLocalDate());
                            if (TreasuryConstants.isPositive(calculateUndebitedInterestValue.getInterestAmount())) {
                                DebitEntry interestDebitEntry = debitEntry.createInterestRateDebitEntry(
                                        calculateUndebitedInterestValue, paymentDate, Optional.<DebitNote> empty());
                                interestRateEntries.add(interestDebitEntry);
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

            //######################################
            //2.2 if there is pending amount, pay the interest rate
            //######################################
            //if we created interestRateEntries then we must close them in a document and try to pay with availableAmount
            if (interestRateEntries.size() > 0) {
                //Create a DebitNote for the Interests DebitEntries
                DebitNote interestNote =
                        DebitNote.create(referenceDebtAccount, this.getDocumentSeriesInterestDebits(), paymentDate);
                for (DebitEntry interestEntry : interestRateEntries) {
                    interestEntry.setFinantialDocument(interestNote);
                }
                interestNote.closeDocument();

                //if "availableAmount" still exists, then we must check if there is any InterestRate to pay
                if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
                    for (DebitEntry interestEntry : interestRateEntries) {
                        //Check if there is enough amount to Pay
                        if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            break;
                        }

                        BigDecimal amountToPay = interestEntry.getOpenAmount();
                        //check if the amount to pay in the Debit Entry 
                        if (amountToPay.compareTo(availableAmount) > 0) {
                            amountToPay = availableAmount;
                        }

                        SettlementEntry newSettlementEntry = SettlementEntry.create(interestEntry, settlementNote, amountToPay,
                                interestEntry.getDescription(), paymentDate, true);
                        //Update the amount to Pay
                        availableAmount = availableAmount.subtract(amountToPay);
                    }
                }
                interestRateEntries.clear();
            }
        }

        //######################################
        //4. If there is money for more, create a "pending" payment (CreditNote) for being used later
        //######################################

        //if "availableAmount" still exists, then we must create a "pending Payment" or "CreditNote"
        if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
            settlementNote.createAdvancedPaymentCreditNote(availableAmount,
                    treasuryBundle("label.PaymentCodeTarget.advancedpayment") + comments + "-" + originDocumentNumber,
                    originDocumentNumber);
        }

        //######################################
        //5. Close the SettlementNote
        //######################################

        final Map<String, String> paymentEntryPropertiesMap = additionalPropertiesMapFunction.apply(this);

        PaymentEntry.create(getPaymentMethod(), settlementNote, amount, fillPaymentEntryMethodId(), paymentEntryPropertiesMap);
        settlementNote.closeDocument();

        return Sets.newHashSet(settlementNote);
    }

    default public Set<SettlementNote> internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            final Set<InvoiceEntry> invoiceEntriesToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction) {
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            throw new RuntimeException("invalid call");
        }

        // Check if invoiceEntriesToPay have mixed invoice entries of certified in legacy erp and not
        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(invoiceEntriesToPay);

        // If all invoice entries are not exported in legacy ERP then invoke the internalProcessPaymentInNormalPaymentMixingLegacyInvoices
        if (invoiceEntriesToPay.stream()
                .allMatch(e -> e.getFinantialDocument() == null || !e.getFinantialDocument().isExportedInLegacyERP())) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(username, amount, paymentDate, sibsTransactionId,
                    comments, invoiceEntriesToPay, additionalPropertiesMapFunction);
        }

        final TreeSet<InvoiceEntry> sortedInvoiceEntriesToPay = Sets.newTreeSet(InvoiceEntry.COMPARE_BY_AMOUNT_AND_DUE_DATE);
        sortedInvoiceEntriesToPay.addAll(invoiceEntriesToPay);

        //Process the payment of pending invoiceEntries
        //1. Find the InvoiceEntries
        //2. Create the SEttlementEntries and the SEttlementNote
        //2.1 create the "InterestRate entries"
        //3. Close the SettlementNote
        //4. If there is money for more, create a "pending" payment (CreditNote) in different settlement note for being used later
        //6. Create a SibsTransactionDetail
        BigDecimal availableAmount = amount;

        final DebtAccount referenceDebtAccount = this.getDebtAccount();
        final DocumentNumberSeries docNumberSeries = this.getDocumentSeriesForPayments();
        final SettlementNote settlementNote =
                SettlementNote.create(referenceDebtAccount, docNumberSeries, new DateTime(), paymentDate, comments, null);

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
                            InterestRateBean calculateUndebitedInterestValue =
                                    debitEntry.calculateUndebitedInterestValue(paymentDate.toLocalDate());
                            if (TreasuryConstants.isPositive(calculateUndebitedInterestValue.getInterestAmount())) {
                                debitEntry.createInterestRateDebitEntry(calculateUndebitedInterestValue, paymentDate,
                                        Optional.<DebitNote> empty());
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

    public static Set<Customer> getReferencedCustomers(final Set<InvoiceEntry> invoiceEntrySet) {
        final Set<Customer> result = Sets.newHashSet();
        for (final InvoiceEntry entry : invoiceEntrySet) {
            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).isForPayorDebtAccount()) {
                result.add(((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(entry.getDebtAccount().getCustomer());
        }

        return result;
    }

}
