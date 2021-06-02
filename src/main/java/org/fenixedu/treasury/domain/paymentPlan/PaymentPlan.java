package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.PendingDebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.DebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.InterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
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
        setPaymentPlanId(paymentPlanBean.getPaymentPlanConfigurator().getNumberGenerators().generateNumber());

        if (paymentPlanBean.getPaymentPlanValidator() != null) {
            getPaymentPlanValidatorsSet().add(paymentPlanBean.getPaymentPlanValidator());
        }

        Map<ISettlementInvoiceEntryBean, DebitEntry> createdEntries = createDebitEntriesMap(paymentPlanBean);

        createInstallments(paymentPlanBean, createdEntries);
        createPaymentReferenceCode();
        annulPaymentReferenceCodeFromDebitEntries(
                paymentPlanBean.getSettlementInvoiceEntryBeans().stream().filter(bean -> bean.isForDebitEntry())
                        .map(bean -> ((DebitEntryBean) bean).getDebitEntry()).collect(Collectors.toList()));
        checkRules();
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
    // TODO: By the method name, it is difficult to know that this method change instance data. The method name should be renamed.
    public void nonCompliance(LocalDate date) {
        setState(PaymentPlanStateType.NON_COMPLIANCE);
        setStateReason(
                TreasuryConstants.treasuryBundle("label.PaymentPlan.paymentPlan.nonCompliance", date.toString("yyyy-MM-dd")));
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
        if (getDebtAccount().getActivePaymentPlansSet().size() > TreasurySettings.getInstance()
                .getNumberOfPaymentPlansActivesPerStudent()) {
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

    private Map<ISettlementInvoiceEntryBean, DebitEntry> createDebitEntriesMap(PaymentPlanBean paymentPlanBean) {
        Map<ISettlementInvoiceEntryBean, DebitEntry> result = new HashMap<ISettlementInvoiceEntryBean, DebitEntry>();
        DebtAccount debtAccount = paymentPlanBean.getDebtAccount();
        LocalDate creationDate = paymentPlanBean.getCreationDate();
        LocalDate endDate = paymentPlanBean.getEndDate();

        //DebitEntries (DebitEntries)
        paymentPlanBean.getSettlementInvoiceEntryBeans().stream().filter(pendingBean -> pendingBean.isForDebitEntry())
                .forEach(debitEntryBean -> {
                    result.put(debitEntryBean, (DebitEntry) debitEntryBean.getInvoiceEntry());
                });

        //PendingDebitEntries (Emolument)
        paymentPlanBean.getSettlementInvoiceEntryBeans().stream().filter(pendingBean -> pendingBean.isForPendingDebitEntry())
                .forEach(pendigBean -> {
                    PendingDebitEntryBean debitEntryBean = (PendingDebitEntryBean) pendigBean;

                    Optional<DebitNote> debitNote = createDebitNote(paymentPlanBean, this);
                    Product product = debitEntryBean.getProduct();
                    Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime())
                            .orElse(null);

                    DebitEntry debitEntry = createDebitEntry(debtAccount, debitNote, debitEntryBean.getDescription(),
                            debitEntryBean.getSettledAmount(), creationDate, endDate, product, vat);

                    result.put(pendigBean, debitEntry);
                });

        //Interests (Interests)
        paymentPlanBean.getSettlementInvoiceEntryBeans().stream().filter(pendingBean -> pendingBean.isForPendingInterest())
                .forEach(pendigBean -> {
                    InterestEntryBean interestEntryBean = (InterestEntryBean) pendigBean;

                    Optional<DebitNote> debitNote = createDebitNote(paymentPlanBean, this);
                    Product product = TreasurySettings.getInstance().getInterestProduct();
                    Vat vat = Vat.findActiveUnique(product.getVatType(), debtAccount.getFinantialInstitution(), new DateTime())
                            .orElse(null);

                    DebitEntry debitEntry = createDebitEntry(debtAccount, debitNote, interestEntryBean.getDescription(),
                            interestEntryBean.getSettledAmount(), creationDate, endDate, product, vat);

                    interestEntryBean.getDebitEntry().addInterestDebitEntries(debitEntry);
                    result.put(pendigBean, debitEntry);
                });
        return result;
    }

    private void createInstallments(PaymentPlanBean paymentPlanBean,
            Map<ISettlementInvoiceEntryBean, DebitEntry> createdEntries) {

        for (InstallmentBean installmentBean : paymentPlanBean.getInstallmentsBean()) {
            Installment installment = Installment.create(installmentBean.getDescription(), installmentBean.getDueDate(), this);
            for (InstallmentEntryBean installmentEntryBean : installmentBean.getInstallmentEntries()) {
                DebitEntry debitEntry = createdEntries.get(installmentEntryBean.getInvoiceEntry());
                InstallmentEntry.create(debitEntry, installmentEntryBean.getAmount(), installment);
            }
        }
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

    public List<DebitEntry> getSortEntriesList(Collection<DebitEntry> collection) {
        return collection.stream().sorted(DebitEntry.COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN).collect(Collectors.toList());
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
            close(TreasuryConstants.treasuryBundle("label.PaymentPlan.paymentPlan.paidOff") + " [" + new LocalDate().toString("yyyy-MM-dd") + "]");
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
