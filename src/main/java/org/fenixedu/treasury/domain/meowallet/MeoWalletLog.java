/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  (o) Redistributions of source code must retain the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer.
 *
 *  (o) Redistributions in binary form must reproduce the
 *  above copyright notice, this list of conditions and the
 *  following disclaimer in the documentation and/or other
 *  materials provided with the distribution.
 *
 *  (o) Neither the name of Quorum Born IT nor the names of
 *  its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written
 *  permission.
 *
 *  (o) Universidade de Lisboa and its respective subsidiary
 *  Serviços Centrais da Universidade de Lisboa (Departamento
 *  de Informática), hereby referred to as the Beneficiary,
 *  is the sole demonstrated end-user and ultimately the only
 *  beneficiary of the redistributed binary form and/or source
 *  code.
 *
 *  (o) The Beneficiary is entrusted with either the binary form,
 *  the source code, or both, and by accepting it, accepts the
 *  terms of this License.
 *
 *  (o) Redistribution of any binary form and/or source code is
 *  only allowed in the scope of the Universidade de Lisboa
 *  FenixEdu(™)’s implementation projects.
 *
 *  (o) This license and conditions of redistribution of source
 *  code/binary can oly be reviewed by the Steering Comittee of
 *  FenixEdu(™) <http://www.fenixedu.org/>.
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
package org.fenixedu.treasury.domain.meowallet;

import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class MeoWalletLog extends MeoWalletLog_Base {

    public static final String REQUEST_PAYMENT_CODE = "mbPaymentRequest";
    public static final String MBWAY_REQUEST_PAYMENT = "mbwayPaymentRequest";
    public static final String WEBHOOK_NOTIFICATION = "WEBHOOK_NOTIFICATION";
    public static final String REQUEST_TRANSACTION_REPORT = "paymentStatus";

    public MeoWalletLog() {
        super();
    }

    protected MeoWalletLog(String operationCode, String extInvoiceId, String extCustomerId) {
        this();

        setOperationCode(operationCode);
        setExtInvoiceId(extInvoiceId);
        setExtCustomerId(extCustomerId);
        checkRules();
    }

    public MeoWalletLog(PaymentRequest paymentRequest, String statusCode, LocalizedString stateDescription) {
        this();
        setPaymentRequest(paymentRequest);
        setStatusCode(statusCode);
        setStateDescription(stateDescription);
    }

    private void checkRules() {

    }

// Give it a look to check if the method is the way you want!
    @pt.ist.fenixframework.Atomic
    public void delete() {
        super.deleteDomainObject();
    }

    public static MeoWalletLog createForMbwayPaymentRequest(String extInvoiceId, String extCustomerId) {
        MeoWalletLog log = new MeoWalletLog(MBWAY_REQUEST_PAYMENT, extInvoiceId, extCustomerId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        return log;
    }

    public static MeoWalletLog createForSibsPaymentRequest(String extInvoiceId, String extCustomerId) {
        MeoWalletLog log = new MeoWalletLog(REQUEST_PAYMENT_CODE, extInvoiceId, extCustomerId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        return log;
    }

    public static MeoWalletLog createLogForWebhookNotification() {
        MeoWalletLog log = new MeoWalletLog(WEBHOOK_NOTIFICATION, "", "");
        log.setResponsibleUsername(WEBHOOK_NOTIFICATION);
        return log;
    }

    public void logRequestSendDate() {
        setRequestSendDate(new DateTime());
    }

    public void logRequestReceiveDateAndData(String id, String type, String method, BigDecimal amount, String status,
            boolean operationSucess) {
        setRequestReceiveDate(new DateTime());
        setMeoWalletId(id);
        setPaymentType(type);
        setPaymentMethod(method);
        setAmount(amount);
        setStatusCode(status);
        setOperationSuccess(operationSucess);
    }

    public void savePaymentInfo(BigDecimal paidAmount, DateTime paymentDate) {
        setPaymentDate(paymentDate);
        setAmount(paidAmount);
    }

    public void markAsDuplicatedTransaction() {
        setTransactionDuplicated(true);
    }

    @Override
    public String getInternalMerchantTransactionId() {
        return super.getExtInvoiceId();
    }
    
    @Override
    public String getExternalTransactionId() {
        return super.getMeoWalletId();
    }
    
    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static MeoWalletLog createPaymentRequestLog(PaymentRequest paymentRequest, String code,
            LocalizedString localizedName) {
        return new MeoWalletLog(paymentRequest, code, localizedName);
    }

    @Atomic
    public static MeoWalletLog createForTransationReport(String merchantTransationId) {
        return new MeoWalletLog(REQUEST_TRANSACTION_REPORT, merchantTransationId, null);
    }

}