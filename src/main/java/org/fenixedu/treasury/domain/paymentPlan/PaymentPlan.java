package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.beans.InstallmentBean;
import org.fenixedu.treasury.domain.paymentPlan.beans.PaymentPlanBean;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPlan extends PaymentPlan_Base {
    public PaymentPlan() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private PaymentPlan(PaymentPlanBean paymentPlanBean) {
        this();
        DebtAccount debtAccount = paymentPlanBean.getDebtAccount();

        setCreationDate(paymentPlanBean.getCreationDate());
        setReason(paymentPlanBean.getReason());
        setDebtAccount(debtAccount);
        setState(PaymentPlanStateType.OPEN);
        setStateReason(null);
        setPaymentPlanId(PaymentPlanSettings.getActiveInstance().getNumberGenerators().generateNumber());

        if (paymentPlanBean.getPaymentPlanValidator() != null) {
            getPaymentPlanValidatorsSet().add(paymentPlanBean.getPaymentPlanValidator());
        }

        LocalDate endDate = paymentPlanBean.getEndDate();
        LocalDate creationDate = this.getCreationDate();
        boolean hasEmolument = !TreasuryConstants.isZero(paymentPlanBean.getEmolumentAmount());
        boolean hasInterest = !TreasuryConstants.isZero(paymentPlanBean.getInterestAmount());
        if (hasEmolument || hasInterest) {
            if (hasEmolument) {
                createEmolument(paymentPlanBean, debtAccount, endDate, creationDate);
            }
            if (hasInterest) {
                createInterests(paymentPlanBean, debtAccount, endDate, creationDate);

            }
        }
        createInstallments(paymentPlanBean);
        createPaymentReferenceCode();
        annulPaymentReferenceCodeFromDebitEntries(paymentPlanBean.getDebitEntries());
        checkRules();
    }

    public void createPaymentReferenceCode() {

        DigitalPaymentPlatform paymentCodePool = getDebtAccount().getFinantialInstitution().getDefaultDigitalPaymentPlatform();

        if (paymentCodePool == null) {
            throw new IllegalArgumentException(TreasuryConstants.treasuryBundle("error.paymentPlan.paymentCodePool.required"));
        }

        for (Installment installment : getInstallmentsSet()) {
            paymentCodePool.castToSibsPaymentCodePoolService().createSibsPaymentRequest(getDebtAccount(), Collections.emptySet(),
                    Set.of(installment));
        }
    }

    private void annulPaymentReferenceCodeFromDebitEntries(List<DebitEntry> list) {
        for (DebitEntry entry : list) {
            Set<SibsPaymentRequest> paymentCodesSet =
                    entry.getSibsPaymentRequests().stream().filter(s -> !s.isInPaidState()).collect(Collectors.toSet());
            for (SibsPaymentRequest paymentCode : paymentCodesSet) {
                if (paymentCode.getDebitEntriesSet().size() == 1 && paymentCode.getInstallmentsSet().isEmpty()) {
                    paymentCode.anull();
                }
            }
        }
    }

    @Atomic
    public static PaymentPlan createPaymentPlan(PaymentPlanBean paymentPlanBean) {
        return new PaymentPlan(paymentPlanBean);
    }

    @Atomic
    public void annul(String reason) {
        setState(PaymentPlanStateType.ANNULED);
        setStateReason(reason);
    }

    @Atomic
    public void close(String reason) {
        setState(PaymentPlanStateType.CLOSED);
        setStateReason(reason);
    }

    @Atomic
    public void nonCompliance(LocalDate date) {
        setState(PaymentPlanStateType.NON_COMPLIANCE);
        setStateReason(
                TreasuryConstants.treasuryBundle("label.PaymentPlan.paymentPlan.nonCompliance", date.toString("dd/MM/yyyy")));
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }

    public void checkRules() {
        if (getReason() == null) {
            throw new TreasuryDomainException("error.paymentPlan.reason.required");
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
        if (getCustomers().size() > 1) {
            throw new TreasuryDomainException("error.paymentPlan.multiple.customers");
        }

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() && hasDebitEntriesExportedInLegacyERP()) {
            throw new TreasuryDomainException(
                    "error.PaymentPlan.debitEntries.exported.in.legacyERP.not.supported.in.restrictedPaymentMode");
        }
    }

    private boolean hasDebitEntriesExportedInLegacyERP() {
        return getInstallmentsSet().stream().flatMap(i -> i.getInstallmentEntriesSet().stream()).map(i -> i.getDebitEntry())
                .anyMatch(d -> d.getFinantialDocument() != null && d.getFinantialDocument().isExportedInLegacyERP());
    }

    private Set<Customer> getCustomers() {
        Set<DebitEntry> debitEntries = getInstallmentsSet().stream().flatMap(i -> i.getInstallmentEntriesSet().stream())
                .map(ie -> ie.getDebitEntry()).collect(Collectors.toSet());

        return debitEntries.stream()
                .map(entry -> (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument())
                        .isForPayorDebtAccount()) ? ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount()
                                .getCustomer() : entry.getDebtAccount().getCustomer())
                .collect(Collectors.toSet());
    }

    private static Optional<DebitNote> createDebitNote(PaymentPlanBean paymentPlanBean, PaymentPlan result) {
        return Optional.of(DebitNote.create(paymentPlanBean.getDebtAccount(),
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                        paymentPlanBean.getDebtAccount().getFinantialInstitution()).get(),
                result.getCreationDate().toDateTimeAtStartOfDay()));
    }

    private static DebitEntry createDebitEntry(DebtAccount debtAccount, Optional<DebitNote> debitNote, String description,
            BigDecimal amount, LocalDate creationDate, LocalDate endDate, Product product, Vat vat) {
        return DebitEntry.create(debitNote, debtAccount, null, vat, amount, endDate, Maps.newHashMap(), product, description,
                BigDecimal.ONE, null, creationDate.toDateTimeAtStartOfDay());
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

    private void createEmolument(PaymentPlanBean paymentPlanBean, DebtAccount debtAccount, LocalDate endDate,
            LocalDate creationDate) {
        Optional<DebitNote> debitNote;
        debitNote = createDebitNote(paymentPlanBean, this);

        PaymentPlanSettings activeInstance = PaymentPlanSettings.getActiveInstance();
        if (activeInstance == null) {
            throw new RuntimeException("error.paymentPlan.paymentPlanSettings.required");
        }
        Product product = activeInstance.getEmolumentProduct();
        BigDecimal amount = paymentPlanBean.getEmolumentAmount();
        String description = product.getName().getContent() + "-" + this.getPaymentPlanId();
        Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime()).orElse(null);

        DebitEntry emolument = createDebitEntry(debtAccount, debitNote, description, amount, creationDate, endDate, product, vat);

        setEmolument(emolument);
    }

    private void createInterests(PaymentPlanBean paymentPlanBean, DebtAccount debtAccount, LocalDate endDate,
            LocalDate creationDate) {
        Optional<DebitNote> debitNote;
        Product product = TreasurySettings.getInstance().getInterestProduct();
        Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime()).orElse(null);

        BigDecimal rest = paymentPlanBean.getInterestAmount().add(BigDecimal.ZERO);

        List<DebitEntry> debitEntries = paymentPlanBean.getDebitEntries();
        int i = 0;
        while (TreasuryConstants.isGreaterThan(rest, BigDecimal.ZERO)) {
            DebitEntry debitEntry = debitEntries.get(i);
            BigDecimal maxInterestAmount = debitEntry.getPendingInterestAmount();

            if (!TreasuryConstants.isZero(maxInterestAmount)) {
                debitNote = createDebitNote(paymentPlanBean, this);

                BigDecimal amount = TreasuryConstants.isGreaterThan(rest, maxInterestAmount) ? maxInterestAmount : rest;
                rest = rest.subtract(amount);
                String description = product.getName().getContent() + "-" + debitEntry.getDescription();

                DebitEntry interest =
                        createDebitEntry(debtAccount, debitNote, description, amount, creationDate, endDate, product, vat);

                debitEntry.addInterestDebitEntries(interest);
                paymentPlanBean.getDebitEntries().add(interest);
            }
            i++;
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

        for (DebitEntry debitEntry : paymentPlanBean.getDebitEntries()) {
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

    public boolean isCompliant() {
        return isCompliant(LocalDate.now());
    }

    public boolean isCompliant(LocalDate date) {
        return getPaymentPlanValidatorsSet().stream().allMatch(v -> v.validate(date, getSortedInstallments()));
    }

    public void tryClosePaymentPlanByPaidOff() {
        if (getSortedOpenInstallments().isEmpty()) {
            close(TreasuryConstants.treasuryBundle("label.PaymentPlan.paymentPlan.paidOff"));
        }
    }

    public static void validatePaymentPlanInNonCompliance() {
        for (PaymentPlan paymentPlan : FenixFramework.getDomainRoot().getPaymentPlansSet()) {
            if (!paymentPlan.getState().isOpen()) {
                continue;
            }
            if (!paymentPlan.isCompliant()) {
                paymentPlan.nonCompliance(LocalDate.now());
            }

        }
    }

}
