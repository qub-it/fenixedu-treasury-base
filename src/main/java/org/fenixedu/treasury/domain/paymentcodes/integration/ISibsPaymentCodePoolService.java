package org.fenixedu.treasury.domain.paymentcodes.integration;

import java.util.Set;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.joda.time.LocalDate;

public interface ISibsPaymentCodePoolService {

    public String getName();

    public String getExternalId();

    public String getEntityReferenceCode();

    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments);

    public SibsPaymentRequest createSibsPaymentRequestWithInterests(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate interestsCalculationDate);
}