package org.fenixedu.treasury.services.payments.paymentscodegenerator;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

@Deprecated
public class SequentialPaymentCodeGenerator implements IPaymentCodeGenerator {

    private final PaymentCodePool referenceCodePool;

    public SequentialPaymentCodeGenerator(PaymentCodePool pool) {
        super();
        this.referenceCodePool = pool;
    }

    private static final String CODE_FILLER = "0";
    private static final int NUM_CONTROL_DIGITS = 2;
    private static final int NUM_SEQUENTIAL_NUMBERS = 7;

    protected Set<PaymentReferenceCode> allPaymentCodes() {
        return this.referenceCodePool.getPaymentReferenceCodesSet();
    }

    @Atomic
    public PaymentReferenceCode generateNewCodeFor(BigDecimal amount, LocalDate validFrom, LocalDate validTo,
            boolean useFixedAmount) {
        return generateNewCodeFor(amount, validFrom, validTo, useFixedAmount, false);
    }

    @Atomic
    public PaymentReferenceCode generateNewCodeFor(BigDecimal amount, LocalDate validFrom, LocalDate validTo,
            boolean useFixedAmount, final boolean forceGeneration) {

        if (!forceGeneration) {
            // First find unused payment code reference
            for (final PaymentReferenceCode paymentReferenceCode : this.referenceCodePool.getPaymentReferenceCodesSet()) {
                if (!paymentReferenceCode.isNew()) {
                    continue;
                }

                // Check if is associated with debt account
                //if(paymentReferenceCode.getDebtAccount() != null) {
                //    continue;
                //}

                if (TreasuryConstants.isGreaterThan(amount, paymentReferenceCode.getMaxAmount())) {
                    continue;
                }

                if (TreasuryConstants.isLessThan(amount, paymentReferenceCode.getMinAmount())) {
                    continue;
                }

                if (paymentReferenceCode.getTargetPayment() != null) {
                    continue;
                }

                if (validTo != null && !paymentReferenceCode.getValidInterval().contains(validTo.toDateTimeAtStartOfDay())) {
                    continue;
                } else if (!paymentReferenceCode.getValidInterval().contains(new DateTime())) {
                    continue;
                }

                if (validFrom != null && !paymentReferenceCode.getValidInterval().contains(validFrom.toDateTimeAtStartOfDay())) {
                    continue;
                } else if (!paymentReferenceCode.getValidInterval().contains(new DateTime())) {
                    continue;
                }

                paymentReferenceCode.setPayableAmount(amount);
                return paymentReferenceCode;
            }
        }

        if (!canGenerateNewCode(forceGeneration)) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        final Long nextSequentialNumber = referenceCodePool.getAndIncrementNextReferenceCode();

        String sequentialNumberPadded =
                StringUtils.leftPad(String.valueOf(nextSequentialNumber), NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
        String controDigitsPadded =
                StringUtils.leftPad(String.valueOf(new Random().nextInt(99)), NUM_CONTROL_DIGITS, CODE_FILLER);

        String referenceCodeString = sequentialNumberPadded + controDigitsPadded;

        BigDecimal minAmount = referenceCodePool.getMinAmount();
        BigDecimal maxAmount = referenceCodePool.getMaxAmount();
        if (useFixedAmount) {
            minAmount = amount;
            maxAmount = amount;
        } else {
            //Correct max amount if needed
            if (TreasuryConstants.isGreaterThan(amount, maxAmount)) {
                maxAmount = amount;
            }
        }

        PaymentReferenceCode newPaymentReference = PaymentReferenceCode.create(referenceCodeString, validFrom, validTo,
                PaymentReferenceCodeStateType.UNUSED, referenceCodePool, minAmount, maxAmount);

        newPaymentReference.setPayableAmount(amount);
        return newPaymentReference;
    }

    @Override
    public PaymentCodePool getReferenceCodePool() {
        return referenceCodePool;
    }

    @Override
    @Atomic
    public PaymentReferenceCode createPaymentReferenceCode(DebtAccount debtAccount, PaymentReferenceCodeBean bean) {
        final PaymentReferenceCode paymentReferenceCode = generateNewCodeFor(bean.getPaymentAmount(), bean.getValidFrom(),
                bean.getValidTo(), getIsFixedAmount(bean.getPaymentCodePool()));

        paymentReferenceCode.createPaymentTargetTo(Sets.newHashSet(bean.getSelectedDebitEntries()), bean.getPaymentAmount());
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
