package org.fenixedu.treasury.domain.sibspay;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class MbwayMandatePaymentSchedule extends MbwayMandatePaymentSchedule_Base {

    public MbwayMandatePaymentSchedule() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setState(MbwayMandatePaymentScheduleState.SCHEDULED);
    }

    public MbwayMandatePaymentSchedule(MbwayMandate mbwayMandate, DateTime sendEmailDate, DateTime paymentChargeDate,
            Set<DebitEntry> debitEntriesSet, Set<Installment> installmentSet) {
        this();

        setMbwayMandate(mbwayMandate);
        setSendEmailDate(sendEmailDate);
        setPaymentChargeDate(paymentChargeDate);

        getDebitEntriesSet().addAll(debitEntriesSet);
        getInstallmentsSet().addAll(installmentSet);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.domainRoot.required");
        }

        if (getCreationDate() == null) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.creationDate.required");
        }

        if (getMbwayMandate() == null) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.mbwayMandate.required");
        }

        if (PaymentRequest.getReferencedCustomers(getDebitEntriesSet(), getInstallmentsSet()).size() != 1) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.only.one.customer.allowed");
        }

        // Ensure all debit entries and installments are from the same debt account
        if (Stream.concat(getDebitEntriesSet().stream().map(DebitEntry::getDebtAccount),
                getInstallmentsSet().stream().map(i -> i.getPaymentPlan().getDebtAccount())).distinct().count() != 1) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.debitEntries.from.different.debt.accounts");
        }

        FinantialEntity finantialEntity = getMbwayMandate().getDigitalPaymentPlatform().getFinantialEntity();

        // Ensure all debit entries and installments are from the same financial entity
        Set<FinantialEntity> finantialEntitySet = Stream.concat(getDebitEntriesSet().stream().map(DebitEntry::getFinantialEntity),
                getInstallmentsSet().stream().map(i -> i.getPaymentPlan().getFinantialEntity())).collect(Collectors.toSet());
        if (finantialEntitySet.size() != 1 || finantialEntitySet.iterator().next() != finantialEntity) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.debitEntries.from.different.finantial.entities");
        }

    }

    public boolean isAnnulled() {
        return getState() == MbwayMandatePaymentScheduleState.ANNULLED;
    }

    public void editEmail(LocalizedString emailSubject, LocalizedString emailBody) {
        setEmailSubject(emailSubject);
        setEmailBody(emailBody);
    }

    public void sendEmail() {

        setState(MbwayMandatePaymentScheduleState.EMAIL_SENT);
    }

    public void annul() {
        super.setState(MbwayMandatePaymentScheduleState.ANNULLED);
    }

    @Atomic(mode = Atomic.TxMode.READ)
    public MbwayRequest chargePayment() {
        // TODO Only charge debts that are not paid

        Set<DebitEntry> debitEntriesToCharge =
                getDebitEntriesSet().stream().filter(DebitEntry::isInDebt).collect(Collectors.toSet());
        Set<Installment> installmentsToCharge =
                getInstallmentsSet().stream().filter(i -> !i.isPaid()).collect(Collectors.toSet());

        if (debitEntriesToCharge.isEmpty() && installmentsToCharge.isEmpty()) {
            throw new IllegalStateException("nothing to charge in this schedule");
        }

        try {
            MbwayRequest mbwayRequest = getMbwayMandate().getDigitalPaymentPlatform().castToMbwayPaymentPlatformService()
                    .createMbwayRequest(this, getDebitEntriesSet(), getInstallmentsSet());

            updateStateToPaymentCharged();

            return mbwayRequest;
        } catch(TreasuryDomainException e) {
            // And error occurred, mark this request with error, so the operator can see it
            updateStateToError();

            throw e;
        }


    }

    @Atomic
    private void updateStateToPaymentCharged() {
        setState(MbwayMandatePaymentScheduleState.PAYMENT_CHARGED);
    }

    @Atomic
    private void updateStateToError() {
        setState(MbwayMandatePaymentScheduleState.ERROR);
    }


    /*
     * SERVICES
     */
    public static MbwayMandatePaymentSchedule create(MbwayMandate mbwayMandate, DateTime sendEmailDate,
            DateTime paymentChargeDate, Set<DebitEntry> debitEntriesSet, Set<Installment> installmentSet) {
        return new MbwayMandatePaymentSchedule(mbwayMandate, sendEmailDate, paymentChargeDate, debitEntriesSet, installmentSet);
    }

    public static Stream<MbwayMandatePaymentSchedule> find() {
        return FenixFramework.getDomainRoot().getMbwayMandatePaymentSchedulesSet().stream();
    }

    public static Stream<MbwayMandatePaymentSchedule> findActiveSchedulesForDebitEntry(DebitEntry debitEntry) {
        return debitEntry.getMbwayMandatePaymentScheduleSet().stream().filter(s -> !s.isAnnulled());
    }

    public static Stream<MbwayMandatePaymentSchedule> findActiveSchedulesForInstallment(Installment installment) {
        return installment.getMbwayMandatePaymentScheduleSet().stream().filter(s -> !s.isAnnulled());
    }

    public static Stream<MbwayMandatePaymentSchedule> findActive() {
        return find().filter(s -> !s.isAnnulled());
    }

}
