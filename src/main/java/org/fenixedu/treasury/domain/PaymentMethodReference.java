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
package org.fenixedu.treasury.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentMethodReference extends PaymentMethodReference_Base {

    public static Comparator<PaymentMethodReference> COMPARE_BY_NAME =
            (o1, o2) -> o1.getName().compareTo(o2.getName()) * 10 + o1.getExternalId().compareTo(o2.getExternalId());

    public PaymentMethodReference() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected PaymentMethodReference(PaymentMethod paymentMethod, FinantialInstitution finantialInstitution, String name,
            String paymentReferenceId, boolean forDigitalPayments) {
        this();

        setPaymentMethod(paymentMethod);
        setFinantialInstitution(finantialInstitution);
        setName(name);
        setPaymentReferenceId(paymentReferenceId);
        setPaymentMethodReferenceActive(true);
        setForDigitalPayments(forDigitalPayments);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.PaymentMethodReference.domainRoot.required");
        }

        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.PaymentMethodReference.paymentMethod.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.PaymentMethodReference.finantialInstitution.required");
        }

        if (StringUtils.isEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentMethodReference.name.required");
        }

        if (StringUtils.isEmpty(getPaymentReferenceId())) {
            throw new TreasuryDomainException("error.PaymentMethodReference.paymentReferenceId.required");
        }

        if (getForDigitalPayments() == null) {
            throw new TreasuryDomainException("error.PaymentMethodReference.forDigitalPayments.required");
        }

        if (findActiveAndForDigitalPayments(getPaymentMethod(), getFinantialInstitution()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentMethodReference.forDigitalPayments.active.more.than.one");
        }
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(getPaymentMethodReferenceActive());
    }

    public void activate() {
        setPaymentMethodReferenceActive(true);

        checkRules();
    }

    public void deactivate() {
        setPaymentMethodReferenceActive(false);

        checkRules();
    }

    public void edit(String name, String paymentReferenceId, boolean forDigitalPayments) {
        setName(name);
        setPaymentReferenceId(paymentReferenceId);
        setForDigitalPayments(forDigitalPayments);

        checkRules();
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setPaymentMethod(null);
        setFinantialInstitution(null);

        super.deleteDomainObject();
    }

    public String buildPaymentReferenceId(PaymentRequest paymentRequest) {
        Map<String, String> valueMap = new HashMap<String, String>();

        if (paymentRequest instanceof SibsPaymentRequest) {
            valueMap.put("sibsEntityReferenceCode",
                    ((SibsPaymentRequest) paymentRequest).getEntityReferenceCode());
            valueMap.put("sibsReferenceCode", ((SibsPaymentRequest) paymentRequest).getReferenceCode());
        }

        return StrSubstitutor.replace(getPaymentReferenceId(), valueMap);
    }

    /* ********
     * SERVICES
     * ********
     */

    public static boolean isPaymentMethodReferencesApplied() {
        return findAll().filter(p -> p.isActive() && !Boolean.TRUE.equals(p.getForDigitalPayments())).count() > 0;
    }

    public static Stream<PaymentMethodReference> findAll() {
        return FenixFramework.getDomainRoot().getPaymentMethodReferencesSet().stream();
    }

    public static Stream<PaymentMethodReference> find(PaymentMethod paymentMethod, FinantialInstitution finantialInstitution) {
        return paymentMethod.getPaymentMethodReferencesSet().stream()
                .filter(p -> p.getFinantialInstitution() == finantialInstitution);
    }

    public static Stream<PaymentMethodReference> findActive(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return find(paymentMethod, finantialInstitution).filter(p -> p.isActive());
    }

    public static Stream<PaymentMethodReference> findActiveForSettlementNoteRegistrationInBackoffice(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return findActive(paymentMethod, finantialInstitution).filter(p -> !Boolean.TRUE.equals(p.getForDigitalPayments()));
    }

    public static Stream<PaymentMethodReference> findActiveAndForDigitalPayments(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return findActive(paymentMethod, finantialInstitution).filter(p -> Boolean.TRUE.equals(p.getForDigitalPayments()));
    }

    public static Optional<PaymentMethodReference> findUniqueActiveAndForDigitalPayments(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return findActiveAndForDigitalPayments(paymentMethod, finantialInstitution).findFirst();
    }

    public static Stream<PaymentMethodReference> findByPaymentReferenceId(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution, String paymentReferenceId) {
        return find(paymentMethod, finantialInstitution).filter(p -> paymentReferenceId.equals(p.getPaymentReferenceId()));
    }

    public static PaymentMethodReference create(PaymentMethod paymentMethod, FinantialInstitution finantialInstitution,
            String name, String paymentReferenceId, boolean forDigitalPayments) {
        return new PaymentMethodReference(paymentMethod, finantialInstitution, name, paymentReferenceId, forDigitalPayments);
    }
}
