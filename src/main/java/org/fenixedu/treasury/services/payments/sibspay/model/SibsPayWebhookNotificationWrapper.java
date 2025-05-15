package org.fenixedu.treasury.services.payments.sibspay.model;

import java.math.BigDecimal;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

public class SibsPayWebhookNotificationWrapper implements DigitalPlatformResultBean {

    private SibsPayWebhookNotification webhookNotification;

    public SibsPayWebhookNotificationWrapper(SibsPayWebhookNotification webhookNotification) {
        this.webhookNotification = webhookNotification;
    }

    // ANIL 2024-11-14  (#qubIT-Fenix-6095)
    //
    // If the notification is paid and the data of structure paymentReference is present,
    // check if the paymentReference status is "PAID" . If it is not throw an exception
    // and analyse why it is not
    public void checkIfNotificationIsPaidAndPaymentReferenceIsAlsoInPaidStatus() {
        if (isPaid() && this.webhookNotification.getPaymentReference() != null && !"PAID".equals(
                this.webhookNotification.getPaymentReference().getStatus())) {
            throw new RuntimeException("the notification is paid but the status of PaymentReference is not 'PAID'");
        }
    }

    @Override
    public BigDecimal getAmount() {
        return this.webhookNotification.getAmount().getValue();
    }

    @Override
    public String getMerchantTransactionId() {
        if (this.webhookNotification.getMerchant() != null) {
            return this.webhookNotification.getMerchant().getTransactionId();
        }

        return null;
    }

    @Override
    public String getPaymentBrand() {
        return this.webhookNotification.getPaymentMethod();
    }

    @Override
    public DateTime getPaymentDate() {
        // ANIL 2024-06-14 #UCP-FENIXEDU-93
        //
        // The transactionDateTime, if present, is in UTC, which is fine when
        // the timezone for Lisbon in winter.
        // But in the summer, the Lisbon timezone is UTC+1, which becomes
        // a issue when converting DateTime to LocalDate .
        //
        // For example, when timezone is UTC+1 and the dateTime is in UTC like
        // '2024-06-12 23:15:00Z', the dateTime in UTC+1 is 
        // '2024-06-13 00:15:00.000+0001'
        //
        // But with this example in UTC, getting DateTime#toLocalDate()
        // returns '2024-06-12', instead of '2024-06-13'
        // 
        // This conversion has issues, when calculating the interests amount
        // which is dependent in the payment date

        // To workaround this, if the transactionDateTime is different than null
        // calculate the offset between the server timezone and the timezone
        // returned by transactionDateTime, which with SIBS Pay is in UTC timezone

        // This workaround was given by https://duckduckgo.com/aichat using GPT-3.5

        if (this.webhookNotification.getTransactionDateTime() == null) {
            // Return null
            return this.webhookNotification.getTransactionDateTime();
        }

        DateTime transactionDateTimeWithSibsPayTimezone = this.webhookNotification.getTransactionDateTime();

        DateTimeZone serverTimezone = DateTimeZone.getDefault();

        int serverOffsetMillis = serverTimezone.getOffset(transactionDateTimeWithSibsPayTimezone);

        // Convert the DateTime to the server's timezone dynamically
        DateTime dateTimeServerTimezone =
                transactionDateTimeWithSibsPayTimezone.withZone(DateTimeZone.forOffsetMillis(serverOffsetMillis));

        return dateTimeServerTimezone;
    }

    @Override
    public String getPaymentResultCode() {
        return this.webhookNotification.getPaymentStatus();
    }

    @Override
    public String getPaymentResultDescription() {
        return null;
    }

    @Override
    public String getPaymentType() {
        return this.webhookNotification.getPaymentMethod();
    }

    @Override
    public String getTimestamp() {
        return null;
    }

    @Override
    public String getTransactionId() {
        return this.webhookNotification.getTransactionID();
    }

    public String getOperationStatusCode() {
        return this.webhookNotification.getReturnStatus().getStatusCode();
    }

    public String getOperationStatusMessage() {
        return this.webhookNotification.getReturnStatus().getStatusMsg();
    }

    public String getOperationStatusDescription() {
        return this.webhookNotification.getReturnStatus().getStatusDescription();
    }

    public String getNotificationID() {
        return this.webhookNotification.getNotificationID();
    }

