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
package org.fenixedu.treasury.domain.meowallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.IMbwayPaymentPlatformService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletCallbackBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletCheckoutBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletPaymentBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletPaymentItemBean;
import org.fenixedu.treasury.services.payments.meowallet.MeoWalletService;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class MeoWallet extends MeoWallet_Base
        implements IMbwayPaymentPlatformService, ISibsPaymentCodePoolService, IForwardPaymentPlatformService {

    public static final String CONTROLLER_URL = "/treasury/document/forwardpayments/sibsonlinepaymentsgateway";
    private static final String RETURN_FORWARD_PAYMENT_URI = "/returnforwardpayment";
    public static final String RETURN_FORWARD_PAYMENT_URL = CONTROLLER_URL + RETURN_FORWARD_PAYMENT_URI;

    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public MeoWallet() {
        super();
    }

    public MeoWallet(FinantialInstitution finantialInstitution, String name, boolean active, String endpointUrl,
            String authorizationAPIToken) {
        this();

        this.init(finantialInstitution, name, active);

        setEndpointUrl(endpointUrl);
        setAuthorizationAPIToken(authorizationAPIToken);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbWayPaymentMethod());

        checkRules();
    }

    public static MeoWallet create(FinantialInstitution finantialInstitution, String name, boolean active, String endpointUrl,
            String authorizationAPIToken) {
        return new MeoWallet(finantialInstitution, name, active, endpointUrl, authorizationAPIToken);
    }

    private void checkRules() {
    }

    @Override
    @pt.ist.fenixframework.Atomic
    public void delete() {
        super.delete();
        super.deleteDomainObject();
    }

    public MeoWalletService getMeoWalletService() {
        return new MeoWalletService(getEndpointUrl(), getAuthorizationAPIToken());
    }

    public String generateNewMerchantTransactionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Atomic(mode = TxMode.WRITE)
    private static MeoWalletLog createLogForMbwayPaymentRequest(String extInvoiceId, String extCustomerId) {
        return MeoWalletLog.createForMbwayPaymentRequest(extInvoiceId, extCustomerId);
    }

    @Atomic(mode = TxMode.WRITE)
    public static MeoWalletLog createLogForSibsPaymentRequest(String extInvoiceId, String extCustomerId) {
        return MeoWalletLog.createForSibsPaymentRequest(extInvoiceId, extCustomerId);
    }

    /**
     * MbwayRequest
     */
    @Override
    @Atomic(mode = TxMode.READ)
    public MbwayRequest createMbwayRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries, Set<Installment> installments,
            String countryPrefix, String localPhoneNumber) {

        if (PaymentRequest.getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        if (StringUtils.isEmpty(localPhoneNumber)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.required");
        }

        if (!localPhoneNumber.matches("\\d+")) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.format.required");
        }

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(e -> e.getOpenAmountWithInterests()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        String merchantTransactionId = generateNewMerchantTransactionId();

        MeoWalletLog log = createLogForMbwayPaymentRequest(merchantTransactionId, debtAccount.getCustomer().getExternalId());;

        List<MeoWalletPaymentItemBean> items = new ArrayList<>();
        debitEntries.stream()
                .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription(), d.getOpenAmountWithInterests())));
        installments.stream()
                .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription().getContent(), d.getOpenAmount())));

        MeoWalletPaymentBean payment = MeoWalletPaymentBean.createMbwayPaymentBean(payableAmount, merchantTransactionId,
                debtAccount.getCustomer().getExternalId(), debtAccount.getCustomer().getName(), localPhoneNumber, items);

        try {
            MbwayRequest mbwayPaymentRequest = FenixFramework.atomic(() -> {
                MbwayRequest request = MbwayRequest.create(this, debtAccount, debitEntries, installments, localPhoneNumber,
                        payableAmount, merchantTransactionId);
                log.logRequestSendDate();
                log.setPaymentRequest(request);
                return request;
            });

            final MeoWalletPaymentBean paymentBean = getMeoWalletService().generateMbwayReference(payment);

            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(paymentBean.getId(), paymentBean.getType(), paymentBean.getMethod(),
                        paymentBean.getAmount(), paymentBean.getStatus(), !paymentBean.getStatus().equals(STATUS_FAIL));

                log.saveRequest(paymentBean.getRequestLog());
                log.saveResponse(paymentBean.getResponseLog());
                log.setStateCode(mbwayPaymentRequest.getState().name());
                log.setStateDescription(mbwayPaymentRequest.getState().getDescriptionI18N());

                mbwayPaymentRequest.setTransactionId(paymentBean.getId());

            });

            return mbwayPaymentRequest;

        } catch (Exception e) {
            boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            FenixFramework.atomic(() -> {

                log.logRequestReceiveDateAndData(null, null, null, null, null, false);
                log.logException(e);

                if (isOnlinePaymentsGatewayException) {
                    OnlinePaymentsGatewayCommunicationException onlineException = (OnlinePaymentsGatewayCommunicationException) e;
                    log.saveRequest(onlineException.getRequestLog());
                    log.saveResponse(onlineException.getResponseLog());
                }
            });

            if (e instanceof TreasuryDomainException) {
                throw (TreasuryDomainException) e;
            } else {
                final String message = "error.MeoWallet.generateNewCodeFor."
                        + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

                throw new TreasuryDomainException(e, message);
            }
        }
    }

    @Override
    public PaymentTransaction processMbwayTransaction(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        return processMbwayTransaction((MeoWalletLog) log, bean);
    }

    @Atomic(mode = TxMode.READ)
    public PaymentTransaction processMbwayTransaction(MeoWalletLog log, DigitalPlatformResultBean bean) {
        MbwayRequest request = (MbwayRequest) log.getPaymentRequest();

        if (!bean.getMerchantTransactionId().equals(request.getMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });

            return null;
        }

        try {
            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            PaymentTransaction transaction = null;
            if (STATUS_COMPLETED.equals(bean.getPaymentResultCode())) {
                try {
                    transaction = FenixFramework.atomic(() -> {
                        log.setStateCode(PaymentReferenceCodeStateType.PROCESSED.name());
                        log.savePaymentInfo(paidAmount, paymentDate);

                        final Set<SettlementNote> settlementNotes = request.processPayment(paidAmount, paymentDate,
                                bean.getTransactionId(), bean.getMerchantTransactionId());
                        PaymentTransaction paymentTransaction = PaymentTransaction.create(request, bean.getTransactionId(),
                                paymentDate, paidAmount, settlementNotes);

                        log.setPaymentTransaction(paymentTransaction);

                        return paymentTransaction;
                    });
                } catch (Exception e) {
                    FenixFramework.atomic(() -> log.logException(e));
                    throw new RuntimeException(e);
                }
            } else if (STATUS_FAIL.equals(bean.getPaymentResultCode())) {
                FenixFramework.atomic(() -> {
                    log.setStateCode(PaymentReferenceCodeStateType.ANNULLED.name());

                    request.anull();
                });
            }

            return transaction;
        } catch (Exception e) {
            FenixFramework.atomic(() -> {
                log.logException(e);
            });

            throw new RuntimeException(e);
        }
    }

    /**
     * SibsPaymentRequest
     */
    @Override
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments) {

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(DebitEntry::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        LocalDate now = new LocalDate();
        Set<LocalDate> map = debitEntries.stream().map(d -> d.getDueDate()).collect(Collectors.toSet());
        map.addAll(installments.stream().map(i -> i.getDueDate()).collect(Collectors.toSet()));
        LocalDate validTo = map.stream().max(LocalDate::compareTo).orElse(now);

        if (validTo.isBefore(now)) {
            validTo = now;
        }

        return createPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequestWithInterests(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate interestsCalculationDate) {
        BigDecimal payableAmountDebitEntries = debitEntries.stream()
                .map(d -> d.getOpenAmountWithInterestsAtDate(interestsCalculationDate)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        LocalDate now = new LocalDate();
        Set<LocalDate> map = debitEntries.stream().map(d -> d.getDueDate()).collect(Collectors.toSet());
        map.addAll(installments.stream().map(i -> i.getDueDate()).collect(Collectors.toSet()));
        LocalDate validTo = map.stream().max(LocalDate::compareTo).orElse(now);

        if (validTo.isBefore(now)) {
            validTo = now;
        }

        return createPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    private SibsPaymentRequest createPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate validTo, BigDecimal payableAmount) {

        if (!isActive()) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.paymentCodePool.not.active");
        }

        if (PaymentRequest.getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        checkMaxActiveSibsPaymentRequests(debitEntries);

        String merchantTransactionId = generateNewMerchantTransactionId();

        List<MeoWalletPaymentItemBean> items = new ArrayList<>();
        debitEntries.stream()
                .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription(), d.getOpenAmountWithInterests())));
        installments.stream()
                .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription().getContent(), d.getOpenAmount())));

        MeoWalletPaymentBean payment = MeoWalletPaymentBean.createMBPaymentBean(payableAmount, merchantTransactionId,
                debtAccount.getCustomer().getExternalId(), debtAccount.getCustomer().getName(), items);

        final MeoWalletLog log = createLogForSibsPaymentRequest(merchantTransactionId, debtAccount.getCustomer().getExternalId());

        try {

            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            MeoWalletPaymentBean paymentBean = getMeoWalletService().generateMBPaymentReference(payment);

            final String sibsReferenceId = paymentBean.getId();
            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(paymentBean.getId(), paymentBean.getType(), paymentBean.getMethod(),
                        paymentBean.getAmount(), paymentBean.getStatus(), !paymentBean.getStatus().equals(STATUS_FAIL));

                log.saveRequest(paymentBean.getRequestLog());
                log.saveResponse(paymentBean.getResponseLog());
            });

            if (StringUtils.isEmpty(paymentBean.getMb().getRef())) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.reference.not.empty");
            }

            if (SibsReferenceCode.findByReferenceCode(paymentBean.getMb().getEntity(), paymentBean.getMb().getRef())
                    .count() >= 2) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
            }
            SibsPaymentRequest sibsPaymentRequest = FenixFramework.atomic(() -> {
                SibsPaymentRequest request = SibsPaymentRequest.create(MeoWallet.this, debtAccount, debitEntries, installments,
                        payableAmount, paymentBean.getMb().getEntity(), paymentBean.getMb().getRef(), merchantTransactionId,
                        sibsReferenceId);
                log.setPaymentRequest(request);

                return request;
            });

            return sibsPaymentRequest;
        } catch (final Exception e) {
            final boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            saveExceptionLog(log, e, isOnlinePaymentsGatewayException);

            if (e instanceof TreasuryDomainException) {
                throw (TreasuryDomainException) e;
            } else {
                String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.unknown";
                if (isOnlinePaymentsGatewayException) {
                    message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication";
                }

                throw new TreasuryDomainException(e, message);
            }
        }

    }

    @Override
    public PaymentTransaction processPaymentReferenceCodeTransaction(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        return processPaymentReferenceCodeTransaction((MeoWalletLog) log, bean);
    }

    @Atomic(mode = TxMode.READ)
    public PaymentTransaction processPaymentReferenceCodeTransaction(final MeoWalletLog log, DigitalPlatformResultBean bean) {
        SibsPaymentRequest paymentRequest = (SibsPaymentRequest) log.getPaymentRequest();
        if (!bean.getMerchantTransactionId().equals(paymentRequest.getMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.merchantTransactionId.not.equal");
        }

        String mbEntity = null;
        String mbRef = null;

        if (bean instanceof MeoWalletCallbackBean) {
            mbEntity = ((MeoWalletCallbackBean) bean).getMb_entity();
            mbRef = ((MeoWalletCallbackBean) bean).getMb_ref();
        } else if (bean instanceof MeoWalletPaymentBean) {
            mbEntity = ((MeoWalletPaymentBean) bean).getMb().getEntity();
            mbRef = ((MeoWalletPaymentBean) bean).getMb().getRef();
        }

        if (!mbEntity.equals(paymentRequest.getEntityReferenceCode())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.entityReferenceCode.not.equal");
        }

        if (!mbRef.equals(paymentRequest.getReferenceCode())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.referenceCode.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.setPaymentRequest(paymentRequest);
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.payment.date");
        }

        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(paymentRequest.getEntityReferenceCode(),
                paymentRequest.getReferenceCode(), paymentDate)) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        try {
            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            PaymentTransaction transaction = null;
            if (STATUS_COMPLETED.equals(bean.getPaymentResultCode())) {
                try {
                    transaction = FenixFramework.atomic(() -> {
                        PaymentTransaction processPayment =
                                paymentRequest.processPayment(paidAmount, bean.getPaymentDate(), bean.getTransactionId(), null,
                                        bean.getMerchantTransactionId(), bean.getPaymentDate(), null, true);

                        log.setStateCode(PaymentReferenceCodeStateType.PROCESSED.name());
                        log.savePaymentInfo(paidAmount, paymentDate);

                        log.setPaymentTransaction(processPayment);
                        return processPayment;
                    });
                } catch (Exception e) {
                    FenixFramework.atomic(() -> {
                        log.logException(e);
                    });

                    throw new RuntimeException(e);
                }

            } else if (STATUS_FAIL.equals(bean.getPaymentResultCode())) {
                FenixFramework.atomic(() -> {
                    log.setStateCode(PaymentReferenceCodeStateType.ANNULLED.name());

                    paymentRequest.anull();
                });
            }

            return transaction;
        } catch (Exception e) {
            FenixFramework.atomic(() -> {
                log.logException(e);
            });

            throw new RuntimeException(e);
        }
    }

    private void checkMaxActiveSibsPaymentRequests(Set<DebitEntry> debitEntries) {
        for (DebitEntry debitEntry : debitEntries) {
            long numActiveSibsPaymentRequests =
                    debitEntry.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                            .map(SibsPaymentRequest.class::cast).filter(r -> r.getState() == PaymentReferenceCodeStateType.UNUSED
                                    || r.getState() == PaymentReferenceCodeStateType.USED)
                            .count();

            if (numActiveSibsPaymentRequests >= 2) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }
    }

    /**
     * ForwardPaymentRequest
     */
    @Override
    public ForwardPaymentRequest createForwardPaymentRequest(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {
        Set<DebitEntry> debitEntries =
                bean.getIncludedInvoiceEntryBeans().stream().map(ISettlementInvoiceEntryBean::getInvoiceEntry)
                        .filter(i -> i != null).map(DebitEntry.class::cast).collect(Collectors.toSet());

        Set<Installment> installments =
                bean.getIncludedInvoiceEntryBeans().stream().filter(i -> i instanceof InstallmentPaymenPlanBean && i.isIncluded())
                        .map(InstallmentPaymenPlanBean.class::cast).map(ib -> ib.getInstallment()).collect(Collectors.toSet());

        ForwardPaymentRequest forwardPaymentRequest =
                ForwardPaymentRequest.create(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(), debitEntries, installments,
                        bean.getTotalAmountToPay(), successUrlFunction, insuccessUrlFunction);

        prepareCheckout(forwardPaymentRequest);

        return forwardPaymentRequest;
    }

    @Override
    public PaymentRequestLog log(PaymentRequest paymentRequest, String statusCode, String statusMessage, String requestBody,
            String responseBody) {
        final MeoWalletLog log = MeoWalletLog.createPaymentRequestLog(paymentRequest, paymentRequest.getCurrentState().getCode(),
                paymentRequest.getCurrentState().getLocalizedName());

        log.setExtInvoiceId(paymentRequest.getMerchantTransactionId());
        log.setMeoWalletId(paymentRequest.getTransactionId());

        log.setStatusCode(statusCode);
        log.setStatusMessage(statusMessage);

        if (!Strings.isNullOrEmpty(requestBody)) {
            log.saveRequest(requestBody);
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            log.saveResponse(responseBody);
        }

        return log;
    }

    @Atomic(mode = TxMode.READ)
    private ForwardPaymentStatusBean prepareCheckout(ForwardPaymentRequest forwardPayment) {
        String merchantTransactionId = generateNewMerchantTransactionId();

        FenixFramework.atomic(() -> {
            if (!StringUtils.isEmpty(forwardPayment.getMerchantTransactionId())) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.sibsMerchantTransactionId.already.filled");
            }

            forwardPayment.setMerchantTransactionId(merchantTransactionId);
        });

        try {
            DateTime requestSendDate = new DateTime();

            List<MeoWalletPaymentItemBean> items = new ArrayList<>();
            forwardPayment.getDebitEntriesSet().stream()
                    .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription(), d.getOpenAmountWithInterests())));
            forwardPayment.getInstallmentsSet().stream()
                    .forEach(d -> items.add(new MeoWalletPaymentItemBean(1, d.getDescription().getContent(), d.getOpenAmount())));
            MeoWalletPaymentBean paymentBean = MeoWalletPaymentBean.createForwardPaymentBean(forwardPayment.getPayableAmount(),
                    forwardPayment.getDebtAccount().getCustomer().getName(), items);
            MeoWalletCheckoutBean checkoutBean = new MeoWalletCheckoutBean(paymentBean,
                    forwardPayment.getForwardPaymentSuccessUrl(), forwardPayment.getForwardPaymentInsuccessUrl(),
                    merchantTransactionId, forwardPayment.getDebtAccount().getCustomer().getExternalId(),
                    new String[] { "UNICRE", "PAYPAL", "MBWAY", "WALLET" });

            final MeoWalletCheckoutBean resultCheckoutBean = getMeoWalletService().prepareOnlinePaymentCheckout(checkoutBean);
            FenixFramework.atomic(() -> {
                forwardPayment.setTransactionId(resultCheckoutBean.getId());
                forwardPayment.setRedirectUrl(resultCheckoutBean.getUrl_redirect());
            });
            DateTime requestReceiveDate = new DateTime();

            final ForwardPaymentStateType stateType =
                    translateForwardPaymentStateType(resultCheckoutBean.getPayment().getStatus());
            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(true, stateType,
                    resultCheckoutBean.getPayment().getStatus(), resultCheckoutBean.getPayment().getStatus(),
                    resultCheckoutBean.getRequestLog(), resultCheckoutBean.getResponseLog());

            FenixFramework.atomic(() -> {
                if (!result.isInvocationSuccess() || (result.getStateType() == ForwardPaymentStateType.REJECTED)) {
                    MeoWalletLog log = (MeoWalletLog) forwardPayment.reject("prepareCheckout",
                            resultCheckoutBean.getPayment().getStatus(), resultCheckoutBean.getPayment().getStatus(),
                            resultCheckoutBean.getRequestLog(), resultCheckoutBean.getResponseLog());

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);

                } else {
                    MeoWalletLog log = (MeoWalletLog) forwardPayment.advanceToRequestState("prepareCheckout",
                            resultCheckoutBean.getPayment().getStatus(), resultCheckoutBean.getPayment().getStatus(),
                            resultCheckoutBean.getRequestLog(), resultCheckoutBean.getResponseLog());

                    log.setOperationSuccess(result.isInvocationSuccess());
                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                }
            });

            return result;

        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                PaymentRequestLog log = forwardPayment.logException(e);
                if (!StringUtils.isEmpty(requestBody)) {
                    log.saveRequest(requestBody);

                }

                if (!StringUtils.isEmpty(responseBody)) {
                    log.saveResponse(responseBody);
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }

    }

    private ForwardPaymentStateType translateForwardPaymentStateType(final String operationResultType) {

        if (operationResultType == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayForwardImplementation.unknown.payment.state");
        }

        if (operationResultType.equals(STATUS_COMPLETED)) {
            return ForwardPaymentStateType.PAYED;
        } else if (operationResultType.equals(STATUS_FAIL)) {
            return ForwardPaymentStateType.REJECTED;
        }

        return ForwardPaymentStateType.REQUESTED;
    }

    public String getReturnURL(final ForwardPaymentRequest forwardPayment) {
        return String.format("%s%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(),
                RETURN_FORWARD_PAYMENT_URL, forwardPayment.getExternalId());
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest request) {
        return request.getRedirectUrl();
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(ForwardPaymentRequest forwardPayment) {
        try {
            MeoWalletCheckoutBean resultCheckoutBean =
                    getMeoWalletService().getForwardPaymentTransactionReportByTransactionId(forwardPayment.getCheckoutId());

            final ForwardPaymentStateType stateType =
                    translateForwardPaymentStateType(resultCheckoutBean.getPayment().getStatus());

            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(true, stateType,
                    resultCheckoutBean.getPayment().getStatus(), resultCheckoutBean.getPayment().getStatus(),
                    resultCheckoutBean.getRequestLog(), resultCheckoutBean.getResponseLog());

            result.editTransactionDetails(resultCheckoutBean.getId(), resultCheckoutBean.getPayment().getDate(),
                    resultCheckoutBean.getPayment().getAmount());

            return result;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                if (!"ERRO".equals(e.getMessage())) {
                    PaymentRequestLog log = forwardPayment.logException(e);
                    log.setOperationCode("paymentStatus");

                    if (!StringUtils.isEmpty(requestBody)) {
                        log.saveRequest(requestBody);
                    }

                    if (!StringUtils.isEmpty(responseBody)) {
                        log.saveResponse(responseBody);
                    }
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId) {
        if (specificTransactionId.isEmpty()) {
            ForwardPaymentStatusBean statusBean =
                    new ForwardPaymentStatusBean(false, forwardPayment.getState(), "N/A", "N/A", null, null);
            return new PostProcessPaymentStatusBean(statusBean, forwardPayment.getState(), false);
        }

        if (!forwardPayment.getState().isInStateToPostProcessPayment()) {
            throw new TreasuryDomainException("error.ManageForwardPayments.forwardPayment.not.created.nor.requested",
                    String.valueOf(forwardPayment.getOrderNumber()));
        }

        try {
            DateTime requestSendDate = new DateTime();

            MeoWalletCheckoutBean resultCheckoutBean =
                    getMeoWalletService().getForwardPaymentTransactionReportByTransactionId(specificTransactionId.get());

            DateTime requestReceiveDate = new DateTime();

            final ForwardPaymentStateType stateType =
                    translateForwardPaymentStateType(resultCheckoutBean.getPayment().getStatus());

            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(true, stateType,
                    resultCheckoutBean.getPayment().getStatus(), resultCheckoutBean.getPayment().getStatus(),
                    resultCheckoutBean.getRequestLog(), resultCheckoutBean.getResponseLog());

            result.editTransactionDetails(resultCheckoutBean.getId(), resultCheckoutBean.getPayment().getDate(),
                    resultCheckoutBean.getPayment().getAmount());

            if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                    .contains(result.getStateType())) {
                // Do nothing
                return new PostProcessPaymentStatusBean(result, forwardPayment.getState(), false);
            }

            // First of all save sibsTransactionId
            FenixFramework.atomic(() -> {
                forwardPayment.setTransactionId(result.getTransactionId());
            });

            PostProcessPaymentStatusBean returnBean =
                    new PostProcessPaymentStatusBean(result, forwardPayment.getState(), result.isInPayedState());

            if (result.isInPayedState()) {
                FenixFramework.atomic(() -> {
                    MeoWalletLog log = (MeoWalletLog) forwardPayment.advanceToPaidState(result.getStatusCode(),
                            result.getStatusMessage(), result.getPayedAmount(), result.getTransactionDate(),
                            result.getTransactionId(), null, result.getRequestBody(), result.getResponseBody(), "");

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    log.setMeoWalletId(result.getTransactionId());
                    log.savePaymentInfo(result.getPayedAmount(), result.getTransactionDate());
                    log.setPaymentMethod(resultCheckoutBean.getPayment().getMethod());
                });

            } else if (result.isInRejectedState()) {
                FenixFramework.atomic(() -> {
                    MeoWalletLog log = (MeoWalletLog) forwardPayment.reject("postProcessPayment", result.getStatusCode(),
                            result.getStatusMessage(), result.getRequestBody(), result.getResponseBody());

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    log.setMeoWalletId(result.getTransactionId());
                });

            }

            return returnBean;
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                PaymentRequestLog log = forwardPayment.logException(e);
                if (!StringUtils.isEmpty(requestBody)) {
                    log.saveRequest(requestBody);
                }

                if (!StringUtils.isEmpty(responseBody)) {
                    log.saveResponse(responseBody);
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void saveExceptionLog(final MeoWalletLog log, final Exception e, final boolean isOnlinePaymentsGatewayException) {
        log.logRequestReceiveDateAndData(null, null, null, null, null, false);
        log.logException(e);

        if (isOnlinePaymentsGatewayException) {
            log.saveRequest(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog());
            log.saveResponse(((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
        }
    }

    @Override
    public String getEntityReferenceCode() {
        return "undefined";
    }

    @Override
    public String getLogosJspPage() {
        return null;
    }

    @Override
    public PaymentRequestLog createLogForWebhookNotification() {
        return MeoWalletLog.createLogForWebhookNotification();
    }

    @Override
    public void fillLogForWebhookNotification(PaymentRequestLog paymentRequestLog, DigitalPlatformResultBean bean) {
        MeoWalletLog log = (MeoWalletLog) paymentRequestLog;

        FenixFramework.atomic(() -> {
            log.logRequestReceiveDateAndData(bean.getTransactionId(), "Notification", bean.getPaymentType(), bean.getAmount(),
                    bean.getPaymentResultCode(), !STATUS_FAIL.equals(bean.getPaymentResultCode()));

            log.setExtInvoiceId(bean.getMerchantTransactionId());
            log.setMeoWalletId(bean.getTransactionId());
        });

    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.MeoWallet.presentationName");
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        final MeoWalletLog log = MeoWalletLog.createForTransationReport(merchantTransationId);
        try {
            List<MeoWalletPaymentBean> resultCheckoutBean = new ArrayList<>();
            FenixFramework.atomic(() -> log.setRequestSendDate(DateTime.now()));
            resultCheckoutBean = getMeoWalletService().getPaymentTransactionReportByMerchantTransactionId(merchantTransationId);

            String request = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getRequestLog();
            String response = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getResponseLog();
            String operationId = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getId();
            FenixFramework.atomic(() -> {
                log.setRequestReceiveDate(DateTime.now());
                log.saveRequest(request);
                log.saveResponse(response);
                log.setMeoWalletId(operationId);
            });

            return resultCheckoutBean;
        } catch (Exception e) {
            FenixFramework.atomic(() -> log.logException(e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPayment(ForwardPaymentRequest forwardPayment) {
        return postProcessPayment(forwardPayment, "", Optional.of(forwardPayment.getTransactionId()));
    }

}
