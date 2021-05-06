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
package org.fenixedu.treasury.dto.meowallet;

import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.meowallet.MeoWallet;
import org.joda.time.DateTime;

public class MeoWalletPaymentBean implements DigitalPlatformResultBean {

    private BigDecimal amount;

    private MeoWalletClientBean client;

    private DateTime create_date;

    private String currency;

    private DateTime date;

    private String ext_invoiceid;

    private String ext_customerid;

    private String id;

    private MeoWalletPaymentItemBean[] items;

    private MeoWalletMbBean mb;

    private String method;

    private DateTime modified_date;

    private String status;

    private String type;

    private MeoWalletMbwayPayment mbway;

    private MeoWalletCardPayment card;

    private String callback;

    private String requestLog;

    private String responseLog;

    public MeoWalletPaymentBean(BigDecimal payableAmount, String customerName, List<MeoWalletPaymentItemBean> items) {
        this.amount = payableAmount;
        client = new MeoWalletClientBean(customerName);

        this.items = new MeoWalletPaymentItemBean[items.size()];
        this.items = items.toArray(this.items);
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public MeoWalletClientBean getClient() {
        return client;
    }

    public void setClient(MeoWalletClientBean client) {
        this.client = client;
    }

    public DateTime getCreate_date() {
        return create_date;
    }

    public void setCreate_date(DateTime create_date) {
        this.create_date = create_date;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public String getExt_invoiceid() {
        return ext_invoiceid;
    }

    public void setExt_invoiceid(String ext_invoiceid) {
        this.ext_invoiceid = ext_invoiceid;
    }

    public String getExt_customerid() {
        return ext_customerid;
    }

    public void setExt_customerid(String ext_customerid) {
        this.ext_customerid = ext_customerid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MeoWalletPaymentItemBean[] getItems() {
        return items;
    }

    public void setItems(MeoWalletPaymentItemBean[] items) {
        this.items = items;
    }

    public MeoWalletMbBean getMb() {
        return mb;
    }

    public void setMb(MeoWalletMbBean mb) {
        this.mb = mb;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public DateTime getModified_date() {
        return modified_date;
    }

    public void setModified_date(DateTime modified_date) {
        this.modified_date = modified_date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MeoWalletMbwayPayment getMbway() {
        return mbway;
    }

    public void setMbway(MeoWalletMbwayPayment mbway) {
        this.mbway = mbway;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(String requestLog) {
        this.requestLog = requestLog;
    }

    public String getResponseLog() {
        return responseLog;
    }

    public void setResponseLog(String responseLog) {
        this.responseLog = responseLog;
    }

    public static MeoWalletPaymentBean createMbwayPaymentBean(BigDecimal payableAmount, String extInvoiceId, String extCustomerId,
            String customerName, String localPhoneNumber, List<MeoWalletPaymentItemBean> items) {
        MeoWalletPaymentBean result = new MeoWalletPaymentBean(payableAmount, customerName, items);
        result.setExt_invoiceid(extInvoiceId);
        result.setExt_customerid(extCustomerId);
        result.setMbway(new MeoWalletMbwayPayment(localPhoneNumber));
        result.setMethod("MBWAY");
        result.setType("PAYMENT");
        result.setCurrency("EUR");

        return result;
    }

    public static MeoWalletPaymentBean createMBPaymentBean(BigDecimal payableAmount, String extInvoiceId, String extCustomerId,
            String customerName, List<MeoWalletPaymentItemBean> items) {
        MeoWalletPaymentBean result = new MeoWalletPaymentBean(payableAmount, customerName, items);
        result.setExt_invoiceid(extInvoiceId);
        result.setExt_customerid(extCustomerId);
        result.setCurrency("EUR");

        return result;
    }

    public static MeoWalletPaymentBean createForwardPaymentBean(BigDecimal payableAmount, String customerName,
            List<MeoWalletPaymentItemBean> items) {
        MeoWalletPaymentBean result = new MeoWalletPaymentBean(payableAmount, customerName, items);
        result.setCurrency("EUR");
        return result;
    }

    public MeoWalletCardPayment getCard() {
        return card;
    }

    public void setCard(MeoWalletCardPayment card) {
        this.card = card;
    }

    @Override
    public String getTransactionId() {
        return getId();
    }

    @Override
    public String getMerchantTransactionId() {
        return getExt_invoiceid();
    }

    @Override
    public String getPaymentType() {
        return getMethod();
    }

    @Override
    public String getPaymentBrand() {
        return getCard() != null ? getCard().getType() : null;
    }

    @Override
    public String getTimestamp() {
        return getDate().toString("YYYY-MM-DDThh:mm:ssTZD");
    }

    @Override
    public DateTime getPaymentDate() {
        return getPaymentDate();
    }

    @Override
    public boolean isPaid() {
        return MeoWallet.STATUS_COMPLETED.equals(getStatus());
    }
    
    @Override
    public boolean isOperationSuccess() {
        return !MeoWallet.STATUS_FAIL.equals(getStatus());
    }

    @Override
    public String getPaymentResultCode() {
        return getStatus();
    }

    @Override
    public String getPaymentResultDescription() {
        return null;
    }
}
