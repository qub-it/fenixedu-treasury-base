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
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

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
            String paymentReferenceId) {
        this();

        setPaymentMethod(paymentMethod);
        setFinantialInstitution(finantialInstitution);
        setName(name);
        setPaymentReferenceId(paymentReferenceId);
        setDefaultReference(false);
        setPaymentMethodReferenceActive(true);

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

        if (findByPaymentReferenceId(getPaymentMethod(), getFinantialInstitution(), getPaymentReferenceId()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentMethodReference.paymentReferenceId.duplicated");
        }

        if (findDefaultPaymentMethodReference(getPaymentMethod(), getFinantialInstitution()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentMethodReference.defaultPaymentMethodReference.not.unique");
        }
        
        if(isDefault() && !isActive()) {
            throw new TreasuryDomainException("error.PaymentMethodReference.default.must.be.active");
        }
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(getDefaultReference());
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(getPaymentMethodReferenceActive());
    }

    public void markAsDefault() {
        // Mark all others as not default
        find(getPaymentMethod(), getFinantialInstitution()).forEach(p -> p.setDefaultReference(false));

        setDefaultReference(true);

        checkRules();
    }

    public void markAsNotDefault() {
        setDefaultReference(false);

        checkRules();
    }

    public void activate() {
        setPaymentMethodReferenceActive(true);

        checkRules();
    }

    public void deactivate() {
        setDefaultReference(false);
        setPaymentMethodReferenceActive(false);

        checkRules();
    }
    
    public void edit(String name, String paymentReferenceId) {
        setName(name);
        setPaymentReferenceId(paymentReferenceId);
        
        checkRules();
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setPaymentMethod(null);
        setFinantialInstitution(null);

        super.deleteDomainObject();
    }

    /* ********
     * SERVICES
     * ********
     */

    public static boolean isPaymentMethodReferencesApplied() {
        return findAll().count() > 0;
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

    public static Stream<PaymentMethodReference> findByPaymentReferenceId(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution, String paymentReferenceId) {
        return find(paymentMethod, finantialInstitution).filter(p -> paymentReferenceId.equals(p.getPaymentReferenceId()));
    }

    public static Stream<PaymentMethodReference> findDefaultPaymentMethodReference(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return find(paymentMethod, finantialInstitution).filter(p -> p.isDefault());
    }

    public static Optional<PaymentMethodReference> findUniqueDefaultPaymentMethodReference(PaymentMethod paymentMethod,
            FinantialInstitution finantialInstitution) {
        return find(paymentMethod, finantialInstitution).filter(p -> p.isDefault()).findFirst();
    }
    
    public static PaymentMethodReference create(PaymentMethod paymentMethod, FinantialInstitution finantialInstitution, String name, String paymentReferenceId) {
        return new PaymentMethodReference(paymentMethod, finantialInstitution, name, paymentReferenceId);
    }
}
