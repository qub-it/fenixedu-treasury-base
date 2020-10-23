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

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InstallmentEntry extends InstallmentEntry_Base {

    public static final Comparator<? super InstallmentEntry> COMPARE_BY_DEBIT_ENTRY_COMPARATOR = (m1, m2) -> {
        return (m1.getInstallment().getDueDate().compareTo(m2.getInstallment().getDueDate()) * 100)
                + (DebitEntry.COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN.compare(m1.getDebitEntry(), m2.getDebitEntry()) * 10)
                + m1.getExternalId().compareTo(m2.getExternalId());
    };

    public InstallmentEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private InstallmentEntry(DebitEntry debitEntry, BigDecimal debitAmount, Installment installment) {
        this();
        setDebitEntry(debitEntry);
        setAmount(debitAmount);
        setInstallment(installment);
        checkRules();
    }

    private void checkRules() {
        if (getDebitEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentEntry.debitEntry.required");
        }
        if (getAmount() == null || !TreasuryConstants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.InstallmentEntry.getAmount.must.be.positive");
        }
        if (getInstallment() == null) {
            throw new TreasuryDomainException("error.InstallmentEntry.installment.required");
        }
//        if (getDebitEntry().getDebitNote() == null || !getDebitEntry().getDebitNote().isClosed()) {
//            throw new TreasuryDomainException("error.InstallmentEntry.debitEntry.require.closed.debitNote");
//        }
    }

    @Atomic
    public static InstallmentEntry create(DebitEntry debitEntry, BigDecimal debitAmount, Installment installment) {
        return new InstallmentEntry(debitEntry, debitAmount, installment);
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setDebitEntry(null);
        deleteDomainObject();
    }

    public BigDecimal getPaidAmount() {
        return getInstallmentSettlementEntriesSet().stream().filter(i -> !i.getSettlementEntry().getSettlementNote().isAnnulled())
                .map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOpenAmount() {
        return getCurrency().getValueWithScale(getAmount().subtract(getPaidAmount()));
    }

    private Currency getCurrency() {
        return getDebitEntry().getCurrency();
    }

    public boolean isPaid() {
        return TreasuryConstants.isZero(getOpenAmount());
    }
}
