/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.domain.sibspaymentsgateway;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

public class SibsPaymentsGatewayLog extends SibsPaymentsGatewayLog_Base {

    public static final String REQUEST_PAYMENT_CODE = "sibsMbPaymentRequest";
    public static final String MBWAY_REQUEST_PAYMENT = "mbwayPaymentRequest";
    public static final String WEBHOOK_NOTIFICATION = "WEBHOOK_NOTIFICATION";

    public static final String OCTECT_STREAM_CONTENT_TYPE = "application/octet-stream";

    public SibsPaymentsGatewayLog() {
        super();
    }

    protected SibsPaymentsGatewayLog(String operationCode) {
        this();

        setOperationCode(operationCode);

        checkRules();
    }

    protected SibsPaymentsGatewayLog(String operationCode, String sibsGatewayMerchantTransactionId) {
        this();

        setOperationCode(operationCode);
        setSibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId);

        checkRules();
    }

    private void checkRules() {

    }

    public boolean isExceptionOccured() {
        return super.getExceptionOccured();
    }

    public boolean isOperationSuccess() {
        return super.getOperationSuccess();
    }

    public void savePaymentInfo(BigDecimal amount, DateTime paymentDate) {
        setAmount(amount);
        setPaymentDate(paymentDate);
    }

    public void savePaymentTypeAndBrand(String paymentType, String paymentBrand) {
        setPaymentType(paymentType);
        setPaymentBrand(paymentBrand);
    }

    public void markAsDuplicatedTransaction() {
        setSibsTransactionDuplicated(true);
    }

    public void logRequestSendDate() {
        setRequestSendDate(new DateTime());
    }

    public void logRequestReceiveDateAndData(String transactionId, boolean operationSuccess, boolean transactionPaid,
            String operationResultCode, String operationResultDescription) {
        setRequestReceiveDate(new DateTime());
        setSibsGatewayTransactionId(transactionId);
        setOperationSuccess(operationSuccess);
        setTransactionWithPayment(transactionPaid);
        setStatusCode(operationResultCode);
        setStatusMessage(operationResultDescription);
    }

    public void saveWebhookNotificationData(String notificationInitializationVector, String notificationAuthenticationTag,
            String notificationEncryptedPayload) {
        final ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();

        setNotificationInitializationVector(notificationInitializationVector);
        setNotificationAuthTag(notificationAuthenticationTag);

        if (notificationEncryptedPayload != null) {
            final String notificationEncryptedPayloadFileId = implementation.createFile(
                    String.format("sibsOnlinePaymentsGatewayLog-notificationEncryptedPayload-%s.txt", getExternalId()),
                    OCTECT_STREAM_CONTENT_TYPE, notificationEncryptedPayload.getBytes());

            setNotificationEncryptedPayloadFileId(notificationEncryptedPayloadFileId);
        }
    }

    public void saveMerchantTransactionId(String merchantTransactionId) {
        setSibsGatewayMerchantTransactionId(merchantTransactionId);
    }
    
    public void saveTransactionId(String transactionId) {
        setSibsGatewayTransactionId(transactionId);
    }
    
    public void saveReferenceId(String referenceId) {
        setSibsGatewayReferenceId(referenceId);
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentsGatewayLog> findAll() {
        return PaymentRequestLog.findAll().filter(p -> p instanceof SibsPaymentsGatewayLog).map(SibsPaymentsGatewayLog.class::cast);
    }
    
    public static SibsPaymentsGatewayLog createForSibsPaymentRequest(String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog(REQUEST_PAYMENT_CODE, sibsGatewayMerchantTransactionId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());
        
        return log;
    }

    public static SibsPaymentsGatewayLog createForMbwayPaymentRequest(String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog(MBWAY_REQUEST_PAYMENT, sibsGatewayMerchantTransactionId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        return log;
    }

    public static SibsPaymentsGatewayLog createLogForWebhookNotification() {
        return new SibsPaymentsGatewayLog(WEBHOOK_NOTIFICATION);
    }

    public static SibsPaymentsGatewayLog create(PaymentRequest paymentRequest, String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog();
        
        log.setPaymentRequest(paymentRequest);
        log.setSibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId);
        
        return log;
    }

}
