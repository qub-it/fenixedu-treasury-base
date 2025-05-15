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
package org.fenixedu.treasury.domain.payments;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalMonth;
import org.fenixedu.treasury.domain.FiscalYear;
import org.fenixedu.treasury.domain.sibspay.MbwayMandate;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentRequestLog extends PaymentRequestLog_Base {

    public static final String OCTECT_STREAM_CONTENT_TYPE = "application/octet-stream";

    public static final Comparator<PaymentRequestLog> COMPARE_BY_CREATION_DATE =
            Comparator.comparing(PaymentRequestLog::getCreationDate).thenComparing(PaymentRequestLog::getExternalId);

    public PaymentRequestLog() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());

        // ANIL 2024-11-14 (#qubIT-Fenix-5969)
        //
        // The association with fiscal month is only to avoid
        // reading all instances of this entity, which in some
        // instalations are many. This relation will be used
        // to filter the logs by an interval of dates.
        //
        // It is ok to associate with the fiscal month of any finantial
        // institution, even if it does not match with the finantial
        // institution of the associated payment request
        FinantialInstitution finantialInstitution = FinantialInstitution.findAll().iterator().next();

        int year = getCreationDate().getYear();
        int monthOfYear = getCreationDate().getMonthOfYear();

        FiscalYear fiscalYear = FiscalYear.getOrCreateFiscalYear(finantialInstitution, year);
        FiscalMonth fiscalMonth = FiscalMonth.getOrCreateFiscalMonth(fiscalYear, monthOfYear);

        setFiscalMonth(fiscalMonth);
    }

    protected PaymentRequestLog(PaymentRequest request, String operationCode, String stateCode,
            LocalizedString stateDescription) {
        this();

        setPaymentRequest(request);
        setOperationCode(operationCode);
        setStateCode(stateCode);
        setStateDescription(stateDescription);

        if (request != null) {
            // Redefine the fiscal month of the FinantialInstitution 
            FinantialInstitution finantialInstitution = request.getDebtAccount().getFinantialInstitution();
            int year = getCreationDate().getYear();
            int monthOfYear = getCreationDate().getMonthOfYear();

            FiscalYear fiscalYear = FiscalYear.getOrCreateFiscalYear(finantialInstitution, year);
            FiscalMonth fiscalMonth = FiscalMonth.getOrCreateFiscalMonth(fiscalYear, monthOfYear);

            setFiscalMonth(fiscalMonth);
        }
    }

    protected PaymentRequestLog(MbwayMandate mbwayMandate, String operationCode, String stateCode,
            LocalizedString stateDescription) {
        this();

        setMbwayMandate(mbwayMandate);
        setOperationCode(operationCode);
        setStateCode(stateCode);
        setStateDescription(stateDescription);
    }

    public PaymentRequestLog(String webhookNotification) {
        this();
        setOperationCode(webhookNotification);
    }

    public void saveRequest(String requestBody) {
        if (requestBody == null) {
            return;
        }

        try {
            String filename = String.format("request_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setRequestLogFile(PaymentRequestLogFile.create(filename, requestBody.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveResponse(String responseBody) {
        if (responseBody == null) {
            return;
        }

        try {
            String filename = String.format("response_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setResponseLogFile(PaymentRequestLogFile.create(filename, responseBody.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void logException(Exception e) {
        String exceptionLog = String.format("%s\n%s", e.getLocalizedMessage(), ExceptionUtils.getFullStackTrace(e));
        setExceptionOccured(true);
        setOperationSuccess(false);

        try {
            String filename = String.format("exception_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setExceptionLogFile(PaymentRequestLogFile.create(filename, exceptionLog.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void editAuthorizationData(String authorizationId, DateTime authorizationDate) {
        setAuthorizationId(authorizationId);
        setAuthorizationDate(authorizationDate);
    }

    public void logRequestSendDate() {
        setRequestSendDate(new DateTime());
    }

    public void markAsDuplicatedTransaction() {
        setTransactionDuplicated(true);
    }

    public void savePaymentInfo(BigDecimal amount, DateTime paymentDate) {
        setAmount(amount);
        setPaymentDate(paymentDate);
    }

    public void savePaymentTypeAndBrand(String paymentType, String paymentBrand) {
        setPaymentType(paymentType);
        setPaymentBrand(paymentBrand);
    }

    public void logRequestReceiveDateAndData(String transactionId, boolean operationSuccess, boolean transactionPaid,
            String operationResultCode, String operationResultDescription) {
        setRequestReceiveDate(new DateTime());
        setExternalTransactionId(transactionId);
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

    @Override
    public void setPaymentRequest(PaymentRequest paymentRequest) {
        super.setPaymentRequest(paymentRequest);

        // Redefine the fiscal month of the FinantialInstitution associated 
        // with the payment request
        if (paymentRequest != null) {
            FinantialInstitution finantialInstitution = paymentRequest.getDebtAccount().getFinantialInstitution();
            int year = getCreationDate().getYear();
            int monthOfYear = getCreationDate().getMonthOfYear();

            FiscalYear fiscalYear = FiscalYear.getOrCreateFiscalYear(finantialInstitution, year);
            FiscalMonth fiscalMonth = FiscalMonth.getOrCreateFiscalMonth(fiscalYear, monthOfYear);

            setFiscalMonth(fiscalMonth);
        }
    }

    // @formatter:off
    /*
     *
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends PaymentRequestLog> findAll() {
        return FenixFramework.getDomainRoot().getPaymentRequestLogsSet().stream();
    }

    public static PaymentRequestLog create(PaymentRequest request, String operationCode, String stateCode,
            LocalizedString stateDescription) {
        return new PaymentRequestLog(request, operationCode, stateCode, stateDescription);
    }

    public static PaymentRequestLog create(MbwayMandate mbwayMandate, String operationCode, String stateCode,
            LocalizedString stateDescription) {
        return new PaymentRequestLog(mbwayMandate, operationCode, stateCode, stateDescription);
    }

}
