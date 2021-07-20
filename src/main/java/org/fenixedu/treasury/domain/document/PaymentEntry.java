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
package org.fenixedu.treasury.domain.document;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.PaymentMethodReference;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentEntry extends PaymentEntry_Base {

    protected PaymentEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected PaymentEntry(final PaymentMethod paymentMethod, final SettlementNote settlementNote, final BigDecimal payedAmount,
            final String paymentMethodId, final Map<String, String> propertiesMap) {
        this();
        init(paymentMethod, settlementNote, payedAmount, paymentMethodId, propertiesMap);
    }

    protected void init(final PaymentMethod paymentMethod, final SettlementNote settlementNote, final BigDecimal payedAmount,
            final String paymentMethodId, final Map<String, String> propertiesMap) {
        setPaymentMethod(paymentMethod);
        setSettlementNote(settlementNote);
        setPayedAmount(payedAmount);
        setPaymentMethodId(paymentMethodId);
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));

        if (StringUtils.isEmpty(getPaymentMethodId()) && PaymentMethodReference.isPaymentMethodReferencesApplied()) {
            // Apply default payment method reference if exists
            Optional<PaymentMethodReference> defaultMethodReference = PaymentMethodReference.findUniqueDefaultPaymentMethodReference(getPaymentMethod(),
                    getSettlementNote().getDebtAccount().getFinantialInstitution());
            
            if(defaultMethodReference.isPresent()) {
                setPaymentMethodId(defaultMethodReference.get().getPaymentReferenceId());
            }
        }

        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.PaymentEntry.paymentMethod.required");
        }

        if (getSettlementNote() == null) {
            throw new TreasuryDomainException("error.PaymentEntry.settlementNote.required");
        }

        //CHANGE_ME In order to validate UNIQUE restrictions
        //if (findByPaymentMethod(getPaymentMethod().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.paymentMethod.duplicated");
        //} 
        //if (findBySettlementNote(getSettlementNote().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.settlementNote.duplicated");
        //} 
        //if (findByPayedAmount(getPayedAmount().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.payedAmount.duplicated");
        //} 
    }

    @Atomic
    public void edit(final PaymentMethod paymentMethod, final SettlementNote settlementNote,
            final java.math.BigDecimal payedAmount, final String paymentMethodId) {
        setPaymentMethod(paymentMethod);
        setSettlementNote(settlementNote);
        setPayedAmount(payedAmount);
        setPaymentMethodId(paymentMethodId);
        checkRules();
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }
    
    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentEntry.cannot.delete");
        }

        setDomainRoot(null);
        this.setPaymentMethod(null);

        deleteDomainObject();
    }

    @Atomic
    public static PaymentEntry create(final PaymentMethod paymentMethod, final SettlementNote settlementNote,
            final BigDecimal payedAmount, final String paymentMethodId, final Map<String, String> propertiesMap) {
        return new PaymentEntry(paymentMethod, settlementNote, payedAmount, paymentMethodId, propertiesMap);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<PaymentEntry> findAll() {
        return FenixFramework.getDomainRoot().getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findByPaymentMethod(final PaymentMethod paymentMethod) {
        return paymentMethod.getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findBySettlementNote(final SettlementNote settlementNote) {
        return settlementNote.getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findByPayedAmount(final java.math.BigDecimal payedAmount) {
        return findAll().filter(i -> payedAmount.equals(i.getPayedAmount()));
    }

}
