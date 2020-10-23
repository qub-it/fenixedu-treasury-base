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
package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Installment extends Installment_Base {

    public static final Comparator<? super Installment> COMPARE_BY_DUEDATE =
            (m1, m2) -> m1.getDueDate().compareTo(m2.getDueDate());

    public Installment() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public Installment(LocalizedString description, LocalDate dueDate, PaymentPlan paymentPlan) {
        this();
        setDescription(description);
        setDueDate(dueDate);
        setPaymentPlan(paymentPlan);
        checkRules();
    }

    @Atomic
    public static Installment create(LocalizedString description, LocalDate dueDate, PaymentPlan paymentPlan) {
        return new Installment(description, dueDate, paymentPlan);
    }

    private void checkRules() {
        if (getDescription() == null) {
            throw new TreasuryDomainException("error.Installment.description.required");
        }

        if (getDueDate() == null) {
            throw new TreasuryDomainException("error.Installment.dueDate.required");
        }

        if (getPaymentPlan() == null) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.required");
        }

        if (getDueDate().isBefore(getPaymentPlan().getCreationDate())) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.must.be.after.paymentPlan.creationDate");
        }
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        getInstallmentEntriesSet().forEach(i -> i.delete());
        deleteDomainObject();
    }

    public BigDecimal getTotalAmount() {
        return getCurrency().getValueWithScale(
                getInstallmentEntriesSet().stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getPaidAmount() {
        return getCurrency().getValueWithScale(
                getInstallmentEntriesSet().stream().map(i -> i.getPaidAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getOpenAmount() {
        return getCurrency().getValueWithScale(getTotalAmount().subtract(getPaidAmount()));
    }

    public boolean isPaid() {
        return TreasuryConstants.isZero(getOpenAmount());
    }

    public List<InstallmentEntry> getSortedInstallmentEntries() {
        return super.getInstallmentEntriesSet().stream().sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<InstallmentEntry> getSortedOpenInstallmentEntries() {
        return super.getInstallmentEntriesSet().stream().filter(d -> !d.isPaid())
                .sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR).collect(Collectors.toList());
    }

    private Currency getCurrency() {
        return getPaymentPlan().getDebtAccount().getFinantialInstitution().getCurrency();
    }

    public boolean isOverdue() {
        return isOverdue(LocalDate.now());
    }

    public boolean isOverdue(LocalDate date) {
        return !isPaid() && getDueDate().isBefore(date);
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }

    public static LocalizedString installmentDescription(int installmentNumber, String paymentPlanId) {
        Map<String, String> values = new HashMap<>();
        values.put("installmentNumber", "" + installmentNumber);
        values.put("paymentPlanId", paymentPlanId);

        PaymentPlanSettings activeInstance = PaymentPlanSettings.getActiveInstance();
        if (activeInstance == null) {
            throw new RuntimeException("error.paymentPlanBean.paymentPlanSettings.required");
        }

        LocalizedString installmentDescriptionFormat = PaymentPlanSettings.getActiveInstance().getInstallmentDescriptionFormat();

        LocalizedString ls = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            ls = ls.with(locale, StrSubstitutor.replace(installmentDescriptionFormat.getContent(locale), values));
        }

        return ls;
    }
}
