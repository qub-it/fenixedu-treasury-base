/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 *
 *
 *
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
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
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
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
			// The pool is "OVER"... Try to get the first unused code

			PaymentReferenceCode availableReferenceCode = this.referenceCodePool.getPaymentReferenceCodesSet().stream()
					.filter(x -> x.isAvailableForReuse()).findFirst().orElse(null);
			if (availableReferenceCode != null) {
				nextReferenceCode = Long.parseLong(availableReferenceCode.getReferenceCodeWithoutCheckDigits());
			} else {
				throw new TreasuryDomainException("error.PaymentCodeGenerator.not.paymentreferences.available.in.pool");
			}
		}

		final String referenceCodeString = CheckDigitGenerator.generateReferenceCodeWithCheckDigit(
				referenceCodePool.getEntityReferenceCode(), "" + nextReferenceCode, amount);

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
	public PaymentReferenceCode createPaymentReferenceCode(final DebtAccount debtAccount,
			final PaymentReferenceCodeBean bean) {
	    
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
	    
		final PaymentReferenceCode paymentReferenceCode = generateNewCodeFor(bean.getPaymentAmount(),
				bean.getBeginDate(), bean.getEndDate(), bean.getPaymentCodePool().getIsFixedAmount());

		paymentReferenceCode.createPaymentTargetTo(Sets.newHashSet(bean.getSelectedDebitEntries()),
				Sets.newHashSet(bean.getSelectedInstallments()), bean.getPaymentAmount());
		return paymentReferenceCode;
	}

	@Override
	public boolean isSibsMerchantTransactionAndReferenceIdRequired() {
		return false;
	}

}
