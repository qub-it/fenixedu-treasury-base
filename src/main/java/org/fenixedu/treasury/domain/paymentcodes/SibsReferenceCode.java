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
package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class SibsReferenceCode extends SibsReferenceCode_Base {
    
    public SibsReferenceCode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    public SibsReferenceCode(SibsPaymentCodePool sibsPaymentCodePool, String referenceCode, LocalDate validFrom,
            LocalDate validTo, BigDecimal minAmount, BigDecimal maxAmount) {
        this();
        
        setDigitalPaymentPlatform(sibsPaymentCodePool);
        setReferenceCode(referenceCode);
        setValidFrom(validFrom);
        setValidTo(validTo);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        
        checkRules();
    }

    public void checkRules() {
        
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.domainRoot.required");
        }
        
        if(StringUtils.isEmpty(getReferenceCode())) {
            throw new TreasuryDomainException("error.SibsReferenceCode.referenceCode.required");
        }
        
        if(getValidFrom() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.validFrom.required");
        }
        
        if(getValidTo() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.validTo.required");
        }
        
        if(getMinAmount() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.minAmount.required");
        }
        
        if(getMaxAmount() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.maxAmount.required");
        }
        
        if(findByReferenceCode(getDigitalPaymentPlatform().getEntityReferenceCode(), getReferenceCode()).count() > 1) {
            throw new TreasuryDomainException("error.SibsReferenceCode.referenceCode.duplicate");
        }
    }

    public String getFormattedCode() {
        final StringBuilder result = new StringBuilder();
        int i = 1;
        for (char character : getReferenceCode().toCharArray()) {
            result.append(character);
            if (i % 3 == 0) {
                result.append(" ");
            }
            i++;
        }

        return result.charAt(result.length() - 1) == ' ' ? result.deleteCharAt(result.length() - 1).toString() : result
                .toString();
    }

    public PaymentReferenceCodeStateType getState() {
        if(getSibsPaymentRequest() == null) {
            return PaymentReferenceCodeStateType.UNUSED;
        }
        
        return getSibsPaymentRequest().getState();
    }

    public boolean isInCreatedState() {
        return getSibsPaymentRequest() == null || getSibsPaymentRequest().isInCreatedState();
    }

    public boolean isInRequestedState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInRequestedState();
    }

    public boolean isInAnnuledState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInAnnuledState();
    }

    public boolean isInPaidState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInPaidState();
    }

    public Interval getValidInterval() {
        DateTime validFromDateTime = getValidFrom().toDateTimeAtStartOfDay();
        DateTime validToDateTime = getValidTo().plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1);
        return new Interval(validFromDateTime, validToDateTime);
    }
    
    @Override
    public SibsPaymentCodePool getDigitalPaymentPlatform() {
        return (SibsPaymentCodePool) super.getDigitalPaymentPlatform();
    }
    
    public DebtAccount getDebtAccount() {
        if(getSibsPaymentRequest() != null) {
            return getSibsPaymentRequest().getDebtAccount();
        }
        
        return null;
    }
    
    public void delete() {
        super.setDomainRoot(null);
        super.setDigitalPaymentPlatform(null);
        
        super.deleteDomainObject();
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsReferenceCode> findAll() {
        return FenixFramework.getDomainRoot().getSibsReferenceCodesSet().stream();
    }

    public static Stream<SibsReferenceCode> findByReferenceCode(String entityReferenceCode, String referenceCode) {
        return findAll()
                .filter(p -> entityReferenceCode.equals(p.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode()))
                .filter(p -> referenceCode.equals(p.getReferenceCode()));
    }

    public static SibsReferenceCode create(SibsPaymentCodePool sibsPaymentCodePool, String referenceCode, LocalDate validFrom,
            LocalDate validTo, BigDecimal minAmount, BigDecimal maxAmount) {
        return new SibsReferenceCode(sibsPaymentCodePool, referenceCode, validFrom, validTo, minAmount, maxAmount);
    }

}