    @Override
    public boolean isOperationSuccess() {
        return SibsPayAPIService.isOperationSuccess(this.webhookNotification.getReturnStatus().getStatusCode());
    }

    @Override
    public boolean isPaid() {
        // ANIL 2024-11-15 (#qubIT-Fenix-6101)
        //
        // The notification of annulment of SIBS payment requests are sent with the 
        // paymentStatus or #getPaymentResultCode() equal to "Success" .
        //
        // This is misleading as these transactions are not successful payments but
        // successful annulments. The payments were not being registered because the
        // transactionIds of annulement notifications are different from what is registered 
        // within the paymentRequest
        //
        // Also in the web controller SibsPayWebhookController, if the payment request
        // was not in creation or request state, it did nothing and returned OK to the
        // payment platform
        //
        // Consider only PURS and PREF as successful payments
        if (!SibsPayAPIService.isPaymentTypePurs(
                this.webhookNotification.getPaymentType()) && !SibsPayAPIService.isPaymentTypePref(
                this.webhookNotification.getPaymentType())) {
            return false;
        }

        return SibsPayAPIService.isPaid(this.getPaymentResultCode());
    }

    public boolean isPending() {
        return SibsPayAPIService.isPending(this.getPaymentResultCode());
    }

    public boolean isExpired() {
        return SibsPayAPIService.isExpired(this.getPaymentResultCode());
    }

    public boolean isDeclined() {
        return SibsPayAPIService.isDeclined(this.getPaymentResultCode());
    }

    public boolean isAuthorizationAccepted() {
        return SibsPayAPIService.isAuthorizationAccepted(this.getPaymentResultCode());
    }

    public boolean isSibsPayPaymentTypeAuth() {
        return SibsPayAPIService.isPaymentTypeAuth(this.webhookNotification.getPaymentType());
    }

    public String getMandateId() {
        if (this.webhookNotification.getMbwayMandate() != null) {
            return this.webhookNotification.getMbwayMandate().getMandateIdentification();
        }

        return null;
    }

    public boolean IsMandateActionAuthCreation() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionAuthCreation(
                this.webhookNotification.getMbwayMandate().getMandateAction());
    }

    public boolean isMandateActionAuthSuspension() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionAuthSuspension(
                this.webhookNotification.getMbwayMandate().getMandateAction());
    }

    public boolean isMandateActionAuthReactivation() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionAuthReactivation(
                this.webhookNotification.getMbwayMandate().getMandateAction());
    }

    public boolean isMandateActionAuthLimitsUpdate() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionAuthLimitsUpdate(
                this.webhookNotification.getMbwayMandate().getMandateAction());
    }

    public boolean isMandateActionAuthCancel() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionAuthCancel(
                this.webhookNotification.getMbwayMandate().getMandateAction());
    }

    public boolean IsMandateActionStatusSuccess() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionStatusSuccess(
                this.webhookNotification.getMbwayMandate().getMandateActionStatus());
    }

    public boolean isMandateActionStatusRejected() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionStatusRejected(
                this.webhookNotification.getMbwayMandate().getMandateActionStatus());
    }

    public boolean isMandateActionStatusRefused() {
        return this.webhookNotification.getMbwayMandate() != null && SibsPayAPIService.isMandateActionStatusRefused(
                this.webhookNotification.getMbwayMandate().getMandateActionStatus());
    }

    public BigDecimal getPlafond() {
        if (this.webhookNotification.getMbwayMandate() == null) {
            return null;
        }

        if (this.webhookNotification.getMbwayMandate().getMandateAmountLimit() == null) {
            return null;
        }

        return this.webhookNotification.getMbwayMandate().getMandateAmountLimit().getValue();
    }

    public LocalDate getAuthorizationExpirationDate() {
        if (this.webhookNotification.getMbwayMandate() == null) {
            return null;
        }

        if (this.webhookNotification.getMbwayMandate().getMandateExpirationDate() == null) {
            return null;
        }

        String value = this.webhookNotification.getMbwayMandate().getMandateExpirationDate();

        if (value.length() > 10) {
            value = value.substring(0, 10);
        }

        return TreasuryConstants.parseLocalDate(value, "yyyy-MM-dd");
    }

}
