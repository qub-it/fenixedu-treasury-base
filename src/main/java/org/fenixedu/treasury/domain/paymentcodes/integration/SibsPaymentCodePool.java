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
package org.fenixedu.treasury.domain.paymentcodes.integration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.services.payments.paymentscodegenerator.CheckDigitGenerator;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public class SibsPaymentCodePool extends SibsPaymentCodePool_Base implements ISibsPaymentCodePoolService {

    private static final String CODE_FILLER = "0";
    private static final int NUM_CONTROL_DIGITS = 2;
    private static final int NUM_SEQUENTIAL_NUMBERS = 7;

    public SibsPaymentCodePool() {
        super();
    }

    protected SibsPaymentCodePool(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String name,
            boolean active, String entityReferenceCode, long minReferenceCode, long maxReferenceCode, BigDecimal minAmount,
            BigDecimal maxAmount, LocalDate validFrom, LocalDate validTo, boolean useCheckDigit,
            boolean generateReferenceCodeOnDemand, String sourceInstitutionId, String destinationInstitutionId) {
        this();

        super.init(finantialInstitution, finantialEntity, name, active);

        setEntityReferenceCode(entityReferenceCode);
        setMinReferenceCode(minReferenceCode);
        setMaxReferenceCode(maxReferenceCode);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        setValidFrom(validFrom);
        setValidTo(validTo);
        setUseCheckDigit(useCheckDigit);
        setGenerateReferenceCodeOnDemand(generateReferenceCodeOnDemand);

        setSourceInstitutionId(sourceInstitutionId);
        setDestinationInstitutionId(destinationInstitutionId);

        setNextReferenceCode(minReferenceCode);

        if (isUseCheckDigit() && !isGenerateReferenceCodeOnDemand()) {
            throw new TreasuryDomainException("error.SibsPaymentCodePool.checkDigit.only.supports.generateReferenceCodeOnDemand");
        }

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbPaymentMethod());

        checkRules();
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (StringUtils.isEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentCodePool.name.required");
        }

        if (StringUtils.isEmpty(this.getEntityReferenceCode())) {
            throw new TreasuryDomainException("error.PaymentCodePool.entityReferenceCode.required");
        }

        if (this.getMinReferenceCode() <= 0 || this.getMinReferenceCode() >= this.getMaxReferenceCode()) {
            throw new TreasuryDomainException("error.PaymentCodePool.MinReferenceCode.invalid");
        }

        if (this.getValidFrom() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.validFrom.required");
        }

        if (this.getValidTo() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.validTo.required");
        }

        if (this.getValidTo().isBefore(this.getValidFrom())) {
            throw new TreasuryDomainException("error.PaymentCodePool.ValiddFrom.ValidTo.invalid");
        }

        SibsPaymentCodePool.findAll().filter(p -> p != this).forEach(p -> {
            if (!p.getEntityReferenceCode().equals(this.getEntityReferenceCode())) {
                return;
            }

            if (this.getMinReferenceCode() >= p.getMinReferenceCode() && this.getMinReferenceCode() <= p.getMaxReferenceCode()) {
                throw new TreasuryDomainException("error.SibsPaymentCodePool.invalid.reference.range.cross.other.pools");
            }

            if (this.getMaxReferenceCode() >= p.getMinReferenceCode() && this.getMaxReferenceCode() <= p.getMinReferenceCode()) {
                throw new TreasuryDomainException("error.SibsPaymentCodePool.invalid.reference.range.cross.other.pools");
            }
        });
    }

    public boolean isUseCheckDigit() {
        return super.getUseCheckDigit();
    }

    public boolean isGenerateReferenceCodeOnDemand() {
        return super.getGenerateReferenceCodeOnDemand();
    }

    public long getAndIncrementNextReferenceCode() {
        final long nextReferenceCode = getNextReferenceCode();
        setNextReferenceCode(nextReferenceCode + 1);

        return nextReferenceCode;
    }

    @Override
    public Set<SibsPaymentRequest> getAssociatedPaymentRequestsSet() {
        return (Set<SibsPaymentRequest>) super.getAssociatedPaymentRequestsSet();
    }

    public void edit(String name, boolean active, LocalDate validFrom, LocalDate validTo, String sourceInstitutionId,
            String destinationInstitutionId) {
        setName(name);

        setActive(active);
        setValidFrom(validFrom);
        setValidTo(validTo);
        setSourceInstitutionId(sourceInstitutionId);
        setDestinationInstitutionId(destinationInstitutionId);

        checkRules();
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments) {
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

        LocalDate now = new LocalDate();
        Set<LocalDate> map = debitEntries.stream().map(d -> d.getDueDate()).collect(Collectors.toSet());
        map.addAll(installments.stream().map(i -> i.getDueDate()).collect(Collectors.toSet()));
        LocalDate validTo = map.stream().max(LocalDate::compareTo).orElse(now);

        if (validTo.isBefore(now)) {
            validTo = now;
        }

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(DebitEntry::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        return createPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequest(SettlementNoteBean settlementNoteBean) {

        DebtAccount debtAccount = settlementNoteBean.getDebtAccount();
        Set<DebitEntry> debitEntries =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.getInvoiceEntry() != null)
                        .map(s -> s.getInvoiceEntry()).map(DebitEntry.class::cast).collect(Collectors.toSet());
        Set<Installment> installments =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.isForInstallment())
                        .map(InstallmentPaymenPlanBean.class::cast).map(s -> s.getInstallment()).collect(Collectors.toSet());

        BigDecimal payableAmount = settlementNoteBean.getTotalAmountToPay();
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

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
    @Deprecated
    // TODO: Only used by PaymentReferenceCodeController.createPaymentCodeForSeveralDebitEntries() method. Replace with settlement note bean
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount) {
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

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
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

        LocalDate now = new LocalDate();
        Set<LocalDate> map = debitEntries.stream().map(d -> d.getDueDate()).collect(Collectors.toSet());
        map.addAll(installments.stream().map(i -> i.getDueDate()).collect(Collectors.toSet()));
        LocalDate validTo = map.stream().max(LocalDate::compareTo).orElse(now);
        if (validTo.isBefore(now)) {
            validTo = now;
        }

        BigDecimal payableAmountDebitEntries = debitEntries.stream()
                .map(d -> d.getOpenAmountWithInterestsAtDate(interestsCalculationDate)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        return createPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    public Set<SibsReferenceCode> createReferenceCodesInAdvance(int numberOfPaymentsCodesToCreate) {
        if (isUseCheckDigit()) {
            throw new RuntimeException("error");
        }

        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

        return IntStream.range(0, numberOfPaymentsCodesToCreate).boxed().map(i -> {
            long nextSequentialNumber = getAndIncrementNextReferenceCode();

            if (nextSequentialNumber > getMaxReferenceCode()) {
                throw new TreasuryDomainException(
                        "error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
            }

            String sequentialNumberPadded =
                    StringUtils.leftPad(String.valueOf(nextSequentialNumber), NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
            String controDigitsPadded =
                    StringUtils.leftPad(String.valueOf(new Random().nextInt(99)), NUM_CONTROL_DIGITS, CODE_FILLER);

            String referenceCodeString = sequentialNumberPadded + controDigitsPadded;

            return SibsReferenceCode.create(this, referenceCodeString, getValidFrom(), getValidTo(), getMinAmount(),
                    getMaxAmount());
        }).collect(Collectors.toSet());
    }

    public Stream<SibsReferenceCode> getPaymentCodesToExport() {
        return getPaymentCodesToExport(new LocalDate());
    }

    public Stream<SibsReferenceCode> getPaymentCodesToExport(LocalDate localDate) {
        if (this.getUseCheckDigit()) {
            return Collections.<SibsReferenceCode> emptySet().stream();
        }

        return this.getSibsReferenceCodesSet().stream().filter(x -> !x.isInPaidState()).filter(x -> !x.isInAnnuledState())
                .filter(x -> !x.getValidTo().isBefore(localDate));
    }

    @Override
    public void delete() {
        super.delete();

        while (!getSibsReferenceCodesSet().isEmpty()) {
            getSibsReferenceCodesSet().iterator().next().delete();
        }

        super.deleteDomainObject();
    }

    @Atomic
    private SibsPaymentRequest createPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate validTo, BigDecimal payableAmount) {
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

        if (SibsPaymentRequest.findRequestedByDebitEntriesSet(debitEntries).count() > 0
                || SibsPaymentRequest.findCreatedByDebitEntriesSet(debitEntries).count() > 0) {
            throw new TreasuryDomainException("error.SibsPaymentCodePool.paymentCode.already.exists.for.selected.debit.entries");
        }

        checkMaxActiveSibsPaymentRequests(debitEntries, installments);

        SibsReferenceCode code = isUseCheckDigit() ? generateCheckDigitPaymentCode(debtAccount, payableAmount,
                validTo) : generateSequentialPaymentCode(debtAccount, payableAmount, validTo);

        SibsPaymentRequest paymentRequest =
                SibsPaymentRequest.create(code, debtAccount, debitEntries, installments, payableAmount);

        return paymentRequest;
    }

    private void checkMaxActiveSibsPaymentRequests(Set<DebitEntry> debitEntries, Set<Installment> installments) {
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
        for (Installment installment : installments) {
            long numActiveSibsPaymentRequests =
                    installment.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                            .map(SibsPaymentRequest.class::cast).filter(r -> r.getState() == PaymentReferenceCodeStateType.UNUSED
                                    || r.getState() == PaymentReferenceCodeStateType.USED)
                            .count();

            if (numActiveSibsPaymentRequests >= 2) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        installment.getDescription().getContent());
            }
        }
    }

    private SibsReferenceCode generateSequentialPaymentCode(DebtAccount debtAccount, BigDecimal payableAmount,
            LocalDate validTo) {
        if (isUseCheckDigit()) {
            throw new RuntimeException("error");
        }

        LocalDate now = new LocalDate();

        // ANIL 2024-08-06 (#qubIT-Fenix-5597)
        // 

        SibsReferenceCode pregeneratedReference =
                findAndUsePregeneratedReference(debtAccount, payableAmount, validTo.isAfter(now) ? validTo : now);

        if (pregeneratedReference != null) {
            // Check that the code pool is the same
            if (pregeneratedReference.getDigitalPaymentPlatform() != this) {
                throw new RuntimeException("pregenerated reference digital payment platform mismatch");
            }

            if (pregeneratedReference.getPregeneratedReferenceDebtAccount() != null) {
                throw new RuntimeException("pregenerated reference not dissociated from debt account");
            }

            return pregeneratedReference;
        }

        // First find unused payment code reference
        Optional<SibsReferenceCode> possiblePaymentCode =
                getSibsReferenceCodesSet().stream().filter(SibsReferenceCode::isInCreatedState)
                        .filter(p -> !TreasuryConstants.isGreaterThan(payableAmount, p.getMaxAmount())) //
                        .filter(p -> !TreasuryConstants.isLessThan(payableAmount, p.getMinAmount())) //
                        .filter(p -> p.getSibsPaymentRequest() == null) //
                        .filter(p -> p.getPregeneratedReferenceDebtAccount() == null) //
                        .filter(p -> p.getValidInterval()
                                .contains(validTo.isAfter(now) ? validTo.toDateTimeAtStartOfDay() : now.toDateTimeAtStartOfDay())) //
                        .findAny();

        if (possiblePaymentCode.isPresent()) {
            return possiblePaymentCode.get();
        }

        if (!isGenerateReferenceCodeOnDemand()) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        long nextSequentialNumber = getAndIncrementNextReferenceCode();
        if (nextSequentialNumber > getMaxReferenceCode()) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        String sequentialNumberPadded =
                StringUtils.leftPad(String.valueOf(nextSequentialNumber), NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
        String controDigitsPadded =
                StringUtils.leftPad(String.valueOf(new Random().nextInt(99)), NUM_CONTROL_DIGITS, CODE_FILLER);

        String referenceCodeString = sequentialNumberPadded + controDigitsPadded;

        if (getValidTo().isBefore(validTo)) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        if (TreasuryConstants.isGreaterThan(payableAmount, getMaxAmount())) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        if (TreasuryConstants.isLessThan(payableAmount, getMinAmount())) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        return SibsReferenceCode.create(this, referenceCodeString, new LocalDate(), validTo, payableAmount, payableAmount);
    }

    private SibsReferenceCode generateCheckDigitPaymentCode(DebtAccount debtAccount, BigDecimal payableAmount,
            LocalDate validTo) {
        if (!isUseCheckDigit()) {
            throw new RuntimeException("not check digit sibs platform");
        }

        // ANIL 2024-08-06 (#qubIT-Fenix-5597)
        // 

        SibsReferenceCode pregeneratedReference = findAndUsePregeneratedReference(debtAccount, payableAmount, validTo);

        if (pregeneratedReference != null) {
            // Check that the code pool is the same
            if (pregeneratedReference.getDigitalPaymentPlatform() != this) {
                throw new RuntimeException("pregenerated reference digital payment platform mismatch");
            }

            // Check that the check digit is the same
            String sequentialNumberPadded = StringUtils.leftPad(pregeneratedReference.getReferenceCode().substring(0, 7),
                    NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
            final String referenceCodeString = CheckDigitGenerator.generateReferenceCodeWithCheckDigit(getEntityReferenceCode(),
                    sequentialNumberPadded, payableAmount);

            if (!pregeneratedReference.getReferenceCode().equals(referenceCodeString)) {
                throw new RuntimeException("pregenerated reference check digit mismatch");
            }

            if (pregeneratedReference.getPregeneratedReferenceDebtAccount() != null) {
                throw new RuntimeException("pregenerated reference not dissociated from debt account");
            }

            return pregeneratedReference;
        }

        long nextReferenceCode = getAndIncrementNextReferenceCode();
        if (nextReferenceCode > getMaxReferenceCode()) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        String sequentialNumberPadded =
                StringUtils.leftPad(String.valueOf("" + nextReferenceCode), NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
        final String referenceCodeString = CheckDigitGenerator.generateReferenceCodeWithCheckDigit(getEntityReferenceCode(),
                sequentialNumberPadded, payableAmount);

        if (getValidTo().isBefore(validTo)) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        if (TreasuryConstants.isGreaterThan(payableAmount, getMaxAmount())) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        if (TreasuryConstants.isLessThan(payableAmount, getMinAmount())) {
            throw new TreasuryDomainException("error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
        }

        return SibsReferenceCode.create(this, referenceCodeString, new LocalDate(), getValidTo(), payableAmount, payableAmount);
    }

    public SibsReferenceCode pregenerateSibsReferenceCode(DebtAccount debtAccount, BigDecimal payableAmount) {
        if (!isActive()) {
            throw new RuntimeException("payment code pool not active");
        }

        if (isUseCheckDigit()) {
            long nextReferenceCode = getAndIncrementNextReferenceCode();
            if (nextReferenceCode > getMaxReferenceCode()) {
                throw new TreasuryDomainException(
                        "error.SequentialPaymentCodeGenerator.generateNewCodeFor.cannot.generate.new.code");
            }

            String sequentialNumberPadded =
                    StringUtils.leftPad(String.valueOf("" + nextReferenceCode), NUM_SEQUENTIAL_NUMBERS, CODE_FILLER);
            final String referenceCodeString = CheckDigitGenerator.generateReferenceCodeWithCheckDigit(getEntityReferenceCode(),
                    sequentialNumberPadded, payableAmount);

            return SibsReferenceCode.createPregeneratedReference(this, debtAccount, referenceCodeString, payableAmount);
        } else {
            // First find unused payment code reference
            Optional<SibsReferenceCode> possiblePaymentCode =
                    getSibsReferenceCodesSet().stream().filter(SibsReferenceCode::isInCreatedState)
                            .filter(p -> !TreasuryConstants.isGreaterThan(payableAmount, p.getMaxAmount())) //
                            .filter(p -> !TreasuryConstants.isLessThan(payableAmount, p.getMinAmount())) //
                            .filter(p -> p.getSibsPaymentRequest() == null) //
                            .filter(p -> p.getPregeneratedReferenceDebtAccount() == null) //
                            .filter(p -> p.isValidInDateInterval(new DateTime())) //
                            .findAny();

            if (possiblePaymentCode.isPresent()) {
                SibsReferenceCode result = possiblePaymentCode.get();
                result.allocateAsPregeneratedReference(debtAccount);

                return result;
            }

            return null;
        }
    }

    public boolean isPregeneratedReferenceExists(DebtAccount debtAccount, BigDecimal payableAmount, LocalDate validTo) {

        SibsReferenceCode pregeneratedReferenceCode = findPregeneratedReference(debtAccount, payableAmount, validTo);

        return pregeneratedReferenceCode != null;
    }

    public SibsReferenceCode findAndUsePregeneratedReference(DebtAccount debtAccount, BigDecimal payableAmount,
            LocalDate validTo) {
        // ANIL 2024-08-06 (#qubIT-Fenix-5597)

        SibsReferenceCode pregeneratedReferenceCode = findPregeneratedReference(debtAccount, payableAmount, validTo);
        if (pregeneratedReferenceCode == null) {
            return null;
        }

        pregeneratedReferenceCode.usePregeneratedReference();

        return pregeneratedReferenceCode;
    }

    public Set<SibsReferenceCode> getAllPregeneratedReferences(DebtAccount debtAccount, BigDecimal payableAmount,
            LocalDate validTo) {
        return getStreamForFindPregeneratedReference(debtAccount, payableAmount, validTo).collect(Collectors.toSet());
    }

    public Set<SibsReferenceCode> getAllPregeneratedReferences(DebtAccount debtAccount) {
        return getStreamForFindPregeneratedReference(debtAccount).collect(Collectors.toSet());
    }

    public static Stream<SibsReferenceCode> getStreamForFindPregeneratedReference(Stream<SibsReferenceCode> stream,
            BigDecimal payableAmount, LocalDate validTo) {

        // ANIL 2024-08-28 (#qubIT-Fenix-5597)
        //
        // This method is an helper to generate the necessary and missing pre references,
        // in order to not create more than what is necessary

        return stream //
                .filter(p -> p.isValidInDateInterval(validTo.toDateTimeAtStartOfDay())) //
                .filter(p -> p.getSibsPaymentRequest() == null) //
                .filter(p -> p.isInPayableAmountInterval(payableAmount));
    }

    private SibsReferenceCode findPregeneratedReference(DebtAccount debtAccount, BigDecimal payableAmount, LocalDate validTo) {

        // ANIL 2024-08-06 (#qubIT-Fenix-5597)
        // 
        // a) First search pre generated reference that is associated with some customer
        // b) the pregenerated must have the same payable amount and valid date interval for validTo
        // c) the pregenerated ref mb must not have a payment request associated
        // d) if the pregenerated ref is found, then dissociate from the customer

        SibsReferenceCode pregeneratedReferenceCode = getStreamForFindPregeneratedReference(debtAccount, payableAmount, validTo) //
                .findFirst().orElse(null);

        return pregeneratedReferenceCode;
    }

    private Stream<SibsReferenceCode> getStreamForFindPregeneratedReference(DebtAccount debtAccount, BigDecimal payableAmount,
            LocalDate validTo) {
        Stream<SibsReferenceCode> stream = getStreamForFindPregeneratedReference(debtAccount);

        return getStreamForFindPregeneratedReference(stream, payableAmount, validTo);
    }

    private Stream<SibsReferenceCode> getStreamForFindPregeneratedReference(DebtAccount debtAccount) {
        return debtAccount.getCustomer().getAllCustomers().stream()
                .flatMap(c -> c.getDebtAccountsSet().stream()
                        .filter(d -> d.getFinantialInstitution() == debtAccount.getFinantialInstitution())) //
                .flatMap(d -> d.getPregeneratedSibsReferenceCodesSet().stream()) //
                .filter(p -> p.getDigitalPaymentPlatform() == this);
    }

    public void removePregeneratedReference(SibsReferenceCode sibsReferenceCode) {
        if (sibsReferenceCode.getPregeneratedReferenceDebtAccount() == null) {
            throw new RuntimeException("error");
        }

        if (isUseCheckDigit()) {
            sibsReferenceCode.delete();
        } else {
            sibsReferenceCode.setPregeneratedReferenceDebtAccount(null);
        }

    }

    //@formatter:off
    /* ********
     * SERVICES
     * ********
     */
    //@formatter:on

    public static Stream<SibsPaymentCodePool> findAll() {
        return DigitalPaymentPlatform.findAll().filter(d -> d instanceof SibsPaymentCodePool)
                .map(SibsPaymentCodePool.class::cast);
    }

    public static Stream<SibsPaymentCodePool> find(FinantialInstitution finantialInstitution) {
        return DigitalPaymentPlatform.find(finantialInstitution).filter(d -> d instanceof SibsPaymentCodePool)
                .map(SibsPaymentCodePool.class::cast);
    }

    public static Stream<SibsPaymentCodePool> find(FinantialInstitution finantialInstitution, String entityReferenceCode) {
        return find(finantialInstitution).filter(d -> d.getEntityReferenceCode().equals(entityReferenceCode));
    }

    public static Stream<SibsPaymentCodePool> find(String entityReferenceCode) {
        return findAll().filter(d -> d.getEntityReferenceCode().equals(entityReferenceCode));
    }

    public static SibsPaymentCodePool create(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity,
            String name, boolean active, String entityReferenceCode, long minReferenceCode, long maxReferenceCode,
            BigDecimal minAmount, BigDecimal maxAmount, LocalDate validFrom, LocalDate validTo, boolean useCheckDigit,
            boolean generateReferenceCodeOnDemand, String sourceInstitutionId, String destinationInstitutionId) {
        return new SibsPaymentCodePool(finantialInstitution, finantialEntity, name, active, entityReferenceCode, minReferenceCode,
                maxReferenceCode, minAmount, maxAmount, validFrom, validTo, useCheckDigit, generateReferenceCodeOnDemand,
                sourceInstitutionId, destinationInstitutionId);
    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.SibsPaymentCodePool.presentationName");
    }

    @Override
    public PaymentTransaction processPaymentReferenceCodeTransaction(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        return processPaymentReferenceCodeTransaction(log, bean);
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        return Collections.emptyList();
    }

    @Override
    public PaymentRequestLog createLogForWebhookNotification() {
        return null;
    }

    @Override
    public void fillLogForWebhookNotification(PaymentRequestLog log, DigitalPlatformResultBean bean) {

    }

    @Override
    public boolean annulPaymentRequestInPlatform(SibsPaymentRequest sibsPaymentRequest) {
        sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null);
        return true;
    }

    @Override
    public int getMaximumLengthForAddressStreetFieldOne() {
        return 50;
    }

    @Override
    public int getMaximumLengthForAddressCity() {
        return 50;
    }

    @Override
    public int getMaximumLengthForPostalCode() {
        return 16;
    }

}
