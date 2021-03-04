package org.fenixedu.treasury.services.payments.paymentscodegenerator;

import java.math.BigDecimal;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.MbCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayLog;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class SibsOnlinePaymentsGatewayPaymentCodeGenerator implements IPaymentCodeGenerator {

    private PaymentCodePool paymentCodePool;

    public SibsOnlinePaymentsGatewayPaymentCodeGenerator(final PaymentCodePool paymentCodePool) {
        this.paymentCodePool = paymentCodePool;
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public PaymentReferenceCode createPaymentReferenceCode(final DebtAccount debtAccount, final PaymentReferenceCodeBean bean) {

        final BigDecimal paymentAmount = bean.getPaymentAmount();

        final PaymentReferenceCode paymentReferenceCode = generateNewCodeFor(debtAccount, paymentAmount, new LocalDate(),
                new LocalDate().plusMonths(
                        paymentCodePool.getSibsOnlinePaymentsGateway().getNumberOfMonthsToExpirePaymentReferenceCode()),
                Sets.newHashSet(bean.getSelectedDebitEntries()), Sets.newHashSet(bean.getSelectedInstallments()));


        return paymentReferenceCode;
    }

    private PaymentReferenceCode generateNewCodeFor(final DebtAccount debtAccount, final BigDecimal amount, LocalDate validFrom,
            LocalDate validTo, Set<DebitEntry> selectedDebitEntries, Set<Installment> selectedInstallments) {

        if (!Boolean.TRUE.equals(this.paymentCodePool.getActive())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.paymentCodePool.not.active");
        }

        for (DebitEntry debitEntry : selectedDebitEntries) {
            final long activePaymentCodesOnDebitEntryCount = MultipleEntriesPaymentCode.findNewByDebitEntry(debitEntry).count()
                    + MultipleEntriesPaymentCode.findUsedByDebitEntry(debitEntry).count();
            
            if (activePaymentCodesOnDebitEntryCount >= MultipleEntriesPaymentCode.MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }

        for (Installment installment : selectedInstallments) {
            final long activePaymentCodesOnDebitEntryCount = MultipleEntriesPaymentCode.findNewByInstallment(installment).count()
                    + MultipleEntriesPaymentCode.findUsedByInstallment(installment).count();

            if (activePaymentCodesOnDebitEntryCount >= MultipleEntriesPaymentCode.MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        installment.getDescription().getContent());
            }
        }
            
        final SibsOnlinePaymentsGateway sibsGateway = this.paymentCodePool.getSibsOnlinePaymentsGateway();
        final String merchantTransactionId = sibsGateway.generateNewMerchantTransactionId();
            
        final SibsOnlinePaymentsGatewayLog log = createLog(sibsGateway, debtAccount);
        try {
        FenixFramework.atomic(() -> {
            log.saveMerchantTransactionId(merchantTransactionId);
            log.logRequestSendDate();
        });
        
        final MbCheckoutResultBean checkoutResultBean =
                sibsGateway.generateMBPaymentReference(amount, validFrom.toDateTimeAtStartOfDay(),
                        validTo.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1), merchantTransactionId);


            final String sibsReferenceId = checkoutResultBean.getTransactionId();
            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(checkoutResultBean.getTransactionId(), checkoutResultBean.isOperationSuccess(),
                        false, checkoutResultBean.getPaymentGatewayResultCode(),
                        checkoutResultBean.getOperationResultDescription());
                log.saveRequestAndResponsePayload(checkoutResultBean.getRequestLog(), checkoutResultBean.getResponseLog());
                log.savePaymentTypeAndBrand(
                        checkoutResultBean.getPaymentType() != null ? checkoutResultBean.getPaymentType().name() : null,
                        checkoutResultBean.getPaymentBrand() != null ? checkoutResultBean.getPaymentBrand().name() : null);
            });


            if (!checkoutResultBean.isOperationSuccess()) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
            }

            final String paymentCode = checkoutResultBean.getPaymentReference();

            if (Strings.isNullOrEmpty(paymentCode)) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.reference.not.empty");
            }
            
            if (PaymentReferenceCode.findByReferenceCode(this.paymentCodePool.getEntityReferenceCode(), paymentCode,
                    this.paymentCodePool.getFinantialInstitution()).count() >= 1) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
            }

            if (!StringUtils.isEmpty(merchantTransactionId)) {
                if (PaymentReferenceCode.findBySibsMerchantTransactionId(merchantTransactionId).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.sibsMerchantTransaction.found.duplicated");
                }
            }

            if (!StringUtils.isEmpty(sibsReferenceId)) {
                if (PaymentReferenceCode.findBySibsReferenceId(sibsReferenceId).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.sibsReferenceId.found.duplicated");
                }
            }
            
            return createPaymentReferenceCodeInstance(amount, validFrom, validTo, log, paymentCode, merchantTransactionId,
                    sibsReferenceId, selectedDebitEntries, selectedInstallments);
        } catch (final Exception e) {
            final boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            saveExceptionLog(log, e, isOnlinePaymentsGatewayException);

            if (e instanceof TreasuryDomainException) {
                throw (TreasuryDomainException) e;
            } else {
                String message =
                        isOnlinePaymentsGatewayException ? "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication" : "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.unknown";

                throw new TreasuryDomainException(e, message);
            }

        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void saveExceptionLog(final SibsOnlinePaymentsGatewayLog log, final Exception e,
            final boolean isOnlinePaymentsGatewayException) {
        log.logRequestReceiveDateAndData(null, false, false, null, null);
        log.markExceptionOccuredAndSaveLog(e);

        if (isOnlinePaymentsGatewayException) {
            log.saveRequestAndResponsePayload(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog(),
                    ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private PaymentReferenceCode createPaymentReferenceCodeInstance(final BigDecimal amount, final LocalDate validFrom,
            final LocalDate validTo, final SibsOnlinePaymentsGatewayLog log, final String paymentCode,
            final String sibsMerchantTransactionId, final String sibsReferenceId, final Set<DebitEntry> selectedDebitEntries,
            Set<Installment> selectedInstallments) throws Exception {
        log.savePaymentCode(paymentCode);

        PaymentReferenceCode paymentReferenceCode = PaymentReferenceCode.createForSibsOnlinePaymentGateway(paymentCode, validFrom,
                validTo, PaymentReferenceCodeStateType.USED, this.paymentCodePool, amount, sibsMerchantTransactionId,
                sibsReferenceId);

        paymentReferenceCode.createPaymentTargetTo(selectedDebitEntries, selectedInstallments, amount);
        return paymentReferenceCode;
    }

    @Atomic(mode = TxMode.WRITE)
    private SibsOnlinePaymentsGatewayLog createLog(final SibsOnlinePaymentsGateway sibsGateway, final DebtAccount debtAccount) {
        return SibsOnlinePaymentsGatewayLog.createLogForRequestPaymentCode(sibsGateway, debtAccount);
    }

    @Override
    public PaymentCodePool getReferenceCodePool() {
        return this.paymentCodePool;
    }

    @Override
    public boolean isSibsMerchantTransactionAndReferenceIdRequired() {
        return true;
    }

}
