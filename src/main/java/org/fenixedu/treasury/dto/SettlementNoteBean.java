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
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FiscalYear;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.AdvancedPaymentCreditNote;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.ReimbursementUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.domain.treasurydebtprocess.InvoiceEntryBlockingPaymentContext;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.virtualpaymententries.IVirtualPaymentEntryHandler;
import org.fenixedu.treasury.services.payments.virtualpaymententries.VirtualPaymentEntryFactory;
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

    private FinantialEntity finantialEntity;

    private DateTime date;

    private DocumentNumberSeries docNumSeries;

    private String originDocumentNumber;

    private List<SettlementCreditEntryBean> creditEntries;

    private List<ISettlementInvoiceEntryBean> debitEntries;

    private List<ISettlementInvoiceEntryBean> virtualDebitEntries;

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

    // Fields used by SibsPaymentRequest generation
    private boolean limitSibsPaymentRequestToCustomDueDate;
    private LocalDate customSibsPaymentRequestDueDate;

    // This date is used only in Angular settlement note creation (treasury-ui)
    private LocalDate uiAngularPaymentDate;

    // By default the invoice entries are checked for payment blocking for the frontend 
    // payment or payment request generation
    //
    // But it is possible to check for payment blockin in the backoffice, which might be
    // more relaxed than frontend payment
    private InvoiceEntryBlockingPaymentContext invoiceEntriesPaymentBlockingContext = InvoiceEntryBlockingPaymentContext.FRONTEND;

    public SettlementNoteBean() {
        init();
    }

    public SettlementNoteBean(DebtAccount debtAccount, boolean isReimbursementNote, boolean excludeDebtsForPayorAccount) {
        this();
        init(debtAccount, isReimbursementNote, excludeDebtsForPayorAccount);
    }

    private SettlementNoteBean(PaymentRequest paymentRequest, DateTime paymentDate, BigDecimal paidAmount,
            String originDocumentNumber) {
        if (!TreasuryConstants.isPositive(paidAmount)) {
            throw new IllegalArgumentException("Paid amount is not positive");
        }

        init();
        setDate(paymentDate);
        setOriginDocumentNumber(originDocumentNumber);
        setAdvancePayment(true);

        this.debtAccount = paymentRequest.getDebtAccount();
        this.finantialEntity = paymentRequest.getFinantialEntity();
        this.reimbursementNote = false;

        // ANIL 2024-08-01
        //
        // In automatic payments, isReimbursementNote is always false
        // It is safe to only consider the finantial document type for settlement note

        this.docNumSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), this.finantialEntity);

        Comparator<InvoiceEntry> COMPARE_BY_DUE_DATE_AND_AMOUNT = (o1, o2) -> {
            int c = o1.getDueDate().compareTo(o2.getDueDate());

            if (c != 0) {
                return c;
            }

            c = -o1.getOpenAmount().compareTo(o2.getOpenAmount());

            if (c != 0) {
                return c;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        };

        if (!paymentRequest.getDebitEntriesSet().stream().filter(d -> d.isInDebt()).anyMatch(
                d -> TreasuryDebtProcessMainService.isBlockingPayment(d, InvoiceEntryBlockingPaymentContext.FRONTEND))) {
            final TreeSet<DebitEntry> sortedDebitEntriesToPay = Sets.newTreeSet(COMPARE_BY_DUE_DATE_AND_AMOUNT);
            sortedDebitEntriesToPay.addAll(paymentRequest.getDebitEntriesSet().stream().collect(Collectors.toSet()));

            for (DebitEntry debitEntry : sortedDebitEntriesToPay) {
                SettlementDebitEntryBean debitEntryBean = new SettlementDebitEntryBean(debitEntry);
                debitEntryBean.setIncluded(TreasuryConstants.isPositive(debitEntry.getOpenAmount()));
                this.debitEntries.add(debitEntryBean);
            }
        }

        if (!paymentRequest.getInstallmentsSet().stream().anyMatch(
                i -> TreasuryDebtProcessMainService.isBlockingPayment(i, InvoiceEntryBlockingPaymentContext.FRONTEND))) {
            final TreeSet<Installment> sortedInstallments = Sets.newTreeSet(Installment.COMPARE_BY_DUEDATE);

            /*
             * Include only open installment, because the installment settlement entries are created
             * for installment entries of open debit entries.
             */
            sortedInstallments.addAll(paymentRequest.getInstallmentsSet().stream()
                    .filter(i -> i.getPaymentPlan().getState().isOpen()).collect(Collectors.toSet()));

            for (Installment installment : sortedInstallments) {
                InstallmentPaymenPlanBean installmentBean = new InstallmentPaymenPlanBean(installment);
                installmentBean.setIncluded(TreasuryConstants.isPositive(installment.getOpenAmount()));
                this.debitEntries.add(installmentBean);
            }
        }

        if (getReferencedCustomers().size() > 1) {
            // Register advance payment only
            this.debitEntries.clear();
        }

        BigDecimal amountToDistribute = paidAmount;

        for (ISettlementInvoiceEntryBean entryBean : this.debitEntries) {
            if (!entryBean.isIncluded()) {
                continue;
            }

            if (TreasuryConstants.isPositive(amountToDistribute)
                    && TreasuryConstants.isGreaterOrEqualThan(amountToDistribute, entryBean.getSettledAmount())) {
                amountToDistribute = amountToDistribute.subtract(entryBean.getSettledAmount());
            } else if (TreasuryConstants.isPositive(amountToDistribute)
                    && TreasuryConstants.isGreaterThan(entryBean.getSettledAmount(), amountToDistribute)) {
                entryBean.setSettledAmount(amountToDistribute);
                amountToDistribute = BigDecimal.ZERO;
            } else {
                entryBean.setSettledAmount(BigDecimal.ZERO);
                entryBean.setIncluded(false);
            }
        }

        calculateVirtualDebitEntries();

        PaymentEntryBean paymentEntryBean =
                new PaymentEntryBean(paidAmount, paymentRequest.getPaymentMethod(), paymentRequest.fillPaymentEntryMethodId());

        this.paymentEntries.add(paymentEntryBean);

    }

    private SettlementNoteBean(SettlementNoteBean settlementNoteBean) {
        init();
        this.debtAccount = settlementNoteBean.getDebtAccount();
        this.finantialEntity = settlementNoteBean.getFinantialEntity();
        this.reimbursementNote = settlementNoteBean.isReimbursementNote();
        this.advancePayment = settlementNoteBean.isAdvancePayment();
        this.date = settlementNoteBean.getDate();
        this.digitalPaymentPlatform = settlementNoteBean.getDigitalPaymentPlatform();
        this.docNumSeries = settlementNoteBean.getDocNumSeries();
        this.finantialTransactionReference = settlementNoteBean.getFinantialTransactionReference();
        this.finantialTransactionReferenceYear = settlementNoteBean.getFinantialTransactionReferenceYear();
        this.originDocumentNumber = settlementNoteBean.getOriginDocumentNumber();

        if (settlementNoteBean.getAddressBean() != null) {
            SibsBillingAddressBean sibsBillingAddressBean = new SibsBillingAddressBean();
            sibsBillingAddressBean.setAddress(settlementNoteBean.getAddressBean().getAddress());
            sibsBillingAddressBean.setAddressCountryCode(settlementNoteBean.getAddressBean().getAddressCountryCode());
            sibsBillingAddressBean.setAddressCountryName(settlementNoteBean.getAddressBean().getAddressCountryName());
            sibsBillingAddressBean.setCity(settlementNoteBean.getAddressBean().getCity());
            sibsBillingAddressBean.setZipCode(settlementNoteBean.getAddressBean().getZipCode());
            this.addressBean = sibsBillingAddressBean;
        }

        this.previousStates = (Stack<Integer>) settlementNoteBean.getPreviousStates().clone();
        this.settlementNoteStateUrls = new ArrayList(settlementNoteBean.getSettlementNoteStateUrls());

        this.documentNumberSeries = new ArrayList<>();
        settlementNoteBean.getDocumentNumberSeries()
                .forEach(bean -> this.documentNumberSeries.add(new TreasuryTupleDataSourceBean(bean.getId(), bean.getText())));

        settlementNoteBean.creditEntries.forEach(bean -> {
            SettlementCreditEntryBean creditBean = new SettlementCreditEntryBean(bean.getCreditEntry());
            creditBean.setIncluded(bean.isIncluded());
            creditBean.setNotValid(bean.isNotValid());
            creditBean.setSettledAmount(bean.getSettledAmount());
            this.creditEntries.add(creditBean);
        });

        settlementNoteBean.debitEntries.stream().filter(bean -> bean.isForDebitEntry()).forEach(bean -> {
            SettlementDebitEntryBean debitEntry = new SettlementDebitEntryBean((DebitEntry) bean.getInvoiceEntry());
            debitEntry.setIncluded(bean.isIncluded());
            debitEntry.setNotValid(bean.isNotValid());
            debitEntry.setSettledAmount(bean.getSettledAmount());
            this.debitEntries.add(debitEntry);
        });

        settlementNoteBean.debitEntries.stream().filter(bean -> bean.isForInstallment()).forEach(bean -> {
            InstallmentPaymenPlanBean installmentBean =
                    new InstallmentPaymenPlanBean(((InstallmentPaymenPlanBean) bean).getInstallment());
            installmentBean.setIncluded(bean.isIncluded());
            installmentBean.setNotValid(bean.isNotValid());
            installmentBean.setSettledAmount(bean.getSettledAmount());
            this.debitEntries.add(installmentBean);
        });

        settlementNoteBean.paymentEntries.forEach(bean -> {
            this.paymentEntries.add(new PaymentEntryBean(bean.paymentAmount, bean.paymentMethod, bean.paymentMethodId));
        });

        settlementNoteBean.virtualDebitEntries.forEach(bean -> {
            String serialize = bean.serialize();
            this.virtualDebitEntries.add(ISettlementInvoiceEntryBean.deserialize(serialize));
        });
    }

    private void init() {
        this.creditEntries = new ArrayList<SettlementCreditEntryBean>();
        this.debitEntries = new ArrayList<ISettlementInvoiceEntryBean>();
        this.virtualDebitEntries = new ArrayList<ISettlementInvoiceEntryBean>();
        this.paymentEntries = new ArrayList<PaymentEntryBean>();
        this.date = new LocalDate().toDateTimeAtStartOfDay();
        this.settlementNoteStateUrls = new ArrayList<>();
        this.previousStates = new Stack<Integer>();
        setPaymentMethods(PaymentMethod.findAvailableForPaymentInApplication().collect(Collectors.toList()));

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());

        this.documentNumberSeries = new ArrayList<>();

        // Fields used by SibsPaymentRequest generation
        this.limitSibsPaymentRequestToCustomDueDate = false;
        this.customSibsPaymentRequestDueDate = null;

        // This date is used only in settlement note creation in angular ui (treasury-ui)
        // This should not be used elsewhere
        this.uiAngularPaymentDate = new LocalDate();
    }

    public void init(DebtAccount debtAccount, boolean reimbursementNote, boolean excludeDebtsForPayorAccount) {
        init(debtAccount, reimbursementNote, excludeDebtsForPayorAccount, true);
    }

    private void init(DebtAccount debtAccount, boolean reimbursementNote, boolean excludeDebtsForPayorAccount,
            boolean excludeTreasuryDebtProcessBlockingPayment) {
        init();

        this.debtAccount = debtAccount;
        this.finantialEntity = FinantialEntity.findAll()
                .filter(fe -> TreasuryAccessControlAPI
                        .isFrontOfficeMember(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername(), fe))
                .filter(fe -> fe.getFinantialInstitution() == debtAccount.getFinantialInstitution()).findFirst().orElse(null);

        this.reimbursementNote = reimbursementNote;

        List<InvoiceEntry> pendingInvoiceEntriesList = debtAccount.getPendingInvoiceEntriesSet().stream() //
                .filter(ie -> !excludeTreasuryDebtProcessBlockingPayment
                        || !TreasuryDebtProcessMainService.isBlockingPayment(ie, this.invoiceEntriesPaymentBlockingContext))
                .filter(ie -> !ie.hasPreparingSettlementEntries()) //
                .sorted(InvoiceEntry.COMPARE_BY_DUE_DATE) //
                .collect(Collectors.<InvoiceEntry> toList());

        for (InvoiceEntry invoiceEntry : pendingInvoiceEntriesList) {
            if (invoiceEntry instanceof DebitEntry) {
                final DebitEntry debitEntry = (DebitEntry) invoiceEntry;

                if (excludeDebtsForPayorAccount && debitEntry.getFinantialDocument() != null
                        && ((Invoice) debitEntry.getFinantialDocument()).isForPayorDebtAccount()) {
                    continue;
                }

                debitEntries.add(new SettlementDebitEntryBean(debitEntry));
            } else {
                final CreditEntry creditEntry = (CreditEntry) invoiceEntry;

                if (excludeDebtsForPayorAccount && creditEntry.getFinantialDocument() != null
                        && ((Invoice) creditEntry.getFinantialDocument()).isForPayorDebtAccount()) {
                    continue;
                }

                creditEntries.add(new SettlementCreditEntryBean((CreditEntry) invoiceEntry));
            }
        }

        Set<Installment> installments = debtAccount.getActivePaymentPlansSet().stream()
                .flatMap(plan -> plan.getSortedOpenInstallments().stream())
                .filter(i -> !excludeTreasuryDebtProcessBlockingPayment
                        || !TreasuryDebtProcessMainService.isBlockingPayment(i, this.invoiceEntriesPaymentBlockingContext))
                .collect(Collectors.toSet());

        for (Installment installment : installments) {
            debitEntries.add(new InstallmentPaymenPlanBean(installment));
        }

        fillDocumentNumberSeriesOptionsList(debtAccount, reimbursementNote);

        if (this.finantialEntity != null) {
            this.docNumSeries = DocumentNumberSeries.findUniqueDefaultSeries(reimbursementNote ? FinantialDocumentType
                    .findForReimbursementNote() : FinantialDocumentType.findForSettlementNote(), this.finantialEntity);
        }

        this.settlementNoteStateUrls = Arrays.asList(
                CHOOSE_INVOICE_ENTRIES_URL + debtAccount.getExternalId() + "/" + reimbursementNote, CHOOSE_INVOICE_ENTRIES_URL,
                CALCULATE_INTEREST_URL, CREATE_DEBIT_NOTE_URL, INSERT_PAYMENT_URL, SUMMARY_URL);

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());

        Collections.sort(this.debitEntries, (o1, o2) -> o1.getDueDate().compareTo(o2.getDueDate()));
    }

    public SettlementNoteBean(DebtAccount debtAccount, DigitalPaymentPlatform digitalPaymentPlatform,
            final boolean reimbursementNote, final boolean excludeDebtsForPayorAccount) {
        this.finantialEntity = digitalPaymentPlatform.getFinantialEntity();
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

        for (final SettlementCreditEntryBean creditEntryBean : creditEntries) {
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

        return !getDate().toLocalDate().isBefore(lastAdvancedCreditSettlementNote.get().getPaymentDate().toLocalDate());
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

    public void calculateVirtualDebitEntries() {
        setVirtualDebitEntries(new ArrayList<ISettlementInvoiceEntryBean>());
        List<IVirtualPaymentEntryHandler> handlers = VirtualPaymentEntryFactory.implementation().getHandlers();
        for (IVirtualPaymentEntryHandler handler : handlers) {
            addAllVirtualDebitEntries(handler.createISettlementInvoiceEntryBean(this));
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

        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
            if (!interestEntryBean.isIncluded()) {
                continue;
            }

            if (isReimbursementNote()) {
                totalAmount = totalAmount.subtract(interestEntryBean.getSettledAmount());
            } else {
                totalAmount = totalAmount.add(interestEntryBean.getSettledAmount());
            }
        }

        for (SettlementCreditEntryBean creditEntryBean : getCreditEntries()) {
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

        for (final SettlementCreditEntryBean creditEntryBean : getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                invoiceEntriesList.add(creditEntryBean);
            }
        }

        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
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

    public void markCheckingInvoiceEntriesPaymentBlockingForBackoffice() {
        this.invoiceEntriesPaymentBlockingContext = InvoiceEntryBlockingPaymentContext.BACKOFFICE;
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public List<Integer> getFinantialTransactionReferenceYears() {
        final List<Integer> years = FiscalYear.findAll().map(g -> (Integer) g.getYear()).sorted().collect(Collectors.toList());
        Collections.reverse(years);

        return years;
    }

    private void fillDocumentNumberSeriesOptionsList(DebtAccount debtAccount, boolean reimbursementNote) {
        if (this.finantialEntity != null) {
            FinantialDocumentType finantialDocumentType = (reimbursementNote) ? FinantialDocumentType
                    .findForReimbursementNote() : FinantialDocumentType.findForSettlementNote();

            Stream<DocumentNumberSeries> availableSeries =
                    DocumentNumberSeries.findActiveAndSelectableSeries(finantialDocumentType, this.finantialEntity)
                            .sorted(DocumentNumberSeries.COMPARE_BY_DEFAULT.thenComparing(DocumentNumberSeries.COMPARE_BY_NAME));

            Function<? super DocumentNumberSeries, ? extends TreasuryTupleDataSourceBean> createTuple = docNumSeries -> {
                TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();

                tuple.setText(docNumSeries.getSeries().getCode() + " - " + docNumSeries.getSeries().getName().getContent());
                tuple.setId(docNumSeries.getExternalId());

                return tuple;
            };

            this.documentNumberSeries = availableSeries.map(createTuple).collect(Collectors.toList());
        }
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public void setDebtAccount(DebtAccount debtAccount) {
        this.debtAccount = debtAccount;
    }

    public FinantialEntity getFinantialEntity() {
        return finantialEntity;
    }

    public void setFinantialEntity(FinantialEntity finantialEntity) {
        this.finantialEntity = finantialEntity;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public List<SettlementCreditEntryBean> getCreditEntries() {
        return creditEntries;
    }

    public void setCreditEntries(List<SettlementCreditEntryBean> creditEntries) {
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

    public boolean isAdvancePayment() {
        return this.advancePayment;
    }

    public void setAdvancePayment(final boolean advancePayment) {
        this.advancePayment = advancePayment;
    }

    public LocalDate getDebitNoteDate() {
        LocalDate lowerDate = new LocalDate();
        for (SettlementDebitEntryBean debitEntryBean : getDebitEntriesByType(SettlementDebitEntryBean.class)) {
            if (debitEntryBean.isIncluded() && debitEntryBean.getDueDate().isBefore(lowerDate)) {
                lowerDate = debitEntryBean.getDueDate();
            }
        }
        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
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
        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
            if (interestEntryBean.isIncluded()) {
                sum = sum.add(interestEntryBean.getSettledAmount());
            }
        }
        for (SettlementCreditEntryBean creditEntryBean : getCreditEntries()) {
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
        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
            if (interestEntryBean.isIncluded()) {
                //Interest doesn't have vat
                sum = sum.add(interestEntryBean.getSettledAmount());
            }
        }
        for (SettlementCreditEntryBean creditEntryBean : getCreditEntries()) {
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
        for (SettlementDebitEntryBean debitEntryBean : getDebitEntriesByType(SettlementDebitEntryBean.class)) {
            if (debitEntryBean.isIncluded()) {
                String vatType = debitEntryBean.getDebitEntry().getVat().getVatType().getName().getContent();
                sumByVat.get(vatType).addAmount(debitEntryBean.getDebtAmount());
                sumByVat.get(vatType).addAmountWithVat(debitEntryBean.getDebtAmountWithVat());
            }
        }
        for (ISettlementInvoiceEntryBean interestEntryBean : getVirtualDebitEntries()) {
            if (interestEntryBean.isIncluded()) {
                String vatType = interestEntryBean.getVat().getVatType().getName().getContent();
                sumByVat.get(vatType).addAmount(interestEntryBean.getSettledAmount());
                sumByVat.get(vatType).addAmountWithVat(interestEntryBean.getSettledAmount());
            }
        }
        for (SettlementCreditEntryBean creditEntryBean : getCreditEntries()) {
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
        return getDebitEntriesByType(SettlementDebitEntryBean.class).stream()
                .anyMatch(deb -> deb.isIncluded() && deb.getDebitEntry().getFinantialDocument() == null)
                || virtualDebitEntries.stream().anyMatch(deb -> deb.isIncluded());
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

    public boolean isLimitSibsPaymentRequestToCustomDueDate() {
        return this.limitSibsPaymentRequestToCustomDueDate;
    }

    public void setLimitSibsPaymentRequestToCustomDueDate(boolean limitSibsPaymentRequestToCustomDueDate) {
        this.limitSibsPaymentRequestToCustomDueDate = limitSibsPaymentRequestToCustomDueDate;
    }

    public LocalDate getCustomSibsPaymentRequestDueDate() {
        return customSibsPaymentRequestDueDate;
    }

    public void setCustomSibsPaymentRequestDueDate(LocalDate customSibsPaymentRequestDueDate) {
        this.customSibsPaymentRequestDueDate = customSibsPaymentRequestDueDate;
    }

    @Deprecated
    // This date is used only in settlement note creation in angular ui (treasury-ui)
    // This should not be used elsewhere
    public LocalDate getUiAngularPaymentDate() {
        return uiAngularPaymentDate;
    }

    @Deprecated
    // This date is used only in settlement note creation in angular ui (treasury-ui)
    // This should not be used elsewhere
    public void setUiAngularPaymentDate(LocalDate uiAngularPaymentDate) {
        this.uiAngularPaymentDate = uiAngularPaymentDate;
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

    public List<ISettlementInvoiceEntryBean> getVirtualDebitEntries() {
        return virtualDebitEntries;
    }

    public void setVirtualDebitEntries(List<ISettlementInvoiceEntryBean> virtualDebitEntries) {
        this.virtualDebitEntries = virtualDebitEntries;
    }

    private void addAllVirtualDebitEntries(List<ISettlementInvoiceEntryBean> iSettlementInvoiceEntryBeans) {
        this.virtualDebitEntries.addAll(iSettlementInvoiceEntryBeans);
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

    /* Services */

    public static SettlementNoteBean copyForSettlementNoteCreation(SettlementNoteBean bean) {
        return new SettlementNoteBean(bean);
    }

    public static SettlementNoteBean createForPaymentRequestProcessPayment(PaymentRequest paymentRequest, DateTime paymentDate,
            BigDecimal paidAmount, String originDocumentNumber) {
        return new SettlementNoteBean(paymentRequest, paymentDate, paidAmount, originDocumentNumber);
    }

    // Used by treasury debt processes
    // Invoice entries blocked are not excluded
    public static SettlementNoteBean createForTreasuryDebtProcesses(DebtAccount debtAccount) {
        SettlementNoteBean result = new SettlementNoteBean();
        result.init(debtAccount, false, false, false);

        return result;
    }

}
