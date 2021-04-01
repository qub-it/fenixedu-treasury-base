package org.fenixedu.treasury.domain.payments.integration;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.FenixFramework;

public class DigitalPaymentPlatformPaymentMode extends DigitalPaymentPlatformPaymentMode_Base {

    public DigitalPaymentPlatformPaymentMode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public DigitalPaymentPlatformPaymentMode(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        this();

        setDigitalPaymentPlatform(platform);
        setPaymentMethod(paymentMethod);
        setActive(true);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.domainRoot.required");
        }

        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.paymentMethod.required");
        }

        //In the future this validation can be removed if is intended to use the same paymentMethod for the various paymentModes available by the platform. eg: creditCard
        if (find(getDigitalPaymentPlatform(), getPaymentMethod()).count() > 1) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.duplicated");
        }
    }

    public boolean isDigitalPaymentPlatformAndPaymentModeActive() {
        return getDigitalPaymentPlatform().isActive() && isActive();
    }

    public boolean isActive() {
        return getActive();
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public LocalizedString getDigitalPaymentPlatformPaymentMethodDesignation() {
        LocalizedString result = new LocalizedString();
        for (Locale locale : CoreConfiguration.supportedLocales()) {
            result.with(locale, String.format("[%s] %s", getDigitalPaymentPlatform().getName(),
                    getPaymentMethod().getName().getContent(locale)));
        }

        return result;
    }

    public void delete() {
        super.setDomainRoot(null);
        super.setDigitalPaymentPlatform(null);
        super.setPaymentMethod(null);

        super.deleteDomainObject();
    }

    /*
     * SERVICES
     */

    public static Stream<DigitalPaymentPlatformPaymentMode> findAll() {
        return FenixFramework.getDomainRoot().getDigitalPaymentPlatformPaymentModesSet().stream();
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive(
            FinantialInstitution finantialInstitution) {
        return findAll().filter(pm -> pm.getDigitalPaymentPlatform().getFinantialInstitution() == finantialInstitution)
                .filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive());
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive() {
        return findAll().filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive());
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> find(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        return platform.getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.getPaymentMethod() == paymentMethod);
    }

    public static Optional<DigitalPaymentPlatformPaymentMode> findUnique(DigitalPaymentPlatform platform,
            PaymentMethod paymentMethod) {
        return find(platform, paymentMethod).findAny();
    }

    public static DigitalPaymentPlatformPaymentMode create(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        return new DigitalPaymentPlatformPaymentMode(platform, paymentMethod);
    }

}