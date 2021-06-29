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

import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;

public class ForwardPaymentConfigurationBean {

    private boolean active;
    private String name;
    private String paymentURL;
    private String returnURL;
    private String virtualTPAMOXXURL;
    private String virtualTPAMerchantId;
    private String virtualTPAId;
    private String virtualTPAKeyStoreName;
    private String virtualTPACertificateAlias;
    private String virtualTPACertificatePassword;
    private String implementation;
    private String paylineMerchantId;
    private String paylineMerchantAccessKey;
    private String paylineContractNumber;
    
    private Series series;
    private PaymentMethod paymentMethod;

    private String reimbursementPolicyJspFile;
    private String privacyPolicyJspFile;
    private String logosJspPageFile;
    
    public ForwardPaymentConfigurationBean() {
    }
    
    public ForwardPaymentConfigurationBean(final ForwardPaymentConfiguration configuration) {
        setActive(configuration.isActive());
        setName(configuration.getName());
        setPaymentURL(configuration.getPaymentURL());
        setReturnURL(configuration.getReturnURL());
        setVirtualTPAMOXXURL(configuration.getVirtualTPAMOXXURL());
        setVirtualTPAMerchantId(configuration.getVirtualTPAMerchantId());
        setVirtualTPAId(configuration.getVirtualTPAId());
        setVirtualTPAKeyStoreName(configuration.getVirtualTPAKeyStoreName());
        setVirtualTPACertificateAlias(configuration.getVirtualTPACertificateAlias());
        setVirtualTPACertificatePassword(configuration.getVirtualTPACertificatePassword());
        setImplementation(configuration.getImplementation());
        setPaylineMerchantId(configuration.getPaylineMerchantId());
        setPaylineMerchantAccessKey(configuration.getPaylineMerchantAccessKey());
        setPaylineContractNumber(configuration.getPaylineContractNumber());
        
        setSeries(configuration.getSeries());
        setPaymentMethod(configuration.getPaymentMethod());
        
        setReimbursementPolicyJspFile(configuration.getReimbursementPolicyJspFile());
        setPrivacyPolicyJspFile(configuration.getPrivacyPolicyJspFile());
        setLogosJspPageFile(configuration.getLogosJspPageFile());
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPaymentURL() {
        return paymentURL;
    }

    public void setPaymentURL(String paymentURL) {
        this.paymentURL = paymentURL;
    }

    public String getReturnURL() {
        return returnURL;
    }

    public void setReturnURL(String returnURL) {
        this.returnURL = returnURL;
    }

    public String getVirtualTPAMOXXURL() {
        return virtualTPAMOXXURL;
    }

    public void setVirtualTPAMOXXURL(String virtualTPAMOXXURL) {
        this.virtualTPAMOXXURL = virtualTPAMOXXURL;
    }

    public String getVirtualTPAMerchantId() {
        return virtualTPAMerchantId;
    }

    public void setVirtualTPAMerchantId(String virtualTPAMerchantId) {
        this.virtualTPAMerchantId = virtualTPAMerchantId;
    }

    public String getVirtualTPAId() {
        return virtualTPAId;
    }

    public void setVirtualTPAId(String virtualTPAId) {
        this.virtualTPAId = virtualTPAId;
    }

    public String getVirtualTPAKeyStoreName() {
        return virtualTPAKeyStoreName;
    }

    public void setVirtualTPAKeyStoreName(String virtualTPAKeyStoreName) {
        this.virtualTPAKeyStoreName = virtualTPAKeyStoreName;
    }

    public String getVirtualTPACertificateAlias() {
        return virtualTPACertificateAlias;
    }

    public void setVirtualTPACertificateAlias(String virtualTPACertificateAlias) {
        this.virtualTPACertificateAlias = virtualTPACertificateAlias;
    }

    public String getVirtualTPACertificatePassword() {
        return virtualTPACertificatePassword;
    }

    public void setVirtualTPACertificatePassword(String virtualTPACertificatePassword) {
        this.virtualTPACertificatePassword = virtualTPACertificatePassword;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getPaylineMerchantId() {
        return paylineMerchantId;
    }

    public void setPaylineMerchantId(String paylineMerchantId) {
        this.paylineMerchantId = paylineMerchantId;
    }

    public String getPaylineMerchantAccessKey() {
        return paylineMerchantAccessKey;
    }

    public void setPaylineMerchantAccessKey(String paylineMerchantAccessKey) {
        this.paylineMerchantAccessKey = paylineMerchantAccessKey;
    }

    public String getPaylineContractNumber() {
        return paylineContractNumber;
    }

    public void setPaylineContractNumber(String paylineContractNumber) {
        this.paylineContractNumber = paylineContractNumber;
    }
    
    public Series getSeries() {
        return series;
    }
    
    public void setSeries(Series series) {
        this.series = series;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReimbursementPolicyJspFile() {
        return reimbursementPolicyJspFile;
    }

    public void setReimbursementPolicyJspFile(String reimbursementPolicyJspFile) {
        this.reimbursementPolicyJspFile = reimbursementPolicyJspFile;
    }

    public String getPrivacyPolicyJspFile() {
        return privacyPolicyJspFile;
    }

    public void setPrivacyPolicyJspFile(String privacyPolicyJspFile) {
        this.privacyPolicyJspFile = privacyPolicyJspFile;
    }
    
    public String getLogosJspPageFile() {
        return logosJspPageFile;
    }
    
    public void setLogosJspPageFile(String logosJspPageFile) {
        this.logosJspPageFile = logosJspPageFile;
    }
}
