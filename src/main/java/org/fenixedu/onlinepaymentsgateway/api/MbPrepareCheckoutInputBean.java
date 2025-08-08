package org.fenixedu.onlinepaymentsgateway.api;

import java.math.BigDecimal;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MbPrepareCheckoutInputBean {
    public BigDecimal amount;
    public String merchantTransactionId;
    private DateTime sibsRefIntDate;
    private DateTime sibsRefLmtDate;

    public MbPrepareCheckoutInputBean(BigDecimal amount, String merchantTransactionId, DateTime sibsRefIntDate,
            DateTime sibsRefLmtDate) {
        super();
        this.amount = amount;
        this.merchantTransactionId = merchantTransactionId;
        this.sibsRefIntDate = sibsRefIntDate;
        this.sibsRefLmtDate = sibsRefLmtDate;
    }

    public MbPrepareCheckoutInputBean(BigDecimal amount, DateTime sibsRefIntDate, DateTime sibsRefLmtDate) {
        super();
        this.amount = amount;
        this.sibsRefIntDate = sibsRefIntDate;
        this.sibsRefLmtDate = sibsRefLmtDate;
    }

    public MbPrepareCheckoutInputBean() {
    }

    public boolean isPropertiesValid() {
        boolean returnValue = true;
        returnValue &= this.amount != null && getAmount().compareTo(BigDecimal.ZERO) > 0;
        returnValue &= sibsRefIntDate != null && sibsRefLmtDate != null && sibsRefIntDate.isBefore(sibsRefLmtDate);
        return returnValue;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public void setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
    }

    public DateTime getSibsRefIntDate() {
        return sibsRefIntDate;
    }

    public void setSibsRefIntDate(DateTime sibsRefIntDate) {
        this.sibsRefIntDate = sibsRefIntDate;
    }

    public DateTime getSibsRefLmtDate() {
        return sibsRefLmtDate;
    }

    public void setSibsRefLmtDate(DateTime sibsRefLmtDate) {
        this.sibsRefLmtDate = sibsRefLmtDate;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }
}
