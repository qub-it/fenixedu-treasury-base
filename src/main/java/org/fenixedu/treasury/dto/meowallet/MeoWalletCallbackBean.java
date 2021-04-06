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

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.joda.time.DateTime;

public class MeoWalletCallbackBean implements DigitalPlatformResultBean {
    private BigDecimal amount;
    private DateTime create_date;
    private String currency;
    private String event;
    private String ext_customerid;
    private String ext_invoiceid;
    private String ext_email;
    private String mb_entity;
    private String mb_ref;
    private String method;
    private DateTime modified_date;
    private String operation_id;
    private String operation_status;
    private int user;

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getExt_customerid() {
        return ext_customerid;
    }

    public void setExt_customerid(String ext_customerid) {
        this.ext_customerid = ext_customerid;
    }

    public String getExt_invoiceid() {
        return ext_invoiceid;
    }

    public void setExt_invoiceid(String ext_invoiceid) {
        this.ext_invoiceid = ext_invoiceid;
    }

    public String getExt_email() {
        return ext_email;
    }

    public void setExt_email(String ext_email) {
        this.ext_email = ext_email;
    }

    public String getMb_entity() {
        return mb_entity;
    }

    public void setMb_entity(String mb_entity) {
        this.mb_entity = mb_entity;
    }

    public String getMb_ref() {
        return mb_ref;
    }

    public void setMb_ref(String mb_ref) {
        this.mb_ref = mb_ref;
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

    public String getOperation_id() {
        return operation_id;
    }

    public void setOperation_id(String operation_id) {
        this.operation_id = operation_id;
    }

    public String getOperation_status() {
        return operation_status;
    }

    public void setOperation_status(String operation_status) {
        this.operation_status = operation_status;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    @Override
    public String getTransactionId() {
        return getOperation_id();
    }

    @Override
    public String getMerchantTransactionId() {
        return getExt_invoiceid();
    }

    @Override
    public String getPaymentType() {
        // TODO Auto-generated method stub
        return getMethod();
    }

    @Override
    public String getPaymentBrand() {
        return null;
    }

    @Override
    public String getTimestamp() {
        return getCreate_date().toString("YYYY-MM-DDThh:mm:ssTZD");
    }

    @Override
    public DateTime getPaymentDate() {
        return getModified_date();
    }

    @Override
    public boolean isPaid() {
        return getOperation_status().equals("COMPLETED");
    }

    @Override
    public String getPaymentResultCode() {
        return getOperation_status();
    }

    @Override
    public String getPaymentResultDescription() {
        return null;
    }
}
