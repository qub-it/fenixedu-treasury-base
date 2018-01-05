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
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public class SettlementNoteBean implements ITreasuryBean, Serializable {

    public static final String CONTROLLER_URL = "/treasury/document/managepayments/settlementnote";
    public static final String SEARCH_URI = "/";
    public static final String SEARCH_URL = CONTROLLER_URL + SEARCH_URI;
    public static final String UPDATE_URI = "/update/";
    public static final String UPDATE_URL = CONTROLLER_URL + UPDATE_URI;
    public static final String CREATE_URI = "/create";
    public static final String CREATE_URL = CONTROLLER_URL + CREATE_URI;
    public static final String READ_URI = "/read/";
    public static final String READ_URL = CONTROLLER_URL + READ_URI;
    public static final String DELETE_URI = "/delete/";
    public static final String DELETE_URL = CONTROLLER_URL + DELETE_URI;
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
    public static final String TRANSACTIONS_SUMMARY_URI = "/transactions/summary/";
    public static final String TRANSACTIONS_SUMMARY_URL = CONTROLLER_URL + TRANSACTIONS_SUMMARY_URI;
	
	
    private static final long serialVersionUID = 1L;

    private boolean reimbursementNote;

    private DebtAccount debtAccount;

    private LocalDate date;

    private DocumentNumberSeries docNumSeries;

    private String originDocumentNumber;

    private List<CreditEntryBean> creditEntries;

    private List<DebitEntryBean> debitEntries;

    private List<InterestEntryBean> interestEntries;

    private List<PaymentEntryBean> paymentEntries;

    private List<TreasuryTupleDataSourceBean> paymentMethods;

    private List<TreasuryTupleDataSourceBean> documentNumberSeries;

    private List<String> settlementNoteStateUrls;

    private Stack<Integer> previousStates;

    private String finantialTransactionReferenceYear;

    private String finantialTransactionReference;

    private boolean advancePayment;

    public SettlementNoteBean() {
        creditEntries = new ArrayList<CreditEntryBean>();
        debitEntries = new ArrayList<DebitEntryBean>();
        interestEntries = new ArrayList<InterestEntryBean>();
        paymentEntries = new ArrayList<PaymentEntryBean>();
        date = new LocalDate();
        previousStates = new Stack<Integer>();
        this.setPaymentMethods(PaymentMethod.findAvailableForPaymentInApplication().collect(Collectors.toList()));

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());
    }

    public SettlementNoteBean(DebtAccount debtAccount, final boolean reimbursementNote,
            final boolean excludeDebtsForPayorAccount) {
        this();
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

//                if (reimbursementNote
//                        && !(invoiceEntry.getFinantialDocument() != null && (invoiceEntry.getFinantialDocument().isPreparing()
//                                || ((CreditNote) invoiceEntry.getFinantialDocument()).isAdvancePayment()))) {
//                    continue;
//                }

                creditEntries.add(new CreditEntryBean((CreditEntry) invoiceEntry));
            }
        }

        setDocumentNumberSeries(debtAccount, reimbursementNote);

        // @formatter:off
        settlementNoteStateUrls =
                Arrays.asList(
                        CHOOSE_INVOICE_ENTRIES_URL + debtAccount.getExternalId() + "/" + reimbursementNote,
                                CHOOSE_INVOICE_ENTRIES_URL, 
                                CALCULATE_INTEREST_URL,
                                CREATE_DEBIT_NOTE_URL, 
                                INSERT_PAYMENT_URL,
                                SUMMARY_URL);
        // @formatter:on

        this.advancePayment = false;
        this.finantialTransactionReferenceYear = String.valueOf((new LocalDate()).getYear());
    }

    public Set<Customer> getReferencedCustomers() {
        final Set<Customer> result = Sets.newHashSet();

        for (final DebitEntryBean debitEntryBean : debitEntries) {
            if (!debitEntryBean.isIncluded()) {
                continue;
            }

            final DebitEntry debitEntry = debitEntryBean.getDebitEntry();

            if (debitEntry.getFinantialDocument() != null
                    && ((Invoice) debitEntry.getFinantialDocument()).getPayorDebtAccount() != null) {
                result.add(((Invoice) debitEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(debitEntry.getDebtAccount().getCustomer());
        }

        for (final CreditEntryBean creditEntryBean : creditEntries) {
            if (!creditEntryBean.isIncluded()) {
                continue;
            }

            final CreditEntry creditEntry = creditEntryBean.getCreditEntry();

            if (creditEntry.getFinantialDocument() != null
                    && ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount() != null) {
                result.add(((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(creditEntry.getDebtAccount().getCustomer());
        }

        return result;
    }
    
    public boolean isReimbursementWithCompensation() {
        if(!isReimbursementNote()) {
            return false;
        }
        
        if(getCreditEntries().stream().filter(ce -> ce.isIncluded()).count() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.reimbursement.supports.only.one.settlement.entry");
        }
        
        final CreditEntry creditEntry = getCreditEntries().stream().filter(ce -> ce.isIncluded()).findFirst().get().getCreditEntry();
        return ReimbursementUtils.isCreditNoteForReimbursementMustBeClosedWithDebitNoteAndCreatedNew(creditEntry);
    }
    
    public boolean checkAdvancePaymentCreditsWithPaymentDate() {
        final Optional<SettlementNote> lastAdvancedCreditSettlementNote = getLastPaidAdvancedCreditSettlementNote();

        if(!lastAdvancedCreditSettlementNote.isPresent()) {
            return true;
        }

        return !getDate().isBefore(lastAdvancedCreditSettlementNote.get().getPaymentDate().toLocalDate());
    }

    public Optional<SettlementNote> getLastPaidAdvancedCreditSettlementNote() {
        final Optional<SettlementNote> lastAdvancedCreditSettlementNote = getCreditEntries().stream()
                .filter(ce -> ce.isIncluded())
                .filter(ce -> ce.getCreditEntry().getFinantialDocument() != null && 
                    ((CreditNote) ce.getCreditEntry().getFinantialDocument()).isAdvancePayment())
                .map(ce -> ce.getCreditEntry().getFinantialDocument())
                .map(AdvancedPaymentCreditNote.class::cast)
                .filter(c -> c.getAdvancedPaymentSettlementNote() != null)
                .map(c -> c.getAdvancedPaymentSettlementNote())
                .sorted(Comparator.comparing(SettlementNote::getPaymentDate).reversed())
                .findFirst();
        
        return lastAdvancedCreditSettlementNote;
    }

    public void calculateInterestDebitEntries() {
        setInterestEntries(new ArrayList<InterestEntryBean>());
        for (DebitEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()
                    && Constants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(), debitEntryBean.getDebtAmount())) {

                //Calculate interest only if we are making a FullPayment
                InterestRateBean debitInterest = debitEntryBean.getDebitEntry().calculateUndebitedInterestValue(getDate());
                if (debitInterest.getInterestAmount().compareTo(BigDecimal.ZERO) != 0) {
                    InterestEntryBean interestEntryBean = new InterestEntryBean(debitEntryBean.getDebitEntry(), debitInterest);
                    getInterestEntries().add(interestEntryBean);
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

    public List<DebitEntryBean> getDebitEntries() {
        return debitEntries;
    }

    public void setDebitEntries(List<DebitEntryBean> debitEntries) {
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
        for (DebitEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded() && debitEntryBean.getDocumentDueDate().isBefore(lowerDate)) {
                lowerDate = debitEntryBean.getDocumentDueDate();
            }
        }
        for (InterestEntryBean interestEntryBean : getInterestEntries()) {
            if (interestEntryBean.isIncluded() && interestEntryBean.getDocumentDueDate().isBefore(lowerDate)) {
                lowerDate = interestEntryBean.getDocumentDueDate();
            }
        }
        return lowerDate;
    }

    public BigDecimal getDebtAmount() {
        BigDecimal sum = BigDecimal.ZERO;
        for (DebitEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()) {
                sum = sum.add(debitEntryBean.getDebtAmount());
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
        for (DebitEntryBean debitEntryBean : getDebitEntries()) {
            if (debitEntryBean.isIncluded()) {
                sum = sum.add(debitEntryBean.getDebtAmountWithVat());
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
        for (DebitEntryBean debitEntryBean : getDebitEntries()) {
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
        return debitEntries.stream().anyMatch(deb -> deb.isIncluded() && deb.getDebitEntry().getFinantialDocument() == null)
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

    // @formatter:off
    /* ************
     * HELPER BEANS
     * ************
     */
    // @formatter:on

    public class DebitEntryBean implements ITreasuryBean, Serializable {

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

        public LocalDate getDocumentDueDate() {
            return debitEntry.getDueDate();
        }

        public boolean isIncluded() {
            return isIncluded;
        }

        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

        public BigDecimal getDebtAmount() {
            if (debtAmount == null) {
                return null;
            }

            return debitEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(debtAmount);
        }

        public BigDecimal getDebtAmountWithVat() {
            if (debtAmount == null) {
                return null;
            }

            return debitEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(debtAmount);
        }

        public void setDebtAmount(BigDecimal debtAmount) {
            this.debtAmount = debtAmount;
        }

        public boolean isNotValid() {
            return isNotValid;
        }

        public void setNotValid(boolean notValid) {
            this.isNotValid = notValid;
        }
    }

    public class CreditEntryBean implements ITreasuryBean, Serializable {

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

        public LocalDate getDocumentDueDate() {
            return creditEntry.getFinantialDocument() != null ? creditEntry.getFinantialDocument()
                    .getDocumentDueDate() : creditEntry.getEntryDateTime().toLocalDate();
        }

        public boolean isIncluded() {
            return isIncluded;
        }

        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

        public BigDecimal getCreditAmount() {
            if (creditAmount == null) {
                return null;
            }

            return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
        }

        public BigDecimal getCreditAmountWithVat() {
            if (creditAmount == null) {
                return null;
            }

            return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
        }

        public void setCreditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }

        public boolean isNotValid() {
            return isNotValid;
        }

        public void setNotValid(boolean notValid) {
            this.isNotValid = notValid;
        }
    }

    public static class InterestEntryBean implements ITreasuryBean, Serializable {

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

        public boolean isIncluded() {
            return isIncluded;
        }

        public void setIncluded(boolean isIncluded) {
            this.isIncluded = isIncluded;
        }

    }

    public class PaymentEntryBean implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal paymentAmount;

        private PaymentMethod paymentMethod;

        private String paymentMethodId;

        public PaymentEntryBean() {
            this.paymentAmount = BigDecimal.ZERO;
        }

        public PaymentEntryBean(BigDecimal paymentAmount, PaymentMethod paymentMethod) {
            this.paymentAmount = paymentAmount;
            this.paymentMethod = paymentMethod;
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
