package org.fenixedu.treasury.domain.sibspay;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

public class MbwayMandate extends MbwayMandate_Base {

    public MbwayMandate() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setState(MbwayMandateState.CREATED);
        setRequestDate(new DateTime());
    }

    protected MbwayMandate(DigitalPaymentPlatform digitalPaymentPlatform, DebtAccount debtAccount, String merchantTransactionId,
            String countryPrefix, String localPhoneNumber) {
        this();

        setDigitalPaymentPlatform(digitalPaymentPlatform);
        setDebtAccount(debtAccount);
        setMerchantTransactionId(merchantTransactionId);
        setCountryPrefix(countryPrefix);
        setLocalPhoneNumber(localPhoneNumber);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.MbwayMandate.domainRoot.required");
        }

        if (getDigitalPaymentPlatform() == null) {
            throw new TreasuryDomainException("error.MbwayMandate.digitalPaymentPlatform.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.MbwayMandate.debtAccount.required");
        }

        if (getState() == null) {
            throw new TreasuryDomainException("error.MbwayMandate.state.required");
        }

        if (StringUtils.isEmpty(getCountryPrefix())) {
            throw new TreasuryDomainException("error.MbwayMandate.countryPrefix.required");
        }

        if (StringUtils.isEmpty(getLocalPhoneNumber())) {
            throw new TreasuryDomainException("error.MbwayMandate.localPhoneNumber.required");
        }

        if (getState().isWaitingAuthorization() || getState().isSuspended()) {
            if (StringUtils.isEmpty(getMandateId())) {
                throw new TreasuryDomainException("error.MbwayMandate.mandateId.required");
            }

            if (StringUtils.isEmpty(getTransactionId())) {
                throw new TreasuryDomainException("error.MbwayMandate.transactionId.required");
            }
        }
    }

    public boolean isMandateProcessActiveInPaymentPlatform() {
        return getState().isCreated() || getState().isActive() || getState().isSuspended();
    }

    public void waitAuthorization(String mandateId, String transactionId) {
        setUpdateDate(new DateTime());
        setState(MbwayMandateState.WAITING_AUTHORIZATION);

        setMandateId(mandateId);
        setTransactionId(transactionId);

        checkRules();
    }

    public void authorize() {
        setUpdateDate(new DateTime());
        setState(MbwayMandateState.ACTIVE);
        setAuthorizationDate(new DateTime());

        checkRules();
    }

    public void reactivate() {
        setUpdateDate(new DateTime());
        if (!getState().isSuspended()) {
            throw new IllegalStateException("mandate should in suspended state, in order to be to be reactivated");
        }

        setState(MbwayMandateState.ACTIVE);

        checkRules();
    }

    public void markAsNotAuthorized(String reason) {
        setUpdateDate(new DateTime());
        setState(MbwayMandateState.NOT_AUTHORIZED);
        setAuthorizationDate(new DateTime());

        setCancelReason(reason);
        setCancelResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());

        checkRules();
    }

    public void cancel(String reason) {
        if(getState().isCanceled()) {
            throw new IllegalStateException("error.MbwayMandate.cancel.already.canceled");
        }

        setUpdateDate(new DateTime());
        setState(MbwayMandateState.CANCELED);

        setCancelReason(reason);
        setCancelResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());

        checkRules();
    }

    public void expire() {
        setUpdateDate(new DateTime());
        setState(MbwayMandateState.EXPIRED);

        checkRules();
    }

    public void suspend() {
        setUpdateDate(new DateTime());
        setState(MbwayMandateState.SUSPENDED);

        checkRules();
    }

    public void updatePlafondAndExpirationDate(BigDecimal newPlafond, LocalDate expirationDate) {
        setUpdateDate(new DateTime());

        setPlafond(newPlafond);
        setExpirationDate(expirationDate);

        checkRules();
    }

    public void scheduleForCancellationInPlatform() {
        setPendingMbwayMandateCancellationPlatform(getDigitalPaymentPlatform());
    }

    public void clearScheduledCancellationInPlatform() {
        setPendingMbwayMandateCancellationPlatform(null);
    }

    /*
     * ********
     * SERVICES
     * ********
     */

    public static MbwayMandate create(DigitalPaymentPlatform digitalPaymentPlatform, DebtAccount debtAccount,
            String merchantTransactionId, String countryPrefix, String localPhoneNumber) {
        if (findAllMandatesActiveInPaymentPlatform(debtAccount.getCustomer(),
                digitalPaymentPlatform.getFinantialInstitution()).count() > 0) {
            throw new TreasuryDomainException("error.MbwayMandate.create.customer.already.has.mandate.alive");
        }

        if(!digitalPaymentPlatform.castToMbwayPaymentPlatformService().isMbwayAuthorizedPaymentsActive()) {
            throw new TreasuryDomainException("error.MbwayMandate.create.platform.not.active");
        }

        return new MbwayMandate(digitalPaymentPlatform, debtAccount, merchantTransactionId, countryPrefix, localPhoneNumber);
    }

    public static Stream<MbwayMandate> findAll() {
        return FenixFramework.getDomainRoot().getMbwayMandatesSet().stream();
    }

    public static Stream<MbwayMandate> find(DebtAccount debtAccount) {
        return debtAccount.getMbwayMandatesSet().stream();
    }

    public static Stream<MbwayMandate> findAllFromCustomer(Customer customer, FinantialInstitution finantialInstitution) {
        return customer.getAllCustomers().stream().map(c -> c.getDebtAccountFor(finantialInstitution))
                .flatMap(d -> d.getMbwayMandatesSet().stream());
    }

    public static Stream<MbwayMandate> findAllMandatesActiveInPaymentPlatform(Customer customer,
            FinantialInstitution finantialInstitution) {
        return findAllFromCustomer(customer, finantialInstitution).filter(m -> m.isMandateProcessActiveInPaymentPlatform());
    }

    public static Optional<MbwayMandate> findUniqueByMandateId(String mandateId) {
        return FenixFramework.getDomainRoot().getMbwayMandatesSet().stream().filter(m -> mandateId.equals(m.getMandateId()))
                .findFirst();
    }

}
