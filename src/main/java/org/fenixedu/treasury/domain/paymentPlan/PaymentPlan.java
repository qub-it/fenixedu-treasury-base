package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPlan extends PaymentPlan_Base {
    private static final int NB_CONSECUTIVE_INSTALLMENTS = 3;
    private static final int NB_NON_CONSECUTIVE_INSTALLMENTS = 6;
    private static final int DAYS_AFTER_LAST = 30;

    public PaymentPlan() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private PaymentPlan(PaymentPlanBean paymentPlanBean) {
        this();
        DebtAccount debtAccount = paymentPlanBean.getDebtAccount();

        setCreationDate(paymentPlanBean.getCreationDate());
        setDescription(paymentPlanBean.getDescription());
        setDebtAccount(debtAccount);
        setState(PaymentPlanStateType.OPEN);
        setReason(null);
        setName(paymentPlanBean.getName());

        LocalDate endDate = paymentPlanBean.getEndDate();
        DateTime creationDate = this.getCreationDate();
        boolean hasEmolument = !TreasuryConstants.isZero(paymentPlanBean.getEmolumentAmount());
        boolean hasInterest = !TreasuryConstants.isZero(paymentPlanBean.getInterestAmount());
        if (hasEmolument || hasInterest) {
            Optional<DebitNote> debitNote = null;

            if (hasEmolument) {
                debitNote = createDebitNote(paymentPlanBean, this);

                PaymentPlanSettings activeInstance = PaymentPlanSettings.getActiveInstance();
                if (activeInstance == null) {
                    throw new RuntimeException("error.paymentPlan.paymentPlanSettings.required");
                }
                Product product = activeInstance.getEmolumentProduct();
                BigDecimal amount = paymentPlanBean.getEmolumentAmount();
                String description = product.getName().getContent() + "-" + this.getName();
                Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime())
                        .orElse(null);

                DebitEntry emolument =
                        createDebitEntry(debtAccount, debitNote, description, amount, creationDate, endDate, product, vat);
                debitNote.get().closeDocument();

                setEmolument(emolument);
            }
            if (hasInterest) {
                Product product = TreasurySettings.getInstance().getInterestProduct();
                Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime())
                        .orElse(null);

                BigDecimal rest = paymentPlanBean.getInterestAmount().add(BigDecimal.ZERO);

                List<DebitEntry> debitEntries = paymentPlanBean.getDebitNotes();
                int i = 0;
                while (TreasuryConstants.isGreaterThan(rest, BigDecimal.ZERO)) {
                    DebitEntry debitEntry = debitEntries.get(i);
                    BigDecimal maxInterestAmount = debitEntry.getPendingInterestAmount();

                    if (!TreasuryConstants.isZero(maxInterestAmount)) {
                        debitNote = createDebitNote(paymentPlanBean, this);

                        BigDecimal amount = TreasuryConstants.isGreaterThan(rest, maxInterestAmount) ? maxInterestAmount : rest;
                        rest = rest.subtract(amount);
                        String description = product.getName().getContent() + "-" + debitEntry.getDescription();

                        DebitEntry interest = createDebitEntry(debtAccount, debitNote, description, amount, creationDate, endDate,
                                product, vat);

                        debitNote.get().closeDocument();

                        debitEntry.addInterestDebitEntries(interest);
                        paymentPlanBean.getDebitNotes().add(interest);
                    }
                    i++;
                }

            }
//			debitNote = createDebitNote(paymentPlanBean, this);

        }
        createInstallments(paymentPlanBean);
        checkRules();
    }

    @Atomic
    public static PaymentPlan createPaymentPlan(PaymentPlanBean paymentPlanBean) {

        return new PaymentPlan(paymentPlanBean);
    }

    @Atomic
    public void annul(String reason) {
        setState(PaymentPlanStateType.ANNULED);
        setReason(reason);
    }

    @Atomic
    public void close(String reason) {
        setState(PaymentPlanStateType.CLOSED);
        setReason(reason);
    }

    private void checkRules() {
        if (getDescription() == null) {
            throw new TreasuryDomainException("error.paymentPlan.description.required");
        }
        if (getName() == null) {
            throw new TreasuryDomainException("error.paymentPlan.name.required");
        }
        if (getCreationDate() == null) {
            throw new TreasuryDomainException("error.paymentPlan.creationDate.required");
        }
        if (getState() == null) {
            throw new TreasuryDomainException("error.paymentPlan.creationDate.required");
        }
        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.paymentPlan.creationDate.required");
        }
        if (getInstallmentsSet() == null || getInstallmentsSet().isEmpty()) {
            throw new TreasuryDomainException("error.paymentPlan.installments.required");
        }
        if (getDebtAccount().getActivePaymentPlansSet().size() > PaymentPlanSettings.getActiveInstance()
                .getNumberOfPaymentPlansActives()) {
            throw new TreasuryDomainException("error.paymentPlan.max.active.plans.reached");
        }

    }

    private static Optional<DebitNote> createDebitNote(PaymentPlanBean paymentPlanBean, PaymentPlan result) {
        return Optional
                .of(DebitNote
                        .create(paymentPlanBean.getDebtAccount(),
                                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                        paymentPlanBean.getDebtAccount().getFinantialInstitution()).get(),
                                result.getCreationDate()));
    }

    private static DebitEntry createDebitEntry(DebtAccount debtAccount, Optional<DebitNote> debitNote, String description,
            BigDecimal amount, DateTime creationDate, LocalDate endDate, Product product, Vat vat) {
        return DebitEntry.create(debitNote, debtAccount, null, vat, amount, endDate, Maps.newHashMap(), product, description,
                BigDecimal.ONE, null, creationDate);
    }

    private void createInstallments(PaymentPlanBean paymentPlanBean) {
        Map<DebitEntry, BigDecimal> mapDebitEntries = getDebitEntriesMap(paymentPlanBean);

        List<DebitEntry> keys = getSortEntriesList(mapDebitEntries.keySet());

        for (InstallmentBean installmentBean : paymentPlanBean.getInstallmentsBean()) {
            Installment installment = Installment.create(installmentBean.getDescription(), installmentBean.getDueDate(), this);
            BigDecimal rest = BigDecimal.ZERO.add(installmentBean.getInstallmentAmmount());
            while (TreasuryConstants.isGreaterThan(rest, BigDecimal.ZERO)) {
                DebitEntry debitEntry = keys.get(0);
                BigDecimal debitAmount = mapDebitEntries.get(debitEntry);
                if (TreasuryConstants.isGreaterThan(debitAmount, rest)) {
                    mapDebitEntries.replace(debitEntry, debitAmount.subtract(rest));
                    debitAmount = rest;
                } else {
                    keys.remove(debitEntry);
                    mapDebitEntries.remove(debitEntry);
                }
                rest = rest.subtract(debitAmount);
                InstallmentEntry.create(debitEntry, debitAmount, installment);
            }
        }
    }

    public List<DebitEntry> getSortEntriesList(Collection<DebitEntry> collection) {
        return collection.stream().sorted(DebitEntry.COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN).collect(Collectors.toList());
    }

    public Map<DebitEntry, BigDecimal> getDebitEntriesMap(PaymentPlanBean paymentPlanBean) {
        Map<DebitEntry, BigDecimal> mapDebitEntries = new LinkedHashMap<>();

        if (getEmolument() != null) {
            mapDebitEntries.put(getEmolument(), getEmolument().getAmountInDebt(LocalDate.now()));
        }

        for (DebitEntry debitEntry : paymentPlanBean.getDebitNotes()) {
            mapDebitEntries.put(debitEntry, debitEntry.getAmountInDebt(LocalDate.now()));
        }
        return mapDebitEntries;
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        getEmolument().delete();

        getInstallmentsSet().forEach(i -> {
            i.delete();
        });
        deleteDomainObject();
    }

    public boolean isOpen() {
        return getState().equals(PaymentPlanStateType.OPEN);
    }

    public List<Installment> getSortedOpenInstallments() {
        return super.getInstallmentsSet().stream().filter(inst -> !inst.isPaid()).sorted(Installment.COMPARE_BY_DUEDATE)
                .collect(Collectors.toList());
    }

    public List<Installment> getSortedInstallments() {
        return super.getInstallmentsSet().stream().sorted(Installment.COMPARE_BY_DUEDATE).collect(Collectors.toList());
    }

    public BigDecimal getTotalDebitEntry(DebitEntry debitEntry) {
        return getInstallmentsSet().stream().flatMap(inst -> inst.getInstallmentEntriesSet().stream())
                .filter(ent -> ent.getDebitEntry() == debitEntry).map(i -> i.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Boolean isCompliant() {
        return validate(LocalDate.now(), getSortedInstallments());
    }

    public Boolean isCompliant(LocalDate date) {
        return validate(date, getSortedInstallments());
    }

    private boolean validate(LocalDate date, List<Installment> sortedInstallments) {

        return withoutXConsecutiveInstallmentsOverdueRule(date, sortedInstallments)
                && withoutXNonConsecutiveInstallmentsOverdueRule(date, sortedInstallments);
    }

    private boolean withoutXNonConsecutiveInstallmentsOverdueRule(LocalDate date, List<Installment> sortedInstallments) {

        return NB_NON_CONSECUTIVE_INSTALLMENTS > sortedInstallments.stream()
                .filter(inst -> inst.isOverdue(date) && inst.getDueDate().isBefore(date.minusDays(DAYS_AFTER_LAST))).count();
    }

    private boolean withoutXConsecutiveInstallmentsOverdueRule(LocalDate date, List<Installment> sortedInstallments) {
        int count = 0;
        for (Installment installment : sortedInstallments) {
            if (installment.isOverdue(date) && installment.getDueDate().isBefore(date.minusDays(DAYS_AFTER_LAST))) {
                count++;
                if (count == NB_CONSECUTIVE_INSTALLMENTS) {
                    return false;
                }
            } else {
                count = 0;
            }
        }
        return true;
    }

}
