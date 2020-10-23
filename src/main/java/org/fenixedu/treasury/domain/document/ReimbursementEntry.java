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
import java.util.Collection;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ReimbursementEntry extends ReimbursementEntry_Base {

    protected ReimbursementEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ReimbursementEntry(final SettlementNote settlementNote, final PaymentMethod paymentMethod,
            final BigDecimal reimbursedAmount, final String reimbursementMethodId) {
        this();
        init(settlementNote, paymentMethod, reimbursedAmount, reimbursementMethodId);
    }

    protected void init(final SettlementNote settlementNote, final PaymentMethod paymentMethod, final BigDecimal reimbursedAmount,
            final String reimbursementMethodId) {
        setSettlementNote(settlementNote);
        setPaymentMethod(paymentMethod);
        setReimbursedAmount(reimbursedAmount);
        setReimbursementMethodId(reimbursementMethodId);

        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getSettlementNote() == null) {
            throw new TreasuryDomainException("error.ReimbursementEntry.settlementNote.required");
        }

        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.ReimbursementEntry.paymentMethod.required");
        }

        //CHANGE_ME In order to validate UNIQUE restrictions
        //if (findBySettlementNote(getSettlementNote().count()>1)
        //{
        //	throw new TreasuryDomainException("error.ReimbursementEntry.settlementNote.duplicated");
        //}	
        //if (findByPaymentMethod(getPaymentMethod().count()>1)
        //{
        //	throw new TreasuryDomainException("error.ReimbursementEntry.paymentMethod.duplicated");
        //}	
        //if (findByReimbursedAmount(getReimbursedAmount().count()>1)
        //{
        //	throw new TreasuryDomainException("error.ReimbursementEntry.reimbursedAmount.duplicated");
        //}	
    }

    @Atomic
    public void edit(final SettlementNote settlementNote, final PaymentMethod paymentMethod,
            final java.math.BigDecimal reimbursedAmount) {
        setSettlementNote(settlementNote);
        setPaymentMethod(paymentMethod);
        setReimbursedAmount(reimbursedAmount);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.ReimbursementEntry.cannot.delete");
        }
        setDomainRoot(null);
        setPaymentMethod(null);
        setSettlementNote(null);

        deleteDomainObject();
    }

    @Atomic
    public static ReimbursementEntry create(final SettlementNote settlementNote, final PaymentMethod paymentMethod,
            final java.math.BigDecimal reimbursedAmount, final String reimbursementMethodId) {
        return new ReimbursementEntry(settlementNote, paymentMethod, reimbursedAmount, reimbursementMethodId);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<ReimbursementEntry> findAll() {
        return FenixFramework.getDomainRoot().getReimbursementEntriesSet().stream();
    }

    public static Stream<ReimbursementEntry> findBySettlementNote(final SettlementNote settlementNote) {
        return settlementNote.getReimbursementEntriesSet().stream();
    }

    public static Stream<ReimbursementEntry> findByPaymentMethod(final PaymentMethod paymentMethod) {
        return paymentMethod.getReimbursementEntriesSet().stream();
    }

    public static Stream<ReimbursementEntry> findByReimbursedAmount(final java.math.BigDecimal reimbursedAmount) {
        return findAll().filter(i -> reimbursedAmount.equals(i.getReimbursedAmount()));
    }

}
