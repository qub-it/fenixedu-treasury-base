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
package org.fenixedu.treasury.services.payments.paymentscodegenerator;

import java.math.BigDecimal;
import java.util.Set;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

@Deprecated
public class SequentialPaymentWithCheckDigitCodeGenerator implements IPaymentCodeGenerator {

    private final PaymentCodePool referenceCodePool;

    @Override
    public PaymentCodePool getReferenceCodePool() {
        return referenceCodePool;
    }

    public SequentialPaymentWithCheckDigitCodeGenerator(PaymentCodePool pool) {
        super();
        this.referenceCodePool = pool;
    }

    @Atomic
    public PaymentReferenceCode generateNewCodeFor(final BigDecimal amount, LocalDate validFrom, LocalDate validTo,
            boolean useFixedAmount) {
        return generateNewCodeFor(amount, validFrom, validTo, useFixedAmount, false);
    }

    @Atomic
    public PaymentReferenceCode generateNewCodeFor(final BigDecimal amount, LocalDate validFrom, LocalDate validTo,
            boolean useFixedAmount, final boolean forceGeneration) {

        if (validFrom == null) {
            validFrom = referenceCodePool.getValidFrom();
        }
        if (validTo == null) {
            validTo = referenceCodePool.getValidTo();
        }

        Long nextReferenceCode = referenceCodePool.getAndIncrementNextReferenceCode();
        if (nextReferenceCode > referenceCodePool.getMaxReferenceCode()) {
            //The pool is "OVER"... Try to get the first unused code

            PaymentReferenceCode availableReferenceCode = this.referenceCodePool.getPaymentReferenceCodesSet().stream()
                    .filter(x -> x.isAvailableForReuse()).findFirst().orElse(null);
            if (availableReferenceCode != null) {
                nextReferenceCode = Long.parseLong(availableReferenceCode.getReferenceCodeWithoutCheckDigits());
            } else {
                throw new TreasuryDomainException("error.PaymentCodeGenerator.not.paymentreferences.available.in.pool");
            }
        }

        final String referenceCodeString = CheckDigitGenerator
                .generateReferenceCodeWithCheckDigit(referenceCodePool.getEntityReferenceCode(), "" + nextReferenceCode, amount);

        BigDecimal minAmount = referenceCodePool.getMinAmount();
        BigDecimal maxAmount = referenceCodePool.getMaxAmount();
        if (useFixedAmount) {
            minAmount = amount;
            maxAmount = amount;
        }

        PaymentReferenceCode newPaymentReference = PaymentReferenceCode.create(referenceCodeString, validFrom, validTo,
                PaymentReferenceCodeStateType.UNUSED, referenceCodePool, minAmount, maxAmount);
        newPaymentReference.setPayableAmount(amount);

        return newPaymentReference;
    }

    protected Set<PaymentReferenceCode> allPaymentCodes(PaymentCodePool referenceCodePool) {
        return referenceCodePool.getPaymentReferenceCodesSet();
    }

    @Override
    @Atomic
    public PaymentReferenceCode createPaymentReferenceCode(final DebtAccount debtAccount, final PaymentReferenceCodeBean bean) {
	    
        for (DebitEntry debitEntry : bean.getSelectedDebitEntries()) {
            final long activePaymentCodesOnDebitEntryCount = MultipleEntriesPaymentCode.findNewByDebitEntry(debitEntry).count()
                    + MultipleEntriesPaymentCode.findUsedByDebitEntry(debitEntry).count();

            if (activePaymentCodesOnDebitEntryCount >= MultipleEntriesPaymentCode.MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }

        for (Installment installment : bean.getSelectedInstallments()) {
            final long activePaymentCodesOnDebitEntryCount = MultipleEntriesPaymentCode.findNewByInstallment(installment).count()
                    + MultipleEntriesPaymentCode.findUsedByInstallment(installment).count();

            if (activePaymentCodesOnDebitEntryCount >= MultipleEntriesPaymentCode.MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        installment.getDescription().getContent());
            }
        }
	    
        final PaymentReferenceCode paymentReferenceCode =
                generateNewCodeFor(
                                bean.getPaymentAmount(), bean.getValidFrom(), bean.getValidTo(),
                                getIsFixedAmount(bean.getPaymentCodePool()));

        paymentReferenceCode.createPaymentTargetTo(Sets.newHashSet(bean.getSelectedDebitEntries()),
			Sets.newHashSet(bean.getSelectedInstallments()), bean.getPaymentAmount());
        return paymentReferenceCode;
    }

    private boolean getIsFixedAmount(DigitalPaymentPlatform platform) {
        //When using checkdigit, it's fixed amount 
        //HACK: there is also an option without check digit for fixed amount
        return ((SibsPaymentCodePool) platform).getUseCheckDigit();
    }

    @Override
    public boolean isSibsMerchantTransactionAndReferenceIdRequired() {
        return false;
    }

}
