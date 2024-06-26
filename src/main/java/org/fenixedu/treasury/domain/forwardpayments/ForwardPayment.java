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
package org.fenixedu.treasury.domain.forwardpayments;

import static com.qubit.terra.framework.tools.excel.ExcelUtil.createCellWithValue;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.exceptions.ForwardPaymentAlreadyPayedException;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentImplementation;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.streaming.spreadsheet.ExcelSheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.Spreadsheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class ForwardPayment extends ForwardPayment_Base {

    private static final Comparator<ForwardPayment> ORDER_COMPARATOR =
            (o1, o2) -> ((Long) o1.getOrderNumber()).compareTo(o2.getOrderNumber());

    private ForwardPayment() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public ForwardPayment(final ForwardPaymentConfiguration forwardPaymentConfiguration, final DebtAccount debtAccount,
            final Set<DebitEntry> debitEntriesSet, Set<Installment> installmentsToPay) {
        this();

        setForwardPaymentConfiguration(forwardPaymentConfiguration);
        setDebtAccount(debtAccount);
        getDebitEntriesSet().addAll(debitEntriesSet);
        getInstallmentsSet().addAll(installmentsToPay);
        setCurrentState(ForwardPaymentStateType.CREATED);
        setWhenOccured(new DateTime());

        // Verify that all debitEntries have open amount greater than zero
        for (final DebitEntry debitEntry : debitEntriesSet) {
            if (!TreasuryConstants.isPositive(debitEntry.getOpenAmount())) {
                throw new TreasuryDomainException("error.ForwardPayment.open.amount.debit.entry.not.positive");
            }
        }

        BigDecimal amount = debitEntriesSet.stream().map(DebitEntry::getOpenAmountWithInterests).reduce((a, c) -> a.add(c))
                .orElse(BigDecimal.ZERO);
        amount = amount.add(
                installmentsToPay.stream().map(Installment::getOpenAmount).reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO));
        setAmount(debtAccount.getFinantialInstitution().getCurrency().getValueWithScale(amount));
        setOrderNumber(lastForwardPayment().isPresent() ? lastForwardPayment().get().getOrderNumber() + 1 : 1);
        log();

        checkRules();
    }

    public void reject(final String statusCode, final String errorMessage, final String requestBody, final String responseBody) {
        setCurrentState(ForwardPaymentStateType.REJECTED);
        setRejectionCode(statusCode);
        setRejectionLog(errorMessage);

        log(statusCode, errorMessage, requestBody, responseBody);
        checkRules();
    }

    public void advanceToRequestState(final String statusCode, final String statusMessage, final String requestBody,
            final String responseBody) {
        setCurrentState(ForwardPaymentStateType.REQUESTED);
        log(statusCode, statusMessage, requestBody, responseBody);

        checkRules();
    }

    public void advanceToAuthenticatedState(final String statusCode, final String statusMessage, final String requestBody,
            final String responseBody) {
        setCurrentState(ForwardPaymentStateType.AUTHENTICATED);
        log(statusCode, statusMessage, requestBody, responseBody);

        checkRules();
    }

    public void advanceToAuthorizedState(final String statusCode, final String errorMessage, final String requestBody,
            final String responseBody) {
        if (!isActive()) {
            throw new TreasuryDomainException("error.ForwardPayment.not.in.active.state");
        }

        if (isInAuthorizedState()) {
            throw new TreasuryDomainException("error.ForwardPayment.already.authorized");
        }

        if (isInPayedState()) {
            throw new ForwardPaymentAlreadyPayedException("error.ForwardPayment.already.payed");
        }

        setCurrentState(ForwardPaymentStateType.AUTHORIZED);
        log(statusCode, errorMessage, requestBody, responseBody);

        checkRules();
    }

    private static final Comparator<DebitEntry> COMPARE_DEBIT_ENTRIES = (o1, o2) -> {
        final Product interestProduct = TreasurySettings.getInstance().getInterestProduct();
        if (o1.getProduct() == interestProduct && o2.getProduct() != interestProduct) {
            return -1;
        }

        if (o1.getProduct() != interestProduct && o2.getProduct() == interestProduct) {
            return 1;
        }

        // compare by openAmount. First higher amounts then lower amounts
        int compareByOpenAmount = o1.getOpenAmount().compareTo(o2.getOpenAmount());

        if (compareByOpenAmount != 0) {
            return compareByOpenAmount * -1;
        }

        return o1.getExternalId().compareTo(o2.getExternalId());
    };

    public void advanceToPayedState(final String statusCode, final String statusMessage, final BigDecimal payedAmount,
            final DateTime transactionDate, final String transactionId, final String authorizationNumber,
            final String requestBody, final String responseBody, String justification) {
        throw new RuntimeException("not supported");
    }

    public boolean payAllDebitEntriesInterests() {
        return true;
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(final String statusCode) {
        final Map<String, String> paymentEntryPropertiesMap = Maps.newHashMap();

        paymentEntryPropertiesMap.put("OrderNumber", String.valueOf(getOrderNumber()));

        if (!Strings.isNullOrEmpty(getTransactionId())) {
            paymentEntryPropertiesMap.put("TransactionId", getTransactionId());
        }

        if (getTransactionDate() != null) {
            paymentEntryPropertiesMap.put("TransactionDate",
                    getTransactionDate().toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
        }

        if (!Strings.isNullOrEmpty(statusCode)) {
            paymentEntryPropertiesMap.put("StatusCode", statusCode);
        }
        return paymentEntryPropertiesMap;
    }

    public boolean isActive() {
        return getCurrentState() != ForwardPaymentStateType.REJECTED;
    }

    public boolean isInCreatedState() {
        return getCurrentState() == ForwardPaymentStateType.CREATED;
    }

    public boolean isInAuthorizedState() {
        return getCurrentState() == ForwardPaymentStateType.AUTHORIZED;
    }

    public boolean isInPayedState() {
        return getCurrentState() == ForwardPaymentStateType.PAYED;
    }

    public boolean isInAuthenticatedState() {
        return getCurrentState() == ForwardPaymentStateType.AUTHENTICATED;
    }

    public boolean isInRequestedState() {
        return getCurrentState() == ForwardPaymentStateType.REQUESTED;
    }

    public String getReferenceNumber() {
        return String.valueOf(getOrderNumber());
    }

    public List<ForwardPaymentLog> getOrderedForwardPaymentLogs() {
        return getForwardPaymentLogsSet().stream().sorted(ForwardPaymentLog.COMPARATOR_BY_ORDER).collect(Collectors.toList());
    }

    private void checkRules() {
        if (isInPayedState() && getSettlementNote() == null) {
            throw new TreasuryDomainException("error.ForwardPayment.settlementNote.required");
        }

        if (getReferencedCustomers().size() > 1) {
            throw new TreasuryDomainException("error.ForwardPayment.referencedCustomers.only.one.allowed");
        }
    }

    public Set<Customer> getReferencedCustomers() {
        final Set<InvoiceEntry> invoiceEntrySet = getInvoiceEntriesSet();
        final Set<Installment> installments = getInstallmentsSet();
        
        final Set<Customer> result = Sets.newHashSet();
        for (final InvoiceEntry entry : invoiceEntrySet) {
            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).isForPayorDebtAccount()) {
                result.add(((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(entry.getDebtAccount().getCustomer());
        }

        for (final Installment entry : installments) {
            result.addAll(entry.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry())
                    .map(deb -> (deb.getFinantialDocument() != null && ((Invoice) deb.getFinantialDocument())
                            .isForPayorDebtAccount()) ? ((Invoice) deb.getFinantialDocument()).getPayorDebtAccount()
                                    .getCustomer() : deb.getDebtAccount().getCustomer())
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    public ForwardPaymentLog log(final String statusCode, final String statusMessage, final String requestBody,
            final String responseBody) {
        final ForwardPaymentLog log = log();

        log.setStatusCode(statusCode);
        log.setStatusLog(statusMessage);

        if (!Strings.isNullOrEmpty(requestBody)) {
            ForwardPaymentLogFile.createForRequestBody(log, requestBody.getBytes());
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            ForwardPaymentLogFile.createForResponseBody(log, responseBody.getBytes());
        }

        return log;
    }

    public ForwardPaymentLog logException(final Exception e, final String requestBody, final String responseBody) {
        final String exceptionLog = String.format("%s\n%s", e.getLocalizedMessage(), ExceptionUtils.getFullStackTrace(e));
        final ForwardPaymentLog log = log();

        log.setExceptionOccured(true);

        if (!Strings.isNullOrEmpty(requestBody)) {
            ForwardPaymentLogFile.createForRequestBody(log, requestBody.getBytes());
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            ForwardPaymentLogFile.createForResponseBody(log, responseBody.getBytes());
        }

        ForwardPaymentLogFile.createForException(log, exceptionLog.getBytes());

        return log;
    }

    private ForwardPaymentLog log() {
        return new ForwardPaymentLog(this, getCurrentState(), getWhenOccured());
    }

    public void delete() {

        setDebtAccount(null);
        setForwardPaymentConfiguration(null);
        setSettlementNote(null);

        setDomainRoot(null);

        getDebitEntriesSet().clear();

        while (!getForwardPaymentLogsSet().isEmpty()) {
            getForwardPaymentLogsSet().iterator().next().delete();
        }

        deleteDomainObject();

    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<ForwardPayment> findAll() {
        return FenixFramework.getDomainRoot().getForwardPaymentsSet().stream();
    }

    public static Stream<ForwardPayment> findAllByStateType(final ForwardPaymentStateType... stateTypes) {
        List<ForwardPaymentStateType> t = Lists.newArrayList(stateTypes);
        return findAll().filter(f -> t.contains(f.getCurrentState()));
    }

    public static ForwardPayment create(final ForwardPaymentConfiguration forwardPaymentConfiguration,
            final DebtAccount debtAccount, final Set<DebitEntry> debitEntriesToPay, Set<Installment> installmentsToPay) {
        throw new RuntimeException("not supported");
    }

    public static ForwardPayment create(final SettlementNoteBean bean, final Function<ForwardPayment, String> successUrlFunction,
            final Function<ForwardPayment, String> insuccessUrlFunction) {
        throw new RuntimeException("not supported");
    }

    private static Optional<ForwardPayment> lastForwardPayment() {
        return FenixFramework.getDomainRoot().getForwardPaymentsSet().stream().max(ORDER_COMPARATOR);
    }

    // @formatter: off
    /*********************************
     * POST FORWARD PAYMENTS SERVICE *
     *********************************/
    // @formatter: on

    public static void postForwardPaymentProcessService(final DateTime beginDate, final DateTime endDate, final Logger logger) {

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    _postForwardPaymentProcessService(beginDate, endDate, logger);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        };

        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
        }
    }

    @Atomic(mode = TxMode.READ)
    private static void _postForwardPaymentProcessService(final DateTime beginDate, final DateTime endDate, final Logger logger)
            throws IOException {
        if (beginDate == null || endDate == null) {
            throw new TreasuryDomainException("error.ForwardPayment.postForwardPaymentProcessService.dates.required");
        }

        final DateTime postForwardPaymentsExecutionDate = new DateTime();

        final List<PostForwardPaymentReportBean> reportBeans = Lists.newArrayList();

        ForwardPayment.findAllByStateType(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                .filter(f -> f.getWhenOccured().compareTo(beginDate) >= 0 && f.getWhenOccured().compareTo(endDate) < 0)
                .forEach(f -> {
                    PostForwardPaymentReportBean reportBean;
                    try {
                        reportBean = updateForwardPayment(f.getExternalId(), logger);

                        if (reportBean != null) {
                // @formatter:off
                            logger.info(String.format(
                                    "C\tPAYMENT REQUEST\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                                    reportBean.executionDate, reportBean.forwardPaymentExternalId,
                                    reportBean.forwardPaymentOrderNumber, reportBean.customerCode, reportBean.customerName,
                                    reportBean.previousStateDescription, reportBean.nextStateDescription,
                                    reportBean.paymentRegisteredWithSuccess, reportBean.settlementNote,
                                    reportBean.advancedPaymentCreditNote, reportBean.paymentDate, reportBean.paidAmount,
                                    reportBean.advancedCreditAmount != null ? reportBean.advancedCreditAmount.toString() : "",
                                    reportBean.transactionId, reportBean.statusCode, reportBean.statusMessage,
                                    reportBean.remarks));

                            reportBeans.add(reportBean);
                            // @formatter:on
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        writeExcel(reportBeans, postForwardPaymentsExecutionDate, beginDate, endDate);
    }

    private static void writeExcel(final List<PostForwardPaymentReportBean> reportBeans,
            final DateTime postForwardPaymentsExecutionDate, final DateTime beginDate, final DateTime endDate) {

        final byte[] content = Spreadsheet.buildSpreadsheetContent(() -> new ExcelSheet[] { new ExcelSheet() {

            @Override
            public String getName() {
                return treasuryBundle("label.PostForwardPaymentReportBean.sheet.name");
            }

            @Override
            public String[] getHeaders() {
                return new String[] { treasuryBundle("label.PostForwardPaymentReportBean.cell.executionDate"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.forwardPaymentExternalId"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.forwardPaymentOrderNumber"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.forwardPaymentWhenOccured"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.customerCode"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.customerName"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.previousStateDescription"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.nextStateDescription"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.paymentRegisteredWithSuccess"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.settlementNote"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.advancedCreditSettlementNote"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.paymentDate"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.paidAmount"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.advancedCreditAmount"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.transactionId"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.statusCode"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.statusMessage"),
                        treasuryBundle("label.PostForwardPaymentReportBean.cell.remarks") };
            }

            @Override
            public Stream<? extends SpreadsheetRow> getRows() {
                return reportBeans.stream();
            }

        } }, null);

        final String filename = treasuryBundle("label.PostForwardPaymentsReportFile.filename",
                postForwardPaymentsExecutionDate.toString("yyyy_MM_dd_HH_mm_ss"));

        PostForwardPaymentsReportFile.create(postForwardPaymentsExecutionDate, beginDate, endDate, filename, content);
    }

    private static PostForwardPaymentReportBean updateForwardPayment(final String forwardPaymentId, final Logger logger)
            throws IOException {

        try {
            PostForwardPaymentReportBean reportBean =
                    FenixFramework.getTransactionManager().withTransaction((Callable<PostForwardPaymentReportBean>) () -> {

                        final ForwardPayment forwardPayment = FenixFramework.getDomainObject(forwardPaymentId);
                        final IForwardPaymentImplementation implementation =
                                forwardPayment.getForwardPaymentConfiguration().implementation();
                        final String justification = treasuryBundle("error.PostForwardPaymentsTask.post.payment.justification");

                        final PostProcessPaymentStatusBean postProcessPaymentStatusBean =
                                implementation.postProcessPayment(forwardPayment, justification, Optional.empty());

                        return new PostForwardPaymentReportBean(forwardPayment, postProcessPaymentStatusBean);
                    });

            return reportBean;
        } catch (Exception e) {
            final String message = e.getMessage();
            final String stackTrace = ExceptionUtils.getStackTrace(e);

            String exceptionOutput = String.format("E\tERROR ON\t%s\t%s\n", forwardPaymentId, message);
            logger.error(exceptionOutput);
            logger.error(stackTrace + "\n");

            return null;
        }
    }

    private static class PostForwardPaymentReportBean implements SpreadsheetRow {

        private String executionDate;
        private String forwardPaymentExternalId;
        private String forwardPaymentOrderNumber;
        private String forwardPaymentWhenOccured;
        private String customerCode;
        private String customerName;
        private String previousStateDescription;
        private String nextStateDescription;
        private boolean paymentRegisteredWithSuccess;
        private String settlementNote = "";
        private String advancedPaymentCreditNote = "";
        private String paymentDate = "";
        private String paidAmount = "";
        private BigDecimal advancedCreditAmount;
        private String transactionId = "";
        private String statusCode;
        private String statusMessage;
        private String remarks = "";

        private PostForwardPaymentReportBean(final ForwardPayment forwardPayment,
                final PostProcessPaymentStatusBean postProcessPaymentStatusBean) {
            this.executionDate = new DateTime().toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);
            this.forwardPaymentExternalId = forwardPayment.getExternalId();
            this.forwardPaymentOrderNumber = forwardPayment.getReferenceNumber();
            this.forwardPaymentWhenOccured =
                    forwardPayment.getWhenOccured().toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);
            this.customerCode = forwardPayment.getDebtAccount().getCustomer().getBusinessIdentification();
            this.customerName = forwardPayment.getDebtAccount().getCustomer().getName();
            this.previousStateDescription = postProcessPaymentStatusBean.getPreviousState().getLocalizedName().getContent();
            this.nextStateDescription =
                    postProcessPaymentStatusBean.getForwardPaymentStatusBean().getStateType().getLocalizedName().getContent();
            this.paymentRegisteredWithSuccess = postProcessPaymentStatusBean.isSuccess();

            if (forwardPayment.getSettlementNote() != null) {
                this.settlementNote = forwardPayment.getSettlementNote().getUiDocumentNumber();
                this.paymentDate = forwardPayment.getSettlementNote().getPaymentDate()
                        .toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);
                this.paidAmount = forwardPayment.getSettlementNote().getTotalPayedAmount().toString();
                this.transactionId = forwardPayment.getTransactionId();

                if (forwardPayment.getSettlementNote().getAdvancedPaymentCreditNote() != null) {
                    this.advancedPaymentCreditNote =
                            forwardPayment.getSettlementNote().getAdvancedPaymentCreditNote().getUiDocumentNumber();
                    this.advancedCreditAmount =
                            forwardPayment.getSettlementNote().getAdvancedPaymentCreditNote().getTotalAmount();
                }

                if (hasSettlementNotesOnSameDayForSameDebts(forwardPayment)) {
                    remarks = treasuryBundle("warn.PostForwardPaymentsTask.settlement.notes.on.same.day.for.same.debts");
                }
            }

            this.statusCode = postProcessPaymentStatusBean.getForwardPaymentStatusBean().getStatusCode();
            this.statusMessage = postProcessPaymentStatusBean.getForwardPaymentStatusBean().getStatusMessage();

        }

        private boolean hasSettlementNotesOnSameDayForSameDebts(final ForwardPayment forwardPayment) {
            final LocalDate paymentDate = forwardPayment.getSettlementNote().getPaymentDate().toLocalDate();

            final Set<DebitEntry> forwardPaymentDebitEntriesSet = forwardPayment.getDebitEntriesSet();

            for (SettlementNote settlementNote : SettlementNote.findByDebtAccount(forwardPayment.getDebtAccount())
                    .collect(Collectors.toSet())) {
                if (settlementNote == forwardPayment.getSettlementNote()) {
                    continue;
                }

                if (settlementNote.isAnnulled()) {
                    continue;
                }

                if (!settlementNote.getPaymentDate().toLocalDate().isEqual(paymentDate)) {
                    continue;
                }

                for (final SettlementEntry settlementEntry : settlementNote.getSettlemetEntriesSet()) {
                    if (forwardPaymentDebitEntriesSet.contains(settlementEntry.getInvoiceEntry())) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public void writeCellValues(final Row row, final IErrorsLog errorsLog) {
            int i = 0;

            createCellWithValue(row, i++, executionDate);
            createCellWithValue(row, i++, forwardPaymentExternalId);
            createCellWithValue(row, i++, forwardPaymentOrderNumber);
            createCellWithValue(row, i++, forwardPaymentWhenOccured);
            createCellWithValue(row, i++, customerCode);
            createCellWithValue(row, i++, customerName);
            createCellWithValue(row, i++, previousStateDescription);
            createCellWithValue(row, i++, nextStateDescription);
            createCellWithValue(row, i++, treasuryBundle("label." + paymentRegisteredWithSuccess));
            createCellWithValue(row, i++, settlementNote);
            createCellWithValue(row, i++, advancedPaymentCreditNote);
            createCellWithValue(row, i++, paymentDate);
            createCellWithValue(row, i++, paidAmount);
            createCellWithValue(row, i++, advancedCreditAmount != null ? advancedCreditAmount.toString() : "");
            createCellWithValue(row, i++, transactionId);
            createCellWithValue(row, i++, statusCode);
            createCellWithValue(row, i++, statusMessage);
            createCellWithValue(row, i++, remarks);
        }

    }

    public static String PAYMENT_TYPE_DESCRIPTION() {
        return treasuryBundle("label.IPaymentProcessorForInvoiceEntries.paymentProcessorDescription.forwardPayment");
    }

    /* IPaymentProcessorForInvoiceEntries */

    public DocumentNumberSeries getDocumentSeriesForPayments() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(),
                getForwardPaymentConfiguration().getSeries());
    }

    public DocumentNumberSeries getDocumentSeriesInterestDebits() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), getForwardPaymentConfiguration().getSeries());
    }

    public PaymentMethod getPaymentMethod() {
        return getForwardPaymentConfiguration().getPaymentMethod();
    }

    public String fillPaymentEntryMethodId() {
        return null;
    }

    public BigDecimal getPayableAmount() {
        return getAmount();
    }

    public DateTime getPaymentRequestDate() {
        return getWhenOccured();
    }

    public String getPaymentRequestStateDescription() {
        return getCurrentState().getLocalizedName().getContent();
    }

    public String getPaymentTypeDescription() {
        return PAYMENT_TYPE_DESCRIPTION();
    }

    public Set<InvoiceEntry> getInvoiceEntriesSet() {
        Set<InvoiceEntry> result = new HashSet<>();
        result.addAll(getDebitEntriesSet());

        return result;
    }

    public boolean isForwardPayment() {
        return true;
    }

    public SibsOnlinePaymentsGateway getSibsOnlinePaymentsGateway() {
        return getForwardPaymentConfiguration().getSibsOnlinePaymentsGateway();
    }

    public String getSibsOppwaMerchantTransactionId() {
        return getSibsMerchantTransactionId();
    }

    public String getSibsOppwaTransactionId() {
        return getSibsTransactionId();
    }

}
