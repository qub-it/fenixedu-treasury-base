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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.FenixFramework;

public class InstallmentSettlementEntry extends InstallmentSettlementEntry_Base {

    public InstallmentSettlementEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private InstallmentSettlementEntry(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
            BigDecimal debtAmount) {
        this();
        setSettlementEntry(settlementEntry);
        setInstallmentEntry(installmentEntry);
        setAmount(debtAmount);
        checkRules();
    }

    private void checkRules() {
        if (getSettlementEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.settlementEntry.required");
        }

        if (getAmount() == null || !TreasuryConstants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.amount.must.be.positive");
        }

        if (getInstallmentEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.installmentEntry.required");
        }

        // Verifiy is installment settlement entry is duplicated
        // by checking if some instance exists for the same installmentEntry and settlementEntry
        if (find(super.getInstallmentEntry(), super.getSettlementEntry()).count() > 1) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.entry.is.duplicated");
        }

        // The sum of all installment settlement entries (of not annuled payment plans)
        // should not overflow settlement entry total amount
        BigDecimal sumOfInstallmentSettlementEntries = getSettlementEntry().getInstallmentSettlementEntriesSet().stream()
                .map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (TreasuryConstants.isGreaterThan(sumOfInstallmentSettlementEntries, getSettlementEntry().getAmount())) {
            throw new TreasuryDomainException(
                    "error.InstallmentSettlementEntry.sum.of.installmentSettlementEntries.exceed.settlement.entry.amount");
        }

    }

    public static InstallmentSettlementEntry create(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
            BigDecimal debtAmount) {
        return new InstallmentSettlementEntry(installmentEntry, settlementEntry, debtAmount);
    }

    /**
     * Just invoke with the creation of settlement entry, settling the debitEntry instead installmentEntry
     *
     * @param settlementEntry
     * @return
     */
    public static Set<InstallmentSettlementEntry> settleInstallmentEntriesOfDebitEntry(SettlementEntry settlementEntry) {
        if (!settlementEntry.getInvoiceEntry().isDebitNoteEntry()) {
            throw new TreasuryDomainException(
                    "error.InstallmentSettlementEntry.settleForDebitEntry.expecting.settlementEntry.forDebitEntry");
        }

        if (!settlementEntry.getInstallmentSettlementEntriesSet().isEmpty()) {
            throw new TreasuryDomainException(
                    "error.InstallmentSettlementEntry.settlementEntry.already.has.installmentSettlementEntries");
        }

        Set<InstallmentSettlementEntry> result = new HashSet<>();
        DebitEntry debitEntry = (DebitEntry) settlementEntry.getInvoiceEntry();

        BigDecimal rest = settlementEntry.getAmount();
        for (InstallmentEntry installmentEntry : debitEntry.getSortedOpenInstallmentEntries()) {
            if (!TreasuryConstants.isPositive(rest)) {
                break;
            }

            if (installmentEntry.isPaid()) {
                continue;
            }

            BigDecimal debtAmount =
                    rest.compareTo(installmentEntry.getOpenAmount()) > 0 ? installmentEntry.getOpenAmount() : rest;
            rest = rest.subtract(debtAmount);
            result.add(InstallmentSettlementEntry.create(installmentEntry, settlementEntry, debtAmount));

            installmentEntry.getInstallment().getPaymentPlan().tryClosePaymentPlanByPaidOff();
        }

        return result;
    }

    public static Stream<InstallmentSettlementEntry> find(InstallmentEntry installmentEntry, SettlementEntry settlementEntry) {
        return installmentEntry.getInstallmentSettlementEntriesSet().stream()
                .filter(i -> i.getSettlementEntry() == settlementEntry);
    }

    public static Optional<InstallmentSettlementEntry> findUnique(InstallmentEntry installmentEntry,
            SettlementEntry settlementEntry) {
        return find(installmentEntry, settlementEntry).findFirst();
    }

}
