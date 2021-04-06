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
package org.fenixedu.treasury.dto.forwardpayments;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.joda.time.DateTime;

public class ForwardPaymentStatusBean {

    private boolean invocationSuccess;

    private ForwardPaymentStateType stateType;

    private String authorizationNumber;
    private DateTime authorizationDate;

    private String transactionId;
    private DateTime transactionDate;
    private BigDecimal payedAmount;

    private String statusCode;
    private String statusMessage;
    private String requestBody;
    private String responseBody;

    // Necessary for SIBS Online Payment Gateway

    private String sibsOnlinePaymentBrands;

    public ForwardPaymentStatusBean(boolean invocationSuccess, ForwardPaymentStateType type, String statusCode,
            String statusMessage, String requestBody, String responseBody) {
        this.invocationSuccess = invocationSuccess;
        this.stateType = type;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }

    public void editTransactionDetails(String transactionId, DateTime transactionDate, BigDecimal payedAmount) {
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.payedAmount = payedAmount;
    }

    public void editAuthorizationDetails(String authorizationNumber, DateTime authorizationDate) {
        this.authorizationNumber = authorizationNumber;
        this.authorizationDate = authorizationDate;
    }

    public boolean isInPayedState() {
        return getStateType() != null && getStateType().isPayed();
    }

    public boolean isInRejectedState() {
        return getStateType() != null && getStateType().isRejected();
    }

    public boolean isAbleToRegisterPostPayment(ForwardPaymentRequest forwardPayment) {
        return forwardPayment.isInStateToPostProcessPayment() && getStateType() != null && getStateType().isPayed();
    }

    public void defineSibsOnlinePaymentBrands(String paymentBrands) {
        this.sibsOnlinePaymentBrands = paymentBrands;
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public boolean isInvocationSuccess() {
        return invocationSuccess;
    }

    public ForwardPaymentStateType getStateType() {
        return stateType;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public DateTime getAuthorizationDate() {
        return authorizationDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public DateTime getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getPayedAmount() {
        return payedAmount;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getSibsOnlinePaymentBrands() {
        return sibsOnlinePaymentBrands;
    }

}
