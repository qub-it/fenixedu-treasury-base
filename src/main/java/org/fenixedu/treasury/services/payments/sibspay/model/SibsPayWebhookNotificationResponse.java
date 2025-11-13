package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayWebhookNotificationResponse {

    @JsonProperty("statusCode")
    private Integer statusCode = null;

    @JsonProperty("statusMsg")
    private String statusMsg = null;

    @JsonProperty("notificationID")
    private String notificationID = null;

    public SibsPayWebhookNotificationResponse() {

    }

    public SibsPayWebhookNotificationResponse(Integer statusCode, String statusMsg, String notificationID) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.notificationID = notificationID;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public String getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(String notificationID) {
        this.notificationID = notificationID;
    }

}
