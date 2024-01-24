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
package org.fenixedu.treasury.domain.payments;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentTransaction extends PaymentTransaction_Base {

    public PaymentTransaction() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    protected PaymentTransaction(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate, BigDecimal paidAmount,
            Set<SettlementNote> settlementNotes) {
        this();

        if (isTransactionDuplicate(transactionId)) {
            throw new TreasuryDomainException("error.PaymentTransaction.transaction.duplicate", transactionId);
        }

        this.init(paymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);

        checkRules();
    }

    protected void init(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate, BigDecimal paidAmount,
            Set<SettlementNote> settlementNotes) {

        setPaymentRequest(paymentRequest);
        setPaymentDate(paymentDate);
        setTransactionId(transactionId);
        setPaidAmount(paidAmount);

        getSettlementNotesSet().addAll(settlementNotes);
    }

    public void checkRules() {

        if (getPaymentRequest() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paymentRequest.required");
        }

        if (getPaymentDate() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paymentDate.required");
        }

        if (getPaidAmount() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paidAmount.required");
        }

        if (!TreasuryConstants.isPositive(getPaidAmount())) {
            throw new TreasuryDomainException("error.PaymentTransaction.paidAmount.invalid");
        }

        if (findByTransactionId(getTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentTransaction.transaction.duplicate", getTransactionId());
        }
    }

    public void delete() {
        super.setDomainRoot(null);

        super.setPaymentRequest(null);
        super.setPaymentRequestLog(null);
        super.getSettlementNotesSet().clear();

        super.deleteDomainObject();
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends PaymentTransaction> findAll() {
        return FenixFramework.getDomainRoot().getPaymentTransactionsSet().stream();
    }

    public static Stream<? extends PaymentTransaction> findByTransactionId(String transactionId) {
        return findAll().filter(t -> transactionId.equalsIgnoreCase(t.getTransactionId()));
    }

    public static boolean isTransactionDuplicate(String transactionId) {
        return findByTransactionId(transactionId).findAny().isPresent();
    }

    public static PaymentTransaction create(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate,
            BigDecimal paidAmount, Set<SettlementNote> settlementNotes) {
        return new PaymentTransaction(paymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);
    }
}
