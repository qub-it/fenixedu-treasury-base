package org.fenixedu.treasury.domain.sibspay;

import java.util.Collections;
import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class MbwayMandatePaymentSchedule extends MbwayMandatePaymentSchedule_Base {

    public MbwayMandatePaymentSchedule() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    public MbwayMandatePaymentSchedule(MbwayMandate mbwayMandate, DateTime sendEmailDate, DateTime paymentChargeDate,
            Set<DebitEntry> debitEntriesSet) {
        this();

        setMbwayMandate(mbwayMandate);
        setSendEmailDate(sendEmailDate);
        setPaymentChargeDate(paymentChargeDate);

        getDebitEntriesSet().addAll(debitEntriesSet);
    }

    public void editEmail(LocalizedString emailSubject, LocalizedString emailBody) {
        setEmailSubject(emailSubject);
        setEmailBody(emailBody);
    }

    public void sendEmail() {
    }

    public MbwayRequest chargePayment() {
        MbwayRequest mbwayRequest = getMbwayMandate().getDigitalPaymentPlatform().castToMbwayPaymentPlatformService()
                .createMbwayRequest(this, getDebitEntriesSet(), Collections.emptySet());

        return mbwayRequest;
    }

    public static MbwayMandatePaymentSchedule create(MbwayMandate mbwayMandate, DateTime sendEmailDate,
            DateTime paymentChargeDate, Set<DebitEntry> debitEntriesSet) {
        return new MbwayMandatePaymentSchedule(mbwayMandate, sendEmailDate, paymentChargeDate, debitEntriesSet);
    }

}
