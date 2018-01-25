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
package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;;

public class PaymentReferenceCode extends PaymentReferenceCode_Base {
    private static final int LENGTH_REFERENCE_CODE = 9;
    private static final BigDecimal SIBS_IGNORE_MAX_AMOUNT = BigDecimal.ZERO;
    public static final String TREASURY_OPERATION_LOG_TYPE = "TREASURY_OPERATION_LOG_TYPE";

    public PaymentReferenceCode() {
        super();
    }

    protected void init(final String referenceCode, final LocalDate beginDate, final LocalDate endDate,
            final PaymentReferenceCodeStateType state, PaymentCodePool pool, BigDecimal minAmount, BigDecimal maxAmount) {
        setReferenceCode(Strings.padStart(referenceCode, LENGTH_REFERENCE_CODE, '0'));
        setBeginDate(beginDate);
        setEndDate(endDate);
        setState(state);
        setPaymentCodePool(pool);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        checkRules();
    }

    public Interval getValidInterval() {
        return new Interval(getBeginDate().toDateTimeAtStartOfDay(),
                getEndDate().plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1));

    }

    private void checkRules() {
        if (this.getMinAmount() == null) {
            this.setMinAmount(BigDecimal.ZERO);
        }
        if (this.getMaxAmount() == null) {
            this.setMaxAmount(BigDecimal.ZERO);
        }

        if (findByReferenceCode(this.getPaymentCodePool().getEntityReferenceCode(), getReferenceCode(),
                this.getPaymentCodePool().getFinantialInstitution()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
        }
    }

    @Atomic
    public void edit(final String referenceCode, final LocalDate beginDate, final LocalDate endDate,
            final PaymentReferenceCodeStateType state) {
        setReferenceCode(referenceCode);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setState(state);
        checkRules();
    }

    public boolean isDeletable() {
        return getReportedInFilesSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.cannot.delete");
        }

        setPaymentCodePool(null);
        setTargetPayment(null);
        deleteDomainObject();
    }

    @Atomic
    public static PaymentReferenceCode create(final String referenceCode, final LocalDate beginDate, final LocalDate endDate,
            final PaymentReferenceCodeStateType state, PaymentCodePool pool, BigDecimal minAmount, BigDecimal maxAmount) {
        PaymentReferenceCode paymentReferenceCode = new PaymentReferenceCode();
        paymentReferenceCode.init(referenceCode, beginDate, endDate, state, pool, minAmount, maxAmount);
        return paymentReferenceCode;
    }

    public static Stream<PaymentReferenceCode> findAll() {
        Set<PaymentReferenceCode> result = new HashSet<PaymentReferenceCode>();

        for (PaymentCodePool pool : PaymentCodePool.findAll().collect(Collectors.toList())) {
            result.addAll(pool.getPaymentReferenceCodesSet());
        }

        return result.stream();
    }

    private static Stream<PaymentReferenceCode> findByReferenceCode(String entityReferenceCode, String referenceCode,
            FinantialInstitution finantialInstitution) {
        return findByReferenceCode(referenceCode, finantialInstitution)
                .filter(x -> x.getPaymentCodePool().getEntityReferenceCode().equals(entityReferenceCode));
    }

    private static Stream<PaymentReferenceCode> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getPaymentCodePoolsSet().stream().flatMap(i -> i.getPaymentReferenceCodesSet().stream());
    }

    public static Stream<PaymentReferenceCode> findByReferenceCode(final String referenceCode,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> referenceCode.equalsIgnoreCase(i.getReferenceCode()));
    }

    public static Stream<PaymentReferenceCode> findByBeginDate(final LocalDate beginDate,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> beginDate.equals(i.getBeginDate()));
    }

    public static Stream<PaymentReferenceCode> findByEndDate(final LocalDate endDate, FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> endDate.equals(i.getEndDate()));
    }

    public static Stream<PaymentReferenceCode> findByState(
            final org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType state,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> state.equals(i.getState()));
    }

    public String getFormattedCode() {
        final StringBuilder result = new StringBuilder();
        int i = 1;
        for (char character : getReferenceCode().toCharArray()) {
            result.append(character);
            if (i % 3 == 0) {
                result.append(" ");
            }
            i++;
        }

        return result.charAt(result.length() - 1) == ' ' ? result.deleteCharAt(result.length() - 1).toString() : result
                .toString();
    }

    @Override
    public void setReferenceCode(String code) {
//        if (getReferenceCode() == null) {
        super.setReferenceCode(code);
//        } else if (code != getReferenceCode()) {
//            throw new TreasuryDomainException("error.accounting.PaymentCode.cannot.modify.code");
//        }
    }

    @Atomic
    public void bruteForceSetReferenceCode(String code) {
        super.setReferenceCode(code);
    }

    @Override
    @Atomic
    public void setState(PaymentReferenceCodeStateType state) {
        super.setState(state);
    }

    public boolean isNew() {
        return getState() == PaymentReferenceCodeStateType.UNUSED;
    }

    protected void reuseCode() {
        setState(PaymentReferenceCodeStateType.UNUSED);
    }

    public boolean isUsed() {
        return getState() == PaymentReferenceCodeStateType.USED;
    }

    public boolean isAnnulled() {
        return getState() == PaymentReferenceCodeStateType.ANNULLED;
    }

    public boolean isProcessed() {
        return getState() == PaymentReferenceCodeStateType.PROCESSED;
    }

    public void anull() {
        setState(PaymentReferenceCodeStateType.ANNULLED);
    }

    public boolean isFixedAmount() {
        return this.getPaymentCodePool().getIsFixedAmount();
    }

    public boolean isAvailableForReuse() {
        return !isNew();
    }

    public void update(final LocalDate startDate, final LocalDate endDate, final BigDecimal minAmount,
            final BigDecimal maxAmount) {
        super.setBeginDate(startDate);
        super.setEndDate(endDate);
        super.setMinAmount(minAmount);
        super.setMaxAmount(maxAmount != null ? maxAmount : SIBS_IGNORE_MAX_AMOUNT);
        checkRules();
    }

    @Atomic
    public SettlementNote processPayment(final String responsibleUsername, BigDecimal amountToPay, DateTime whenRegistered,
            String sibsTransactionId, String comments, final DateTime whenProcessedBySibs, final SibsReportFile sibsReportFile) {

        if (!isNew() && SibsTransactionDetail.isReferenceProcessingDuplicate(this.getReferenceCode(),
                this.getPaymentCodePool().getEntityReferenceCode(), whenRegistered)) {
            return null;
        }

        SettlementNote note =
                this.getTargetPayment().processPayment(responsibleUsername, amountToPay, whenRegistered, sibsTransactionId, comments);

        final DebtAccount referenceDebtAccount = this.getTargetPayment().getDebtAccount();

        final String debtAccountId = referenceDebtAccount.getExternalId();
        final String customerId = referenceDebtAccount.getCustomer().getExternalId();
        final String businessIdentification = referenceDebtAccount.getCustomer().getBusinessIdentification();
        final String fiscalNumber = valueOrEmpty(referenceDebtAccount.getCustomer().getFiscalCountry()) + ":"
                + valueOrEmpty(referenceDebtAccount.getCustomer().getFiscalNumber());
        final String customerName = referenceDebtAccount.getCustomer().getName();
        final String settlementDocumentNumber = note.getUiDocumentNumber();

        SibsTransactionDetail.create(sibsReportFile, comments, whenProcessedBySibs, whenRegistered, amountToPay,
                getPaymentCodePool().getEntityReferenceCode(), getReferenceCode(), sibsTransactionId, debtAccountId, customerId,
                businessIdentification, fiscalNumber, customerName, settlementDocumentNumber);

        return note;

    }

    public String getDescription() {
        return this.getPaymentCodePool().getEntityReferenceCode() + " " + this.getReferenceCode();
    }

    static public PaymentReferenceCode readByCode(final String code, FinantialInstitution finantialInstitution) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        PaymentReferenceCode paymentReferenceCode = null;
        for (PaymentCodePool pool : finantialInstitution.getPaymentCodePoolsSet()) {
            paymentReferenceCode = pool.getPaymentReferenceCodesSet().stream().filter(y -> y.getReferenceCode().equals(code))
                    .findFirst().orElse(null);
            if (paymentReferenceCode != null) {
                break;
            }
        }
        return paymentReferenceCode;
    }

    public String getReferenceCodeWithoutCheckDigits() {
        if (Boolean.TRUE.equals(this.getPaymentCodePool().getUseCheckDigit())) {
            if (this.getReferenceCode().length() >= 2) {
                return this.getReferenceCode().substring(0, this.getReferenceCode().length() - 2);
            } else {
                return this.getReferenceCode();
            }
        }
        throw new TreasuryDomainException("error.PaymentReferenceCode.not.from.pool.with.checkdigit");
    }

    public SibsReportFile getReportOnDate(DateTime transactionWhenRegistered) {
        return this.getReportedInFilesSet().stream().filter(x -> x.getWhenProcessedBySibs().equals(transactionWhenRegistered))
                .findFirst().orElse(null);
    }

    @Atomic
    public void createPaymentTargetTo(FinantialDocument finantialDocument) {
        if (this.getTargetPayment() != null && Boolean.TRUE.equals(this.getTargetPayment().getValid())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.payment.target.already.exists");
        }
        FinantialDocumentPaymentCode targetToFinantialDocument =
                FinantialDocumentPaymentCode.create(finantialDocument, this, true);
        this.setTargetPayment(targetToFinantialDocument);
        this.setState(PaymentReferenceCodeStateType.USED);
        this.setPayableAmount(finantialDocument.getOpenAmount());
        checkRules();
    }

    @Atomic
    public void createPaymentTargetTo(final Set<DebitEntry> debitNoteEntries, final BigDecimal payableAmount) {
        if (this.getTargetPayment() != null && Boolean.TRUE.equals(this.getTargetPayment().getValid())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.payment.target.already.exists");
        }

        MultipleEntriesPaymentCode target = MultipleEntriesPaymentCode.create(debitNoteEntries, this, true);
        this.setTargetPayment(target);
        this.setState(PaymentReferenceCodeStateType.USED);
        this.setPayableAmount(payableAmount);
        checkRules();
    }

    @Atomic
    public void anullPaymentReferenceCode() {
        if (!this.getState().equals(PaymentReferenceCodeStateType.ANNULLED)) {
            this.setState(PaymentReferenceCodeStateType.ANNULLED);
        }
        checkRules();
    }

    private String valueOrEmpty(final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return "";
        }

        return value;
    }
    
    @Atomic
    public static PaymentReferenceCode createPaymentReferenceCodeForMultipleDebitEntries(final PaymentReferenceCodeBean bean) {
        BigDecimal amount = BigDecimal.ZERO;
        for(DebitEntry entry : bean.getSelectedDebitEntries()) {
            amount = amount.add(entry.getOpenAmount());
        }
        
        bean.setPaymentAmount(amount);

        final PaymentReferenceCode paymentReferenceCode =
                bean.getPaymentCodePool()
                        .getReferenceCodeGenerator()
                        .generateNewCodeFor(
                                bean.getPaymentAmount(), bean.getBeginDate(), bean.getEndDate(),
                                bean.getPaymentCodePool().getIsFixedAmount());

        paymentReferenceCode.createPaymentTargetTo(Sets.newHashSet(bean.getSelectedDebitEntries()), bean.getPaymentAmount());
        return paymentReferenceCode;
    }

    
}