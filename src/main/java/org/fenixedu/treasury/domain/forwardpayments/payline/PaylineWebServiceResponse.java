package org.fenixedu.treasury.domain.forwardpayments.payline;

import java.math.BigDecimal;

import org.joda.time.DateTime;

public class PaylineWebServiceResponse {

    private String resultCode;
    private String authorizationNumber;
    private DateTime authorizationDate;
    private String transactionId;
    private BigDecimal paymentAmount;
    private DateTime transactionDate;
    private String resultLongMessage;

    private String jsonRequest;
    private String jsonResponse;
    
    private String token;
    private String redirectURL;

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public void setAuthorizationNumber(String authorizationNumber) {
        this.authorizationNumber = authorizationNumber;
    }

    public DateTime getAuthorizationDate() {
        return authorizationDate;
    }

    public void setAuthorizationDate(DateTime authorizationDate) {
        this.authorizationDate = authorizationDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public DateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(DateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getResultLongMessage() {
        return resultLongMessage;
    }

    public void setResultLongMessage(String resultLongMessage) {
        this.resultLongMessage = resultLongMessage;
    }

    public String getJsonRequest() {
        return jsonRequest;
    }

    public void setJsonRequest(String jsonRequest) {
        this.jsonRequest = jsonRequest;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getRedirectURL() {
        return redirectURL;
    }
    
    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }
    
}
