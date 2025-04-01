package org.fenixedu.treasury.dto.sibspay;

import org.fenixedu.treasury.domain.sibspay.MbwayMandateState;
import org.joda.time.LocalDate;

import java.math.BigDecimal;

public class MbwayMandateBean {

    private String mandateId;
    private String transactionId;
    private MbwayMandateState state;
    private BigDecimal plafond;
    private LocalDate expirationDate;

    public MbwayMandateBean() {
    }

    public MbwayMandateBean(String mandateId, String transactionId, MbwayMandateState state, BigDecimal plafond,
            LocalDate expirationDate) {
        this.mandateId = mandateId;
        this.transactionId = transactionId;
        this.state = state;
        this.plafond = plafond;
        this.expirationDate = expirationDate;
    }

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public MbwayMandateState getState() {
        return state;
    }

    public void setState(MbwayMandateState state) {
        this.state = state;
    }

    public BigDecimal getPlafond() {
        return plafond;
    }

    public void setPlafond(BigDecimal plafond) {
        this.plafond = plafond;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
}
