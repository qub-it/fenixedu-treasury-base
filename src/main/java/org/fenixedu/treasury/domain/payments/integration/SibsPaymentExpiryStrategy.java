package org.fenixedu.treasury.domain.payments.integration;

import java.util.Set;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public abstract class SibsPaymentExpiryStrategy extends SibsPaymentExpiryStrategy_Base {

    public SibsPaymentExpiryStrategy() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setNewModeApplied(false);
    }

    protected void init(DigitalPaymentPlatform digitalPaymentPlatform) {
        setDigitalPaymentPlatform(digitalPaymentPlatform);
    }

    protected void checkRules() {
        if (getDomainRoot() == null) {
            throw new IllegalStateException("error.SibsPaymentExpiryStrategy.domainRoot.required");
        }

        if (getDigitalPaymentPlatform() == null) {
            throw new IllegalStateException("error.SibsPaymentExpiryStrategy.digitalPaymentPlatform.required");
        }
    }

    public abstract LocalDate calculateSibsPaymentRequestExpiryDate(Set<DebitEntry> debitEntries, Set<Installment> installments,
            boolean limitSibsPaymentRequestToCustomDueDate, LocalDate customSibsPaymentRequestDueDate);

}
