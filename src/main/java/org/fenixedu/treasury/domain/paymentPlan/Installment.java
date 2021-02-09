package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Installment extends Installment_Base {

    public static final Comparator<? super Installment> COMPARE_BY_DUEDATE =
            (m1, m2) -> m1.getDueDate().compareTo(m2.getDueDate());

    public Installment() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public Installment(LocalizedString description, LocalDate dueDate, PaymentPlan paymentPlan) {
        this();
        setDescription(description);
        setDueDate(dueDate);
        setPaymentPlan(paymentPlan);
        checkRules();
    }

    @Atomic
    public static Installment create(LocalizedString description, LocalDate dueDate, PaymentPlan paymentPlan) {
        return new Installment(description, dueDate, paymentPlan);
    }

    private void checkRules() {
        if (getDescription() == null) {
            throw new TreasuryDomainException("error.Installment.description.required");
        }
        if (getDueDate() == null) {
            throw new TreasuryDomainException("error.Installment.dueDate.required");
        }

        if (getPaymentPlan() == null) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.required");
        }
        if (getDueDate().isBefore(getPaymentPlan().getCreationDate().toLocalDate())) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.must.be.after.paymentPlan.creationDate");
        }
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        getInstallmentEntriesSet().forEach(i -> i.delete());
        deleteDomainObject();
    }

    public BigDecimal getTotalAmount() {
        return getCurrency().getValueWithScale(
                getInstallmentEntriesSet().stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getPaidAmount() {
        return getCurrency().getValueWithScale(
                getInstallmentEntriesSet().stream().map(i -> i.getPaidAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getOpenAmount() {
        return getCurrency().getValueWithScale(getTotalAmount().subtract(getPaidAmount()));
    }

    public boolean isPaid() {
        return TreasuryConstants.isZero(getOpenAmount());
    }

    public List<InstallmentEntry> getSortedInstallmentEntries() {
        return super.getInstallmentEntriesSet().stream().sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<InstallmentEntry> getSortedOpenInstallmentEntries() {
        return super.getInstallmentEntriesSet().stream().filter(d -> !d.isPaid())
                .sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR).collect(Collectors.toList());
    }

    private Currency getCurrency() {
        return getPaymentPlan().getDebtAccount().getFinantialInstitution().getCurrency();
    }

    public boolean isOverdue() {
        return isOverdue(LocalDate.now());
    }

    public boolean isOverdue(LocalDate date) {
        return !isPaid() && getDueDate().isBefore(date);
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }
    
    public static LocalizedString installmentDescription(int installmentNumber, String paymentPlanId) {
        Map<String, String> values = new HashMap<>();
        values.put("installmentNumber", "" + installmentNumber);
        values.put("paymentPlanId", paymentPlanId);
        
        PaymentPlanSettings activeInstance = PaymentPlanSettings.getActiveInstance();
        if (activeInstance == null) {
            throw new RuntimeException("error.paymentPlanBean.paymentPlanSettings.required");
        }
        
        LocalizedString installmentDescriptionFormat = PaymentPlanSettings.getActiveInstance().getInstallmentDescriptionFormat();
        
        LocalizedString ls = new LocalizedString();
        for (Locale locale : CoreConfiguration.supportedLocales()) {
            ls = ls.with(locale, StrSubstitutor.replace(installmentDescriptionFormat.getContent(locale), values));
        }

        return ls;
    }
}
