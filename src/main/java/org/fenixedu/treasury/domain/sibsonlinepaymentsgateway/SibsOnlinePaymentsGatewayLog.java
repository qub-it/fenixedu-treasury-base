package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

public class SibsOnlinePaymentsGatewayLog extends SibsOnlinePaymentsGatewayLog_Base {
    
    public static final String REQUEST_PAYMENT_CODE = "REQUEST_PAYMENT_CODE";
    public static final String WEBHOOK_NOTIFICATION = "WEBHOOK_NOTIFICATION";

    public SibsOnlinePaymentsGatewayLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected SibsOnlinePaymentsGatewayLog(final String operationCode) {
        this();
        
        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
        setOperationCode(operationCode);
    }

    protected SibsOnlinePaymentsGatewayLog(final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, final String operationCode,
            final DebtAccount debtAccount) {
        this();

        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());

        setSibsOnlinePaymentsGateway(sibsOnlinePaymentsGateway);
        setOperationCode(operationCode);

        setDebtAccount(debtAccount);
        setCustomerFiscalNumber(debtAccount.getCustomer().getUiFiscalNumber());
        setCustomerName(debtAccount.getCustomer().getName());
        setCustomerBusinessIdentification(debtAccount.getCustomer().getBusinessIdentification());

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayLog.domainRoot.required");
        }

        if (Strings.isNullOrEmpty(getOperationCode())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayLog.operationCode.required");
        }

        if(REQUEST_PAYMENT_CODE.equals(getOperationCode())) {
            if (getSibsOnlinePaymentsGateway() == null) {
                throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayLog.sibsOnlinePaymentsGateway.required");
            }

            if(getDebtAccount() == null) {
                throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayLog.debtAccount.required");
            }
        }
    }
    
    public boolean isExceptionOccured() {
        return super.getExceptionOccured();
    }
    
    public boolean isOperationSuccess() {
        return super.getOperationSuccess();
    }

    public void logRequestSendDate() {
        setRequestSendDate(new DateTime());
    }

    public void logRequestReceiveDateAndData(final String transactionId, final boolean operationSuccess,
            final boolean transactionPaid, final String operationResultCode, final String operationResultDescription) {
        setRequestReceiveDate(new DateTime());
        setTransactionId(transactionId);
        setOperationSuccess(operationSuccess);
        setTransactionPaid(transactionPaid);
        setOperationResultCode(operationResultCode);
        setOperationResultDescription(operationResultDescription);
    }
    
    public static final String OCTECT_STREAM_CONTENT_TYPE = "application/octet-stream";

    public void saveRequestAndResponsePayload(final String requestPayload, final String responsePayload) {
        final ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        
        if(requestPayload != null) {
            final String requestPayloadFileId = implementation.createFile(String.format("sibsOnlinePaymentsGatewayLog-requestPayload-%s.txt", 
                    getExternalId()), OCTECT_STREAM_CONTENT_TYPE, requestPayload.getBytes());
            
            setRequestPayloadFileId(requestPayloadFileId);
        }

        if(responsePayload != null) {
            final String responsePayloadFileId = implementation.createFile(String.format("sibsOnlinePaymentsGatewayLog-responsePayload-%s.txt", 
                    getExternalId()), OCTECT_STREAM_CONTENT_TYPE, responsePayload.getBytes());
            
            setResponsePayloadFileId(responsePayloadFileId);
        }
    }

    public void markExceptionOccuredAndSaveLog(final Exception e) {
        final ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        
        final String exceptionLog = String.format("%s\n%s", e.getLocalizedMessage(), ExceptionUtils.getFullStackTrace(e));

        setExceptionOccured(true);

        final String exceptionLogFileId = implementation.createFile(String.format("sibsOnlinePaymentsGatewayLog-exception-%s.txt", 
                getExternalId()), OCTECT_STREAM_CONTENT_TYPE, exceptionLog.getBytes());
        
        setExceptionLogFileId(exceptionLogFileId);
    }

    public void savePaymentCode(final String paymentCode) {
        setPaymentCode(paymentCode);
    }

    public void saveMerchantTransactionId(final String merchantTransactionId) {
        setMerchantTransactionId(merchantTransactionId);
    }
    
    public void saveReferenceId(final String referenceId) {
        setReferenceId(referenceId);
    }

    public void saveWebhookNotificationData(final String notificationInitializationVector, final String notificationAuthenticationTag,
            final String notificationEncryptedPayload) {
        final ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        
        setNotificationInitializationVector(notificationInitializationVector);
        setNotificationAuthTag(notificationAuthenticationTag);
        
        if(notificationEncryptedPayload != null) {
            final String notificationEncryptedPayloadFileId = implementation.createFile(String.format("sibsOnlinePaymentsGatewayLog-notificationEncryptedPayload-%s.txt", 
                    getExternalId()), OCTECT_STREAM_CONTENT_TYPE, notificationEncryptedPayload.getBytes());
            
            setNotificationEncryptedPayloadFileId(notificationEncryptedPayloadFileId);
        }
        
    }

    public void associateSibsOnlinePaymentGatewayAndDebtAccount(final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway,
            final DebtAccount debtAccount) {
        setSibsOnlinePaymentsGateway(sibsOnlinePaymentsGateway);
        setDebtAccount(debtAccount);
    }

    public void savePaymentInfo(BigDecimal amount, DateTime paymentDate) {
        setAmount(amount);
        setPaymentDate(paymentDate);
    }
    
    public void savePaymentTypeAndBrand(String paymentType, String paymentBrand) {
        setPaymentType(paymentType);
        setPaymentBrand(paymentBrand);
    }

    public void markAsDuplicatedTransaction() {
        setSibsTransactionDuplicated(true);
    }

    public void markSettlementNotesCreated(Set<SettlementNote> settlementNotes) {
        final String settlementNotesNumber = String.join(";", settlementNotes.stream()
                .map(SettlementNote::getUiDocumentNumber).collect(Collectors.toSet()));
        
        setSettlementNoteNumbers(settlementNotesNumber);
    }


    /* ******** */
    /* SERVICES */
    /* ******** */
    
    public static SibsOnlinePaymentsGatewayLog createLogForRequestPaymentCode(
            final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, final DebtAccount debtAccount) {
        return new SibsOnlinePaymentsGatewayLog(sibsOnlinePaymentsGateway, REQUEST_PAYMENT_CODE, debtAccount);
    }
    
    public static SibsOnlinePaymentsGatewayLog createLogForWebhookNotification() {
        return new SibsOnlinePaymentsGatewayLog(WEBHOOK_NOTIFICATION);
    }

}
