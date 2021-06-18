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
package org.fenixedu.treasury.domain.paymentpenalty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltyTaxSettings extends PaymentPenaltyTaxSettings_Base {

    public PaymentPenaltyTaxSettings() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setActive(false);
        setCreatePaymentCode(false);
        setApplyPenaltyOnDebitsWithoutInterest(false);
    }
    
    public PaymentPenaltyTaxSettings(FinantialEntity finantialEntity, Product penaltyProduct) {
        this();
        
        this.setFinantialEntity(finantialEntity);
        this.setPenaltyProduct(penaltyProduct);
        
        checkRules();
    }

    private void checkRules() {
        if (super.getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.domainRoot.required");
        }

        if(getFinantialEntity() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.finantialEntity.required");
        }
        
        if (super.getActive() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.active.required");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getEmolumentDescription() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.is.active.but.emolumentDescription.is.null");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getPenaltyProduct() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.is.active.but.penaltyProduct.is.null");
        }

        if (super.getCreatePaymentCode() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.createPaymentCode.required");
        }

        if (super.getApplyPenaltyOnDebitsWithoutInterest() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.applyPenaltyOnDebitsWithoutInterest.required");
        }

    }

    public LocalizedString buildEmolumentDescription(DebitEntry originDebitEntry) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("debitEntryDescription", originDebitEntry.getDescription());
            valueMap.put("penaltyProductName", getPenaltyProduct().getName().getContent(locale));

            result = result.with(locale, StrSubstitutor.replace(getEmolumentDescription().getContent(locale), valueMap));
        }

        return result;
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setFinantialEntity(null);
        setPenaltyProduct(null);
        
        getTargetProductsSet().clear();

        super.deleteDomainObject();
    }

    public void edit(boolean active, Product penaltyProduct, LocalizedString emolumentDescription, boolean createPaymentCode,
            boolean applyPenaltyOnDebitsWithoutInterest) {
        super.setActive(active);

        super.setPenaltyProduct(penaltyProduct);
        super.setEmolumentDescription(emolumentDescription);
        super.setCreatePaymentCode(createPaymentCode);
        super.setApplyPenaltyOnDebitsWithoutInterest(applyPenaltyOnDebitsWithoutInterest);

        checkRules();
    }

    /*
     * SERVICES
     */

    public static PaymentPenaltyTaxSettings create(FinantialEntity finantialEntity, Product penaltyProduct) {
        return new PaymentPenaltyTaxSettings(finantialEntity, penaltyProduct);
    }

    public static Stream<PaymentPenaltyTaxSettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltyTaxSettingsSet().stream();
    }

    public static Stream<PaymentPenaltyTaxSettings> findActive() {
        return findAll().filter(s -> Boolean.TRUE.equals(s.getActive()));
    }
    
    public static Stream<PaymentPenaltyTaxSettings> findActiveForOriginDebitEntry(DebitEntry originDebitEntry) {
        return findActive().filter(s -> s.getTargetProductsSet().contains(originDebitEntry.getProduct()));
    }

}
