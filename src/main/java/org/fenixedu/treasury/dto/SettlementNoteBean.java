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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.AdvancedPaymentCreditNote;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.ReimbursementUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SettlementNoteBean implements ITreasuryBean, Serializable {
    public static final String CONTROLLER_URL = "/treasury/document/managepayments/settlementnote";

    public static final String CHOOSE_INVOICE_ENTRIES_URI = "/chooseInvoiceEntries/";
    public static final String CHOOSE_INVOICE_ENTRIES_URL = CONTROLLER_URL + CHOOSE_INVOICE_ENTRIES_URI;
    public static final String CALCULATE_INTEREST_URI = "/calculateInterest/";
    public static final String CALCULATE_INTEREST_URL = CONTROLLER_URL + CALCULATE_INTEREST_URI;
    public static final String CREATE_DEBIT_NOTE_URI = "/createDebitNote/";
    public static final String CREATE_DEBIT_NOTE_URL = CONTROLLER_URL + CREATE_DEBIT_NOTE_URI;
    public static final String INSERT_PAYMENT_URI = "/insertpayment/";
    public static final String INSERT_PAYMENT_URL = CONTROLLER_URL + INSERT_PAYMENT_URI;
    public static final String SUMMARY_URI = "/summary/";
    public static final String SUMMARY_URL = CONTROLLER_URL + SUMMARY_URI;

    private static final long serialVersionUID = 1L;

    private boolean reimbursementNote;

    private DebtAccount debtAccount;

    private LocalDate date;

    private DocumentNumberSeries docNumSeries;

    private String originDocumentNumber;

    private List<CreditEntryBean> creditEntries;

    private List<ISettlementInvoiceEntryBean> debitEntries;

    private List<InterestEntryBean> interestEntries;

    private List<PaymentEntryBean> paymentEntries;

    private List<TreasuryTupleDataSourceBean> paymentMethods;

    private List<TreasuryTupleDataSourceBean> documentNumberSeries;

    private List<String> settlementNoteStateUrls;

    private Stack<Integer> previousStates;

    private String finantialTransactionReferenceYear;

    private String finantialTransactionReference;

    private boolean advancePayment;

    private DigitalPaymentPlatform digitalPaymentPlatform;

    private SibsBillingAddressBean addressBean;

    public SettlementNoteBean() {
        init();
    }

    public SettlementNoteBean(DebtAccount debtAccount, boolean isReimbursementNote, boolean excludeDebtsForPayorAccount) {
        this();
        init(debtAccount, isReimbursementNote, excludeDebtsForPayorAccount);
    }

    private void init() {
        this.creditEntries = new ArrayList<CreditEntryBean>();
        this.debitEntries = new ArrayList<ISettlementInvoiceEntryBean>();
        this.interestEntries = new ArrayList<InterestEntryBean>();
        this.paymentEntries = new ArrayList<PaymentEntryBean>();
        this.date = new LocalDate();
        this.previousStates = new Stack<Integer>();
        setPaymentMethods(PaymentMethod.findAvailableForPaymentInApplication().collect(Collectors.toList()));

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());
    }

    public void init(DebtAccount debtAccount, boolean reimbursementNote, boolean excludeDebtsForPayorAccount) {
        init();

        this.debtAccount = debtAccount;
        this.reimbursementNote = reimbursementNote;

        for (InvoiceEntry invoiceEntry : debtAccount.getPendingInvoiceEntriesSet().stream()
                .filter(ie -> ie.hasPreparingSettlementEntries() == false)
                .sorted((x, y) -> x.getDueDate().compareTo(y.getDueDate())).collect(Collectors.<InvoiceEntry> toList())) {
            if (invoiceEntry instanceof DebitEntry) {
                final DebitEntry debitEntry = (DebitEntry) invoiceEntry;

                if (excludeDebtsForPayorAccount && debitEntry.getFinantialDocument() != null
                        && ((Invoice) debitEntry.getFinantialDocument()).isForPayorDebtAccount()) {
                    continue;
                }

                debitEntries.add(new DebitEntryBean(debitEntry));
            } else {
                final CreditEntry creditEntry = (CreditEntry) invoiceEntry;

                if (excludeDebtsForPayorAccount && creditEntry.getFinantialDocument() != null
                        && ((Invoice) creditEntry.getFinantialDocument()).isForPayorDebtAccount()) {
                    continue;
                }

                creditEntries.add(new CreditEntryBean((CreditEntry) invoiceEntry));
            }
        }

        Set<Installment> installments = debtAccount.getActivePaymentPlansSet().stream()
                .flatMap(plan -> plan.getSortedOpenInstallments().stream()).collect(Collectors.toSet());

        for (Installment installment : installments) {
            debitEntries.add(new InstallmentPaymenPlanBean(installment));
        }

        setDocumentNumberSeries(debtAccount, reimbursementNote);

        settlementNoteStateUrls = Arrays.asList(
                CHOOSE_INVOICE_ENTRIES_URL + debtAccount.getExternalId() + "/" + reimbursementNote, CHOOSE_INVOICE_ENTRIES_URL,
                CALCULATE_INTEREST_URL, CREATE_DEBIT_NOTE_URL, INSERT_PAYMENT_URL, SUMMARY_URL);

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());

        Collections.sort(this.debitEntries, (o1, o2) -> o1.getDueDate().compareTo(o2.getDueDate()));
    }

    public SettlementNoteBean(DebtAccount debtAccount, DigitalPaymentPlatform digitalPaymentPlatform,
            final boolean reimbursementNote, final boolean excludeDebtsForPayorAccount) {
        init(debtAccount, reimbursementNote, excludeDebtsForPayorAccount);

        setDigitalPaymentPlatform(digitalPaymentPlatform);
    }

    public Set<Customer> getReferencedCustomers() {
        final Set<Customer> result = Sets.newHashSet();

        for (final ISettlementInvoiceEntryBean iSettlementInvoiceEntryBean : getDebitEntries()) {
            if (!iSettlementInvoiceEntryBean.isIncluded()) {
                continue;
            }
            result.addAll(iSettlementInvoiceEntryBean.getPaymentCustomer());

        }

        for (final CreditEntryBean creditEntryBean : creditEntries) {
            if (!creditEntryBean.isIncluded()) {
                continue;
            }
            result.addAll(creditEntryBean.getPaymentCustomer());
        }

        return result;
    }

    public boolean isReimbursementWithCompensation() {
        if (!isReimbursementNote()) {
            return false;
        }

        if (getCreditEntries().stream().filter(ce -> ce.isIncluded()).count() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.reimbursement.supports.only.one.settlement.entry");
        }

        final CreditEntry creditEntry =
                getCreditEntries().stream().filter(ce -> ce.isIncluded()).findFirst().get().getCreditEntry();
        return ReimbursementUtils.isCreditNoteForReimbursementMustBeClosedWithDebitNoteAndCreatedNew(creditEntry);
    }

    public boolean checkAdvancePaymentCreditsWithPaymentDate() {
        final Optional<SettlementNote> lastAdvancedCreditSettlementNote = getLastPaidAdvancedCreditSettlementNote();

        if (!lastAdvancedCreditSettlementNote.isPresent()) {
            return true;
        }

        return !getDate().isBefore(lastAdvancedCreditSettlementNote.get().getPaymentDate().toLocalDate());
    }

    public Optional<SettlementNote> getLastPaidAdvancedCreditSettlementNote() {
        final Optional<SettlementNote> lastAdvancedCreditSettlementNote =
                getCreditEntries().stream().filter(ce -> ce.isIncluded())
                        .filter(ce -> ce.getCreditEntry().getFinantialDocument() != null
                                && ((CreditNote) ce.getCreditEntry().getFinantialDocument()).isAdvancePayment())
                        .map(ce -> ce.getCreditEntry().getFinantialDocument()).map(AdvancedPaymentCreditNote.class::cast)
                        .filter(c -> c.getAdvancedPaymentSettlementNote() != null).map(c -> c.getAdvancedPaymentSettlementNote())
                        .sorted(Comparator.comparing(SettlementNote::getPaymentDate).reversed()).findFirst();

        return lastAdvancedCreditSettlementNote;
    }

    public void calculateInterestDebitEntries() {
        setInterestEntries(new ArrayList<InterestEntryBean>());
        for (DebitEntryBean debitEntryBean : getDebitEntriesByType(DebitEntryBean.class)) {
            if (debitEntryBean.isIncluded() && TreasuryConstants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(),
                    debitEntryBean.getDebtAmount())) {

                //Calculate interest only if we are making a FullPayment
                InterestRateBean debitInterest = debitEntryBean.getDebitEntry().calculateUndebitedInterestValue(getDate());
                if (TreasuryConstants.isPositive(debitInterest.getInterestAmount())) {
                    InterestEntryBean interestEntryBean = new InterestEntryBean(debitEntryBean.getDebitEntry(), debitInterest);
                    getInterestEntries().add(interestEntryBean);
                }
            }
        }
    }

    public BigDecimal getTotalAmountToPay() {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (final ISettlementInvoiceEntryBean debitEntryBean : getDebitEntries()) {
            if (!debitEntryBean.isIncluded()) {
                continue;
            }

            if (isReimbursementNote()) {
                totalAmount = totalAmount.subtract(debitEntryBean.getSettledAmount());
            } else {
                totalAmount = totalAmount.add(debitEntryBean.getSettledAmount());
            }
        }

        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (!interestEntryBean.isIncluded()) {
                continue;
            }

            if (isReimbursementNote()) {
                totalAmount = totalAmount.subtract(interestEntryBean.getInterest().getInterestAmount());
            } else {
                totalAmount = totalAmount.add(interestEntryBean.getInterest().getInterestAmount());
            }
        }

        for (CreditEntryBean creditEntryBean : getCreditEntries()) {
            if (!creditEntryBean.isIncluded()) {
                continue;
            }

            if (isReimbursementNote()) {
                totalAmount = totalAmount.add(creditEntryBean.getCreditAmountWithVat());
            } else {
                totalAmount = totalAmount.subtract(creditEntryBean.getCreditAmountWithVat());
            }
        }

        return totalAmount;
    }

    public List<ISettlementInvoiceEntryBean> getIncludedInvoiceEntryBeans() {
        final List<ISettlementInvoiceEntryBean> invoiceEntriesList = Lists.newArrayList();
        for (ISettlementInvoiceEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()) {
                invoiceEntriesList.add(debitEntryBean);
            }
        }

        for (final CreditEntryBean creditEntryBean : getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                invoiceEntriesList.add(creditEntryBean);
            }
        }

        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded()) {
                invoiceEntriesList.add(interestEntryBean);
            }
        }
        return invoiceEntriesList;
    }

    public boolean isIncludedLegacyERPInvoiceEntryBeans() {
        for (ISettlementInvoiceEntryBean entryBean : getIncludedInvoiceEntryBeans()) {
            if (entryBean.getInvoiceEntry() == null) {
                continue;
            }

            if (entryBean.getInvoiceEntry().getFinantialDocument() == null) {
                continue;
            }

            if (entryBean.getInvoiceEntry().getFinantialDocument().isExportedInLegacyERP()) {
                return true;
            }
        }

        return false;
    }

    public void includeAllInterestOfSelectedDebitEntries() {
        setInterestEntries(new ArrayList<InterestEntryBean>());
        List<DebitEntryBean> debitEntriesToIterate = Lists.newArrayList(getDebitEntriesByType(DebitEntryBean.class));
        for (DebitEntryBean debitEntryBean : debitEntriesToIterate) {
            if (debitEntryBean.isIncluded() && TreasuryConstants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(),
                    debitEntryBean.getDebtAmount())) {

                //Calculate interest only if we are making a FullPayment
                InterestRateBean debitInterest = debitEntryBean.getDebitEntry().calculateUndebitedInterestValue(getDate());
                if (debitInterest.getInterestAmount().compareTo(BigDecimal.ZERO) != 0) {
                    InterestEntryBean interestEntryBean = new InterestEntryBean(debitEntryBean.getDebitEntry(), debitInterest);
                    getInterestEntries().add(interestEntryBean);
                    interestEntryBean.setIncluded(true);
                }
            }
        }

        for (DebitEntryBean debitEntryBean : debitEntriesToIterate) {
            if (debitEntryBean.isIncluded() && TreasuryConstants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(),
                    debitEntryBean.getDebtAmount())) {
                for (final DebitEntry interestDebitEntry : debitEntryBean.getDebitEntry().getInterestDebitEntriesSet()) {
                    if (getDebitEntriesByType(DebitEntryBean.class).stream().filter(e -> e.isIncluded)
                            .filter(e -> e.getDebitEntry() == interestDebitEntry).findAny().isPresent()) {
                        continue;
                    }

                    if (interestDebitEntry.isInDebt()) {
                        getDebitEntries().stream().filter(e -> ((DebitEntryBean) e).getDebitEntry() == interestDebitEntry)
                                .findAny().get().setIncluded(true);
                    }
                }
            }
        }

    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public List<Integer> getFinantialTransactionReferenceYears() {
        final List<Integer> years =
                GlobalInterestRate.findAll().map(g -> (Integer) g.getYear()).sorted().collect(Collectors.toList());
        Collections.reverse(years);

        return years;
    }

    private void setDocumentNumberSeries(DebtAccount debtAccount, boolean reimbursementNote) {
        FinantialDocumentType finantialDocumentType = (reimbursementNote) ? FinantialDocumentType
                .findForReimbursementNote() : FinantialDocumentType.findForSettlementNote();

        List<DocumentNumberSeries> availableSeries = DocumentNumberSeries
                .find(finantialDocumentType, debtAccount.getFinantialInstitution()).collect(Collectors.toList());

        this.setDocumentNumberSeries(DocumentNumberSeries.applyActiveSelectableAndDefaultSorting(availableSeries.stream())
                .collect(Collectors.toList()));
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public void setDebtAccount(DebtAccount debtAccount) {
        this.debtAccount = debtAccount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<CreditEntryBean> getCreditEntries() {
        return creditEntries;
    }

    public void setCreditEntries(List<CreditEntryBean> creditEntries) {
        this.creditEntries = creditEntries;
    }

    public <T> List<T> getDebitEntriesByType(Class<T> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        return debitEntries.stream().filter(entry -> clazz.isAssignableFrom(entry.getClass())).map(obj -> clazz.cast(obj))
                .collect(Collectors.toList());
    }

    public List<ISettlementInvoiceEntryBean> getDebitEntries() {
        return debitEntries;
    }

    public void setDebitEntries(List<ISettlementInvoiceEntryBean> debitEntries) {
        this.debitEntries = debitEntries;
    }

    public List<InterestEntryBean> getInterestEntries() {
        return interestEntries;
    }

    public void setInterestEntries(List<InterestEntryBean> interestEntries) {
        this.interestEntries = interestEntries;
    }

    public boolean isAdvancePayment() {
        return this.advancePayment;
    }

    public void setAdvancePayment(final boolean advancePayment) {
        this.advancePayment = advancePayment;
    }

    public LocalDate getDebitNoteDate() {
        LocalDate lowerDate = new LocalDate();
        for (DebitEntryBean debitEntryBean : getDebitEntriesByType(DebitEntryBean.class)) {
            if (debitEntryBean.isIncluded() && debitEntryBean.getDueDate().isBefore(lowerDate)) {
                lowerDate = debitEntryBean.getDueDate();
            }
        }
        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded() && interestEntryBean.getDueDate().isBefore(lowerDate)) {
                lowerDate = interestEntryBean.getDueDate();
            }
        }
        return lowerDate;
    }

    public BigDecimal getDebtAmount() {
        BigDecimal sum = BigDecimal.ZERO;
        for (ISettlementInvoiceEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()) {
                sum = sum.add(debitEntryBean.getSettledAmount());
            }
        }
        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded()) {
                sum = sum.add(interestEntryBean.getInterest().getInterestAmount());
            }
        }
        for (CreditEntryBean creditEntryBean : getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                sum = sum.subtract(creditEntryBean.getCreditAmount());
            }
        }
        return sum;
    }

    public BigDecimal getDebtAmountWithVat() {
        BigDecimal sum = BigDecimal.ZERO;
        for (ISettlementInvoiceEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()) {
                sum = sum.add(debitEntryBean.getSettledAmount());
            }
        }
        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded()) {
                //Interest doesn't have vat
                sum = sum.add(interestEntryBean.getInterest().getInterestAmount());
            }
        }
        for (CreditEntryBean creditEntryBean : getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                sum = sum.subtract(creditEntryBean.getCreditAmountWithVat());
            }
        }
        return sum;
    }

    public BigDecimal getVatAmount() {
        return getDebtAmountWithVat().subtract(getDebtAmount());
    }

    public Map<String, VatAmountBean> getValuesByVat() {
        Map<String, VatAmountBean> sumByVat = new HashMap<String, VatAmountBean>();
        for (VatType vatType : VatType.findAll().collect(Collectors.toList())) {
            sumByVat.put(vatType.getName().getContent(), new VatAmountBean(BigDecimal.ZERO, BigDecimal.ZERO));
        }
        for (DebitEntryBean debitEntryBean : getDebitEntriesByType(DebitEntryBean.class)) {
            if (debitEntryBean.isIncluded()) {
                String vatType = debitEntryBean.getDebitEntry().getVat().getVatType().getName().getContent();
                sumByVat.get(vatType).addAmount(debitEntryBean.getDebtAmount());
                sumByVat.get(vatType).addAmountWithVat(debitEntryBean.getDebtAmountWithVat());
            }
        }
        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded()) {
                String vatType = interestEntryBean.getDebitEntry().getVat().getVatType().getName().getContent();
                sumByVat.get(vatType).addAmount(interestEntryBean.getInterest().getInterestAmount());
                sumByVat.get(vatType).addAmountWithVat(interestEntryBean.getInterest().getInterestAmount());
            }
        }
        for (CreditEntryBean creditEntryBean : getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                String vatType = creditEntryBean.getCreditEntry().getVat().getVatType().getName().getContent();
                sumByVat.get(vatType).subtractAmount(creditEntryBean.getCreditAmount());
                sumByVat.get(vatType).subtractAmountWithVat(creditEntryBean.getCreditAmountWithVat());
            }
        }
        return sumByVat;
    }

    public List<PaymentEntryBean> getPaymentEntries() {
        return paymentEntries;
    }

    public void setPaymentEntries(List<PaymentEntryBean> paymentEntries) {
        this.paymentEntries = paymentEntries;
    }

    public List<TreasuryTupleDataSourceBean> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods.stream().map(paymentMethod -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setText(paymentMethod.getName().getContent());
            tuple.setId(paymentMethod.getExternalId());
            return tuple;
        }).collect(Collectors.toList());
    }

    public BigDecimal getPaymentAmount() {
        BigDecimal paymentAmount = BigDecimal.ZERO;
        for (PaymentEntryBean paymentEntryBean : getPaymentEntries()) {
            paymentAmount = paymentAmount.add(paymentEntryBean.getPaymentAmount());
        }
        return paymentAmount;
    }

    public DocumentNumberSeries getDocNumSeries() {
        return docNumSeries;
    }

    public void setDocNumSeries(DocumentNumberSeries docNumSeries) {
        this.docNumSeries = docNumSeries;
    }

    public List<TreasuryTupleDataSourceBean> getDocumentNumberSeries() {
        return documentNumberSeries;
    }

    public void setDocumentNumberSeries(List<DocumentNumberSeries> documentNumberSeries) {
        this.documentNumberSeries = documentNumberSeries.stream().map(docNumSeries -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setText(docNumSeries.getSeries().getCode() + " - " + docNumSeries.getSeries().getName().getContent());
            tuple.setId(docNumSeries.getExternalId());
            return tuple;
        }).collect(Collectors.toList());
    }

    public String getOriginDocumentNumber() {
        return originDocumentNumber;
    }

    public void setOriginDocumentNumber(String originDocumentNumber) {
        this.originDocumentNumber = originDocumentNumber;
    }

    public boolean isReimbursementNote() {
        return reimbursementNote;
    }

    public void setReimbursementNote(boolean reimbursementNote) {
        this.reimbursementNote = reimbursementNote;
    }

    public Stack<Integer> getPreviousStates() {
        return previousStates;
    }

    public void setPreviousStates(Stack<Integer> previousStates) {
        this.previousStates = previousStates;
    }

    public List<String> getSettlementNoteStateUrls() {
        return settlementNoteStateUrls;
    }

    public void setSettlementNoteStateUrls(List<String> settlementNoteStateUrls) {
        this.settlementNoteStateUrls = settlementNoteStateUrls;
    }

    public boolean hasEntriesWithoutDocument() {
        return getDebitEntriesByType(DebitEntryBean.class).stream()
                .anyMatch(deb -> deb.isIncluded() && deb.getDebitEntry().getFinantialDocument() == null)
                || interestEntries.stream().anyMatch(deb -> deb.isIncluded());
    }

    public String getFinantialTransactionReference() {
        return finantialTransactionReference;
    }

    public void setFinantialTransactionReference(String finantialTransactionReference) {
        this.finantialTransactionReference = finantialTransactionReference;
    }

    public String getFinantialTransactionReferenceYear() {
        return finantialTransactionReferenceYear;
    }

    public void setFinantialTransactionReferenceYear(String finantialTransactionReferenceYear) {
        this.finantialTransactionReferenceYear = finantialTransactionReferenceYear;
    }

    public DigitalPaymentPlatform getDigitalPaymentPlatform() {
        return digitalPaymentPlatform;
    }

    public void setDigitalPaymentPlatform(DigitalPaymentPlatform digitalPaymentPlatform) {
        this.digitalPaymentPlatform = digitalPaymentPlatform;
    }

    // @formatter:off
    /* ************
     * HELPER BEANS
     * ************
     */
    // @formatter:on

    public SibsBillingAddressBean getAddressBean() {
        return addressBean;
    }

    public void setAddressBean(SibsBillingAddressBean addressBean) {
        this.addressBean = addressBean;
    }

    public static class DebitEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private DebitEntry debitEntry;

        private boolean isIncluded;

        private boolean isNotValid;

        private BigDecimal debtAmount;

        public DebitEntryBean() {

        }

        public DebitEntryBean(DebitEntry debitEntry) {
            this.debitEntry = debitEntry;
            this.isIncluded = false;
            this.isNotValid = false;
            this.debtAmount = debitEntry.getOpenAmount();
        }

        public DebitEntry getDebitEntry() {
            return debitEntry;
        }

        public void setDebitEntry(DebitEntry debitEntry) {
            this.debitEntry = debitEntry;
        }

        public String getDocumentNumber() {
            return debitEntry.getFinantialDocument() != null ? debitEntry.getFinantialDocument().getDocumentNumber() : null;
        }

        @Deprecated
        public LocalDate getDocumentDueDate() {
            return debitEntry.getDueDate();
        }

        @Deprecated
        public BigDecimal getDebtAmount() {
            if (debtAmount == null) {
                return null;
            }

            return debitEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(debtAmount);
        }

        @Deprecated
        public BigDecimal getDebtAmountWithVat() {
            if (debtAmount == null) {
                return null;
            }

            return debitEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(debtAmount);
        }

        @Deprecated
        public void setDebtAmount(BigDecimal debtAmount) {
            this.debtAmount = debtAmount;
        }

        @Override
        public InvoiceEntry getInvoiceEntry() {
            return debitEntry;
        }

        @Override
        public String getDescription() {
            return debitEntry.getDescription();
        }

        @Override
        public LocalDate getDueDate() {
            return debitEntry.getDueDate();
        }

        @Override
        public BigDecimal getEntryAmount() {
            return debitEntry.getAmount();
        }

        @Override
        public BigDecimal getEntryOpenAmount() {
            return debitEntry.getOpenAmount();
        }

        @Override
        public BigDecimal getSettledAmount() {
            return debtAmount;
        }

        @Override
        public void setSettledAmount(BigDecimal debtAmount) {
            this.debtAmount = debtAmount;
        }

        @Override
        public Vat getVat() {
            return debitEntry.getVat();
        }

        @Override
        public BigDecimal getVatRate() {
            return debitEntry.getVatRate();
        }

        @Override
        public boolean isIncluded() {
            return isIncluded;
        }

        @Override
        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

        @Override
        public boolean isNotValid() {
            return isNotValid;
        }

        @Override
        public void setNotValid(boolean notValid) {
            this.isNotValid = notValid;
        }

        @Override
        public FinantialDocument getFinantialDocument() {
            return debitEntry.getFinantialDocument();
        }

        @Override
        public Set<Customer> getPaymentCustomer() {
            return debitEntry.getFinantialDocument() != null
                    && ((Invoice) debitEntry.getFinantialDocument()).getPayorDebtAccount() != null ? Collections.singleton(
                            ((Invoice) debitEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer()) : Collections
                                    .singleton(debitEntry.getDebtAccount().getCustomer());
        }

        /*
         * Methods to support jsp, overriden in subclasses
         */

        @Override
        public boolean isForDebitEntry() {
            return true;
        }

        @Override
        public boolean isForInstallment() {
            return false;
        }

        @Override
        public boolean isForCreditEntry() {
            return false;
        }

        @Override
        public boolean isForPendingInterest() {
            return false;
        }

        @Override
        public boolean isForPaymentPenalty() {
            return false;
        }

        @Override
        public boolean isForPendingDebitEntry() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    public static class CreditEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private CreditEntry creditEntry;

        private boolean isIncluded;

        private boolean isNotValid;

        private BigDecimal creditAmount;

        public CreditEntryBean() {
        }

        public CreditEntryBean(CreditEntry creditEntry) {
            this.creditEntry = creditEntry;
            this.isIncluded = false;
            this.isNotValid = false;
            this.creditAmount = creditEntry.getOpenAmount();
        }

        public CreditEntry getCreditEntry() {
            return creditEntry;
        }

        public void setCreditEntry(CreditEntry creditEntry) {
            this.creditEntry = creditEntry;
        }

        public String getDocumentNumber() {
            return creditEntry.getFinantialDocument() != null ? creditEntry.getFinantialDocument().getDocumentNumber() : null;
        }

        @Deprecated
        public LocalDate getDocumentDueDate() {
            return creditEntry.getFinantialDocument() != null ? creditEntry.getFinantialDocument()
                    .getDocumentDueDate() : creditEntry.getEntryDateTime().toLocalDate();
        }

        @Deprecated
        public BigDecimal getCreditAmount() {
            if (creditAmount == null) {
                return null;
            }

            return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
        }

        @Deprecated
        public BigDecimal getCreditAmountWithVat() {
            if (creditAmount == null) {
                return null;
            }

            return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
        }

        @Deprecated
        public void setCreditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }

        @Override
        public InvoiceEntry getInvoiceEntry() {
            return creditEntry;
        }

        @Override
        public String getDescription() {
            return creditEntry.getDescription();
        }

        @Override
        public LocalDate getDueDate() {
            return creditEntry.getDueDate();
        }

        @Override
        public BigDecimal getEntryAmount() {
            return creditEntry.getAmount();
        }

        @Override
        public BigDecimal getEntryOpenAmount() {
            return creditEntry.getOpenAmount();
        }

        @Override
        public BigDecimal getSettledAmount() {
            return creditAmount;
        }

        @Override
        public void setSettledAmount(BigDecimal debtAmount) {
            this.creditAmount = debtAmount;
        }

        @Override
        public Vat getVat() {
            return creditEntry.getVat();
        }

        @Override
        public BigDecimal getVatRate() {
            return creditEntry.getVatRate();
        }

        @Override
        public boolean isIncluded() {
            return isIncluded;
        }

        @Override
        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

        @Override
        public boolean isNotValid() {
            return isNotValid;
        }

        @Override
        public void setNotValid(boolean notValid) {
            this.isNotValid = notValid;
        }

        @Override
        public FinantialDocument getFinantialDocument() {
            return creditEntry.getFinantialDocument();
        }

        @Override
        public Set<Customer> getPaymentCustomer() {
            return creditEntry.getFinantialDocument() != null
                    && ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount() != null ? Collections.singleton(
                            ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer()) : Collections
                                    .singleton(creditEntry.getDebtAccount().getCustomer());
        }

        /*
         * Methods to support jsp, overriden in subclasses
         */

        @Override
        public boolean isForDebitEntry() {
            return false;
        }

        @Override
        public boolean isForInstallment() {
            return false;
        }

        @Override
        public boolean isForCreditEntry() {
            return true;
        }

        @Override
        public boolean isForPendingInterest() {
            return false;
        }

        @Override
        public boolean isForPaymentPenalty() {
            return false;
        }

        @Override
        public boolean isForPendingDebitEntry() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    public static class InterestEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private DebitEntry debitEntry;

        private boolean isIncluded;

        private InterestRateBean interest;

        public InterestEntryBean() {
            this.isIncluded = false;
        }

        public InterestEntryBean(DebitEntry debitEntry, InterestRateBean interest) {
            this();
            this.debitEntry = debitEntry;
            this.interest = interest;
        }

        public InterestRateBean getInterest() {
            return interest;
        }

        public void setInterest(InterestRateBean interest) {
            this.interest = interest;
        }

        public DebitEntry getDebitEntry() {
            return debitEntry;
        }

        public void setDebitEntry(DebitEntry debitEntry) {
            this.debitEntry = debitEntry;
        }

        public LocalDate getDocumentDueDate() {
            return debitEntry.getFinantialDocument() != null ? debitEntry.getFinantialDocument().getDocumentDueDate() : debitEntry
                    .getDueDate();
        }

        @Override
        public boolean isIncluded() {
            return isIncluded;
        }

        @Override
        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

        @Override
        public InvoiceEntry getInvoiceEntry() {
            return null;
        }

        @Override
        public String getDescription() {
            return getInterest().getDescription();
        }

        @Override
        public boolean isNotValid() {
            return false;
        }

        @Override
        public void setNotValid(boolean notValid) {
        }

        @Override
        public BigDecimal getVatRate() {
            return debitEntry.getVatRate();
        }

        @Override
        public FinantialDocument getFinantialDocument() {
            return null;
        }

        @Override
        public Vat getVat() {
            final VatType vatType = TreasurySettings.getInstance().getInterestProduct().getVatType();
            return Vat.findActiveUnique(vatType, debitEntry.getDebtAccount().getFinantialInstitution(), DateTime.now()).get();

        }

        @Override
        public Set<Customer> getPaymentCustomer() {
            return Collections.emptySet();
        }

        @Override
        public LocalDate getDueDate() {
            return debitEntry.getFinantialDocument() != null ? debitEntry.getFinantialDocument().getDocumentDueDate() : debitEntry
                    .getDueDate();
        }

        @Override
        public BigDecimal getEntryAmount() {
            return null;
        }

        @Override
        public BigDecimal getEntryOpenAmount() {
            return null;
        }

        @Override
        public BigDecimal getSettledAmount() {
            return getInterest().getInterestAmount();
        }

        @Override
        public void setSettledAmount(BigDecimal debtAmount) {

        }

        /*
         * Methods to support jsp, overriden in subclasses
         */

        @Override
        public boolean isForDebitEntry() {
            return false;
        }

        @Override
        public boolean isForInstallment() {
            return false;
        }

        @Override
        public boolean isForCreditEntry() {
            return false;
        }

        @Override
        public boolean isForPendingInterest() {
            return true;
        }

        @Override
        public boolean isForPaymentPenalty() {
            return false;
        }

        @Override
        public boolean isForPendingDebitEntry() {
            return false;
        }
    }

    public static class PaymentEntryBean implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal paymentAmount;

        private PaymentMethod paymentMethod;

        private String paymentMethodId;

        public PaymentEntryBean() {
            this.paymentAmount = BigDecimal.ZERO;
        }

        public PaymentEntryBean(BigDecimal paymentAmount, PaymentMethod paymentMethod, String paymentMethodId) {
            this.paymentAmount = paymentAmount;
            this.paymentMethod = paymentMethod;
            this.paymentMethodId = paymentMethodId;
        }

        public BigDecimal getPaymentAmount() {
            return paymentAmount;
        }

        public void setPaymentAmount(BigDecimal paymentAmount) {
            this.paymentAmount = paymentAmount;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getPaymentMethodId() {
            return paymentMethodId;
        }

        public void setPaymentMethodId(String paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }

    }

    public class VatAmountBean implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal amount;

        private BigDecimal amountWithVat;

        public VatAmountBean(BigDecimal amount, BigDecimal amountWithVat) {
            this.amount = amount;
            this.amountWithVat = amountWithVat;
        }

        public VatAmountBean() {
            this.amount = BigDecimal.ZERO;
            this.amountWithVat = BigDecimal.ZERO;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public void addAmount(BigDecimal partialAmount) {
            this.amount = amount.add(partialAmount);
        }

        public void subtractAmount(BigDecimal partialAmount) {
            this.amount = amount.subtract(partialAmount);
        }

        public BigDecimal getAmountWithVat() {
            return amountWithVat;
        }

        public void setAmountWithVat(BigDecimal amountWithVat) {
            this.amountWithVat = amountWithVat;
        }

        public void addAmountWithVat(BigDecimal partialAmountWithVat) {
            this.amountWithVat = amountWithVat.add(partialAmountWithVat);
        }

        public void subtractAmountWithVat(BigDecimal partialAmountWithVat) {
            this.amountWithVat = amountWithVat.subtract(partialAmountWithVat);
        }
    }
}
