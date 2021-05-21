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

public class MeoWalletCheckoutBean {

    //O Mandatory
    private String id;

    //I
    private String[] exclude;

    //I Mandatory
    private String url_cancel;

    //I Mandatory
    private String url_confirm;

    //IO Mandatory
    private MeoWalletPaymentBean payment;

    //IO
    private String ext_invoiceid;

    //IO
    private String ext_customerid;

    //O
    private String url_redirect;

    private String requestLog;

    private String responseLog;

    public MeoWalletCheckoutBean() {
        super();
    }

    public MeoWalletCheckoutBean(MeoWalletPaymentBean paymentBean, String successURL, String insuccessUrl, String extInvoiceId,
            String extCustomerId, String[] exclude) {
        this();
        this.payment = paymentBean;
        this.url_confirm = successURL;
        this.url_cancel = insuccessUrl;
        paymentBean.setExt_invoiceid(extInvoiceId);
        paymentBean.setExt_customerid(extCustomerId);
        this.exclude = exclude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getExclude() {
        return exclude;
    }

    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }

    public String getUrl_cancel() {
        return url_cancel;
    }

    public void setUrl_cancel(String url_cancel) {
        this.url_cancel = url_cancel;
    }

    public String getUrl_confirm() {
        return url_confirm;
    }

    public void setUrl_confirm(String url_confirm) {
        this.url_confirm = url_confirm;
    }

    public MeoWalletPaymentBean getPayment() {
        return payment;
    }

    public void setPayment(MeoWalletPaymentBean payment) {
        this.payment = payment;
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

    public String getUrl_redirect() {
        return url_redirect;
    }

    public void setUrl_redirect(String url_redirect) {
        this.url_redirect = url_redirect;
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

}
