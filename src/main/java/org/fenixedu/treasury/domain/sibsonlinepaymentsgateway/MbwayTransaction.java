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
package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class MbwayTransaction extends MbwayTransaction_Base {

    public MbwayTransaction() {
        super();
        setCreationDate(new DateTime());
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public MbwayTransaction(MbwayPaymentRequest mbwayPaymentRequest, String sibsTransactionId, BigDecimal amount,
            DateTime paymentDate, final Set<SettlementNote> settlementNotes) {
        this();

        setMbwayPaymentRequest(mbwayPaymentRequest);
        setAmount(amount);
        setSibsTransactionId(sibsTransactionId);
        setPaymentDate(paymentDate);
        getSettlementNotesSet().addAll(settlementNotes);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.domainRoot.required");
        }

        if (getMbwayPaymentRequest() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.mbwayPaymentRequest.required");
        }

        if (StringUtils.isEmpty(getSibsTransactionId())) {
            throw new TreasuryDomainException("error.MbwayTransaction.sibsTransactionId.required");
        }

        if (getPaymentDate() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.paymentDate.required");
        }

        if (getSettlementNotesSet().isEmpty()) {
            throw new TreasuryDomainException("error.MbwayTransaction.paymentDate.required");
        }

        if (getAmount() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.amount.required");
        }

        if (findBySibsTransactionId(getSibsTransactionId()).count() >= 2) {
            throw new TreasuryDomainException("error.MbwayTransaction.sibsTransactionId.already.registered");
        }

    }

    /* ************ */
    /* * SERVICES * */
    /* ************ */

    public static MbwayTransaction create(final MbwayPaymentRequest mbwayPaymentRequest, final String sibsTransactionId,
            final BigDecimal amount, final DateTime paymentDate, final Set<SettlementNote> settlementNotes) {
        final MbwayTransaction mbwayTransaction =
                new MbwayTransaction(mbwayPaymentRequest, sibsTransactionId, amount, paymentDate, settlementNotes);

        return mbwayTransaction;
    }

    public static Stream<MbwayTransaction> findAll() {
        return FenixFramework.getDomainRoot().getMbwayPaymentTransactionsSet().stream();
    }

    public static Stream<MbwayTransaction> find(final MbwayPaymentRequest mbwayPaymentRequest) {
        return mbwayPaymentRequest.getMbwayTransactionsSet().stream();
    }

    public static Stream<MbwayTransaction> findBySibsTransactionId(final String sibsTransactionId) {
        return findAll().filter(t -> t.getSibsTransactionId().equals(sibsTransactionId));
    }

    public static boolean isTransactionProcessingDuplicate(final String sibsTransactionId) {
        return findBySibsTransactionId(sibsTransactionId).count() > 0;
    }

}