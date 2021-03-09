package org.fenixedu.treasury.domain.payments.integration;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

public abstract class DigitalPaymentPlatform extends DigitalPaymentPlatform_Base {

    public static final Comparator<DigitalPaymentPlatform> COMPARE_BY_NAME = (o1, o2) -> 
        o1.getName().compareTo(o2.getName()) * 10 + o1.getExternalId().compareTo(o2.getExternalId());
    
    protected DigitalPaymentPlatform() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(FinantialInstitution finantialInstitution, String name, boolean active) {
        setFinantialInstitution(finantialInstitution);
        setName(name);
        setActive(active);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.domainRoot.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.finantialInstitution.required");
        }

        if (StringUtils.isEmpty(getName())) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.name.required");
        }
    }

    public boolean isSibsPaymentCodeServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getMbPaymentMethod());
    }

    public boolean isForwardPaymentServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getCreditCardPaymentMethod());
    }

    public boolean isMbwayServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getMbWayPaymentMethod());
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isActive(PaymentMethod paymentMethod) {
        Optional<DigitalPaymentPlatformPaymentMode> optional =
                getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.getPaymentMethod() == paymentMethod).findAny();

        return isActive() && optional.isPresent() && optional.get().isActive();
    }

    public ISibsPaymentCodePoolService castToSibsPaymentCodePoolService() {
        return (ISibsPaymentCodePoolService) this;
    }

    public IForwardPaymentPlatformService castToForwardPaymentPlatformService() {
        return (IForwardPaymentPlatformService) this;
    }

    public Set<? extends PaymentRequest> getAssociatedPaymentRequestsSet() {
        return super.getPaymentRequestsSet();
    }

    public Set<DigitalPaymentPlatformPaymentMode> getActiveDigitalPaymentModesSet() {
        return getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.isActive()).collect(Collectors.toSet());
    }

    public void delete() {
        if (!super.getPaymentRequestsSet().isEmpty()) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.cannot.delete.due.to.requests");
        }

        super.setDomainRoot(null);
        super.setFinantialInstitution(null);

        while (!getDigitalPaymentPlatformPaymentModesSet().isEmpty()) {
            getDigitalPaymentPlatformPaymentModesSet().iterator().next().delete();
        }
    }

    public PaymentRequestLog log(PaymentRequest paymentRequest, String statusCode, String statusMessage, String requestBody,
            String responseBody) {
        final PaymentRequestLog log = log(paymentRequest);

        log.setStatusCode(statusCode);
        log.setStatusMessage(statusMessage);

        if (!Strings.isNullOrEmpty(requestBody)) {
            log.saveRequest(requestBody);
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            log.saveResponse(responseBody);
        }

        return log;
    }

    /*
    public PaymentRequestLog logException(PaymentRequest paymentRequest, Exception e) {
        PaymentRequestLog log = log(paymentRequest);
        log.logException(e);

        return log;
    }
    */

    public PaymentRequestLog log(PaymentRequest paymentRequest) {
        return PaymentRequestLog.create(paymentRequest, paymentRequest.getCurrentState().getCode(),
                paymentRequest.getCurrentState().getLocalizedName());
    }

    // @formatter:off
    /*
     * --------
     * SERVICES
     * --------
     */
    // @formatter:on

    public static Stream<? extends DigitalPaymentPlatform> findAll() {
        return FenixFramework.getDomainRoot().getDigitalPaymentPlatformsSet().stream();
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeService() {
        return findAll().filter(d -> d.isSibsPaymentCodeServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> find(FinantialInstitution finantialInstitution) {
        return finantialInstitution.getDigitalPaymentPlatformsSet().stream();
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeService(
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(d -> d.isSibsPaymentCodeServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeServiceByActive(
            FinantialInstitution finantialInstitution, boolean active) {
        PaymentMethod mbPaymentMethod = TreasurySettings.getInstance().getMbPaymentMethod();
        return find(finantialInstitution).filter(d -> d.isSibsPaymentCodeServiceSupported())
                .filter(d -> active == d.isActive(mbPaymentMethod));
    }

    public static Stream<? extends DigitalPaymentPlatform> findForForwardPaymentService(
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(d -> d.isForwardPaymentServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> findForForwardPaymentService(FinantialInstitution finantialInstitution,
            boolean active) {
        PaymentMethod creditCardPaymentMethod = TreasurySettings.getInstance().getCreditCardPaymentMethod();
        return find(finantialInstitution).filter(d -> d.isForwardPaymentServiceSupported())
                .filter(d -> active == d.isActive(creditCardPaymentMethod));
    }
    
    public static Stream<? extends DigitalPaymentPlatform> find(FinantialInstitution finantialInstitution,
            PaymentMethod paymentMethod, boolean active) {
        return find(finantialInstitution).filter(d -> active == d.isActive(paymentMethod));
    }

}
