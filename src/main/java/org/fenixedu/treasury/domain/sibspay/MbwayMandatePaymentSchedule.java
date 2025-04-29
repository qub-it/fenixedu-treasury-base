package org.fenixedu.treasury.domain.sibspay;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.IMbwayPaymentPlatformService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MbwayMandatePaymentSchedule extends MbwayMandatePaymentSchedule_Base {

    public MbwayMandatePaymentSchedule() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setState(MbwayMandatePaymentScheduleState.SCHEDULED);
        setUpdateStateDate(new DateTime());
    }

    public MbwayMandatePaymentSchedule(MbwayMandate mbwayMandate, LocalDate sendNotificationDate, LocalDate paymentChargeDate,
            Set<DebitEntry> debitEntriesSet, Set<Installment> installmentSet) {
        this();

        setMbwayMandate(mbwayMandate);
        setSendNotificationDate(sendNotificationDate);
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
        if (Stream.concat(Stream.concat(getDebitEntriesSet().stream().map(DebitEntry::getDebtAccount),
                        getInstallmentsSet().stream().map(i -> i.getPaymentPlan().getDebtAccount())),
                Stream.of(getMbwayMandate().getDebtAccount())).distinct().count() != 1) {
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

    public BigDecimal getOpenAmount() {
        BigDecimal result =
                getDebitEntriesSet().stream().map(de -> de.getOpenAmountWithInterests()).reduce(BigDecimal.ZERO, BigDecimal::add);

        result = result.add(getInstallmentsSet().stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));

        return result;
    }

    public boolean isInProgress() {
        return getState().isScheduled() || getState().isNotificationSent();
    }

    public boolean isCanceled() {
        return getState().isCanceled();
    }

    public void sendNotification() {
        // Make logic to send notification

        setState(MbwayMandatePaymentScheduleState.NOTIFICATION_SENT);
        setUpdateStateDate(new DateTime());

        checkRules();
    }

    public void annul(String reason) {
        super.setState(MbwayMandatePaymentScheduleState.CANCELED);
        setUpdateStateDate(new DateTime());

        setCancelReason(reason);
        setCancelDate(new DateTime());
        setCancelResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());

        checkRules();
    }

    public void reschedule() {
        if (!getState().isError()) {
            throw new IllegalStateException("error.MbwayMandatePaymentSchedule.reschedule.not.error.state");
        }

        getDebitEntriesSet().forEach(d -> {
            if (findSchedulesInProgressForDebitEntry(d).count() > 0) {
                throw new TreasuryDomainException("error.MbwayMandatePaymentSchedule.debitEntry.already.has.schedule.in.progress",
                        d.getDescription());
            }
        });

        getInstallmentsSet().stream().forEach(i -> {
            if (findSchedulesInProgressForInstallment(i).count() > 0) {
                throw new TreasuryDomainException(
                        "error.MbwayMandatePaymentSchedule.installment.already.has.schedule.in.progress",
                        i.getDescription().getContent());
            }
        });

        IMbwayPaymentPlatformService service = getMbwayMandate().getDigitalPaymentPlatform().castToMbwayPaymentPlatformService();

        if (!service.isMbwayAuthorizedPaymentsActive()) {
            throw new TreasuryDomainException("error.MbwayMandate.create.platform.not.active");
        }

        super.setState(MbwayMandatePaymentScheduleState.SCHEDULED);
        setUpdateStateDate(new DateTime());

        checkRules();
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
        } catch (TreasuryDomainException e) {
            // And error occurred, mark this request with error, so the operator can see it
            updateStateToError();

            throw e;
        }
    }

    @Atomic
    private void updateStateToPaymentCharged() {
        setState(MbwayMandatePaymentScheduleState.PAYMENT_CHARGED);
        setUpdateStateDate(new DateTime());
    }

    @Atomic
    private void updateStateToError() {
        setState(MbwayMandatePaymentScheduleState.ERROR);
        setUpdateStateDate(new DateTime());
    }

    /*
     * SERVICES
     */
    public static MbwayMandatePaymentSchedule create(MbwayMandate mbwayMandate, LocalDate sendEmailDate,
            LocalDate paymentChargeDate, Set<DebitEntry> debitEntriesSet, Set<Installment> installmentSet) {

        debitEntriesSet.stream().forEach(d -> {
            if (findSchedulesInProgressForDebitEntry(d).count() > 0) {
                throw new TreasuryDomainException("error.MbwayMandatePaymentSchedule.debitEntry.already.has.schedule.in.progress",
                        d.getDescription());
            }
        });

        installmentSet.stream().forEach(i -> {
            if (findSchedulesInProgressForInstallment(i).count() > 0) {
                throw new TreasuryDomainException(
                        "error.MbwayMandatePaymentSchedule.installment.already.has.schedule.in.progress",
                        i.getDescription().getContent());
            }
        });

        IMbwayPaymentPlatformService service = mbwayMandate.getDigitalPaymentPlatform().castToMbwayPaymentPlatformService();

        if (!service.isMbwayAuthorizedPaymentsActive()) {
            throw new TreasuryDomainException("error.MbwayMandate.create.platform.not.active");
        }

        return new MbwayMandatePaymentSchedule(mbwayMandate, sendEmailDate, paymentChargeDate, debitEntriesSet, installmentSet);
    }

    public static Stream<MbwayMandatePaymentSchedule> findAll() {
        return FenixFramework.getDomainRoot().getMbwayMandatePaymentSchedulesSet().stream();
    }

    public static Stream<MbwayMandatePaymentSchedule> findSchedulesInProgressForDebitEntry(DebitEntry debitEntry) {
        return debitEntry.getMbwayMandatePaymentScheduleSet().stream().filter(s -> s.isInProgress());
    }

    public static Stream<MbwayMandatePaymentSchedule> findSchedulesInProgressForInstallment(Installment installment) {
        return installment.getMbwayMandatePaymentScheduleSet().stream().filter(s -> s.isInProgress());
    }

    public static Stream<MbwayMandatePaymentSchedule> findActive() {
        return findAll().filter(s -> !s.isCanceled());
    }

    public void transferScheduleToOtherDebtAccount(MbwayMandate newMandate, Map<DebitEntry, DebitEntry> debitEntriesTransferMap,
            Map<Installment, Installment> installmentsTransferMap) {
        Set<DebitEntry> newDebitEntriesSet = getDebitEntriesSet().stream() //
                .filter(d -> debitEntriesTransferMap.get(d) != null) //
                .filter(d -> debitEntriesTransferMap.get(d).isInDebt()) //
                .map(d -> debitEntriesTransferMap.get(d)) //
                .collect(Collectors.toSet());

        Set<Installment> newInstallmentsSet = getInstallmentsSet().stream() //
                .filter(i -> installmentsTransferMap.get(i) != null) //
                .filter(i -> TreasuryConstants.isPositive(installmentsTransferMap.get(i).getOpenAmount()))
                .map(i -> installmentsTransferMap.get(i)) //
                .collect(Collectors.toSet());

        MbwayMandatePaymentScheduleState originalState = getState();
        DateTime originalUpdateStateDate = getUpdateStateDate();

        setState(MbwayMandatePaymentScheduleState.TRANSFERRED);
        setUpdateStateDate(new DateTime());

        if (!newDebitEntriesSet.isEmpty() || !newInstallmentsSet.isEmpty()) {
            MbwayMandatePaymentSchedule newSchedule =
                    new MbwayMandatePaymentSchedule(newMandate, getSendNotificationDate(), getPaymentChargeDate(),
                            newDebitEntriesSet, newInstallmentsSet);

            newSchedule.setState(originalState);
            newSchedule.setUpdateStateDate(originalUpdateStateDate);
            newSchedule.setCreationDate(getCreationDate());
            newSchedule.setSendNotificationDate(getSendNotificationDate());
            newSchedule.setPaymentChargeDate(getPaymentChargeDate());

            newSchedule.checkRules();
        }

        checkRules();
    }

}
