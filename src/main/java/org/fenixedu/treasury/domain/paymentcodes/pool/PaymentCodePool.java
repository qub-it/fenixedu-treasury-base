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
package org.fenixedu.treasury.domain.paymentcodes.pool;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.services.payments.paymentscodegenerator.IPaymentCodeGenerator;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class PaymentCodePool extends PaymentCodePool_Base {

    public static Comparator<PaymentCodePool> COMPARATOR_BY_FINANTIAL_INSTITUTION_AND_NAME = (o1, o2) -> {
        int c = FinantialInstitution.COMPARATOR_BY_NAME.compare(o1.getFinantialInstitution(), o2.getFinantialInstitution());
        if(c != 0) {
            return c;
        }
        
        c = o1.getName().compareTo(o2.getName());
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };
    
    public static Comparator<PaymentCodePool> COMPARATOR_BY_NAME = (o1, o2) -> {
        int c = o1.getName().compareTo(o2.getName());
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };
    
    protected PaymentCodePool() {
        super();
    }

    protected PaymentCodePool(final String name, final String entityReferenceCode, final Long minReferenceCode,
            final Long maxReferenceCode, final BigDecimal minAmount, final BigDecimal maxAmount, final LocalDate validFrom,
            final LocalDate validTo, final Boolean active, final Boolean useCheckDigit,
            final FinantialInstitution finantialInstitution, DocumentNumberSeries seriesToUseInPayments,
            PaymentMethod paymentMethod, final PaymentCodeGeneratorInstance paymentCodeGeneratorInstance) {
        this();
        init(name, entityReferenceCode, minReferenceCode, maxReferenceCode, minAmount, maxAmount, validFrom, validTo, active,
                useCheckDigit, finantialInstitution, seriesToUseInPayments, paymentMethod, paymentCodeGeneratorInstance);
    }

    protected void init(final String name, final String entityReferenceCode, final Long minReferenceCode,
            final Long maxReferenceCode, final BigDecimal minAmount, final BigDecimal maxAmount, final LocalDate validFrom,
            final LocalDate validTo, final Boolean active, final Boolean useCheckDigit,
            final FinantialInstitution finantialInstitution, DocumentNumberSeries seriesToUseInPayments,
            PaymentMethod paymentMethod, PaymentCodeGeneratorInstance paymentCodeGeneratorInstance) {
        setName(name);
        setEntityReferenceCode(entityReferenceCode);
        setNextReferenceCode(minReferenceCode);
        setMinReferenceCode(minReferenceCode);
        setMaxReferenceCode(maxReferenceCode);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        setValidFrom(validFrom);
        setValidTo(validTo);
        setActive(active);
        setUseCheckDigit(useCheckDigit);

        setFinantialInstitution(finantialInstitution);
        setPaymentMethod(paymentMethod);
        setDocumentSeriesForPayments(seriesToUseInPayments);
        setPaymentCodeGeneratorInstance(paymentCodeGeneratorInstance);
        
        checkRules();
    }

    private void checkRules() {
        if (this.getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.finantialInstitution.required");
        }

        if (this.getFinantialInstitution().getSibsConfiguration() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.finantialInstitution.sibsconfiguration.required");
        }
        Set<PaymentCodePool> allPools =
                PaymentCodePool.findByActive(true, this.getFinantialInstitution()).collect(Collectors.toSet());

        for (PaymentCodePool pool : allPools) {
            if (!pool.equals(this)) {
                if (pool.getEntityReferenceCode().equals(this.getEntityReferenceCode())) {
                    if (this.getMinReferenceCode() >= pool.getMinReferenceCode()
                            && this.getMinReferenceCode() <= pool.getMaxReferenceCode()) {
                        throw new TreasuryDomainException("error.PaymentCodePool.invalid.reference.range.cross.other.pools");
                    }

                    if (this.getMaxReferenceCode() >= pool.getMinReferenceCode()
                            && this.getMaxReferenceCode() <= pool.getMinReferenceCode()) {
                        throw new TreasuryDomainException("error.PaymentCodePool.invalid.reference.range.cross.other.pools");
                    }
                }
            }
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentCodePool.name.required");
        }

        if (Strings.isNullOrEmpty(this.getEntityReferenceCode())) {
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

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentCodePool.name.required");
        }

        if (this.getMaxAmount().compareTo(this.getMinAmount()) < 0) {
            throw new TreasuryDomainException("error.PaymentCodePool.MinMaxAmount.invalid");
        }

        if (this.getValidTo().isBefore(this.getValidFrom())) {
            throw new TreasuryDomainException("error.PaymentCodePool.ValiddFrom.ValidTo.invalid");
        }

        if (this.getDocumentSeriesForPayments() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.documentSeriesForPayments.required");
        }

        if (this.getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.PaymentCodePool.paymentMethod.required");
        }

        if (this.getFinantialInstitution() != this.getDocumentSeriesForPayments().getSeries().getFinantialInstitution()) {
            throw new TreasuryDomainException(
                    "error.PaymentCodePool.documentNumberSeriesForPayments.invalid.finantialInstitution");
        }

        if(getPaymentCodeGeneratorInstance() == null) {
            throw new TreasuryDomainException(
                    "error.PaymentCodePool.documentNumberSeriesForPayments.invalid.paymentCodeGeneratorInstance");
        }
    }

    public boolean isGenerateReferenceCodeOnDemand() {
        return getGenerateReferenceCodeOnDemand();
    }

    @Atomic
    @Deprecated
    public void edit(final String name, final Boolean active, DocumentNumberSeries seriesToUseInPayments,
            PaymentMethod paymentMethod) {
        setName(name);
        setActive(active);
        setDocumentSeriesForPayments(seriesToUseInPayments);
        setPaymentMethod(paymentMethod);
        checkRules();
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentCodePool.cannot.delete");
        }
        
        if(getSibsOnlinePaymentsGateway() != null) {
            throw new TreasuryDomainException("error.PaymentCodePool.remove.sibs.oppwa.configuration.first");
        }

        setDocumentSeriesForPayments(null);
        setFinantialInstitution(null);
        setPaymentCodeGeneratorInstance(null);

        deleteDomainObject();
    }
    
    public IPaymentCodeGenerator getPaymentCodeGenerator() {
        return getPaymentCodeGeneratorInstance().getPaymentCodeGenerator(this);
    }
    
    @Atomic
    public static PaymentCodePool create(final String name, final String entityReferenceCode, final Long minReferenceCode,
            final Long maxReferenceCode, final BigDecimal minAmount, final BigDecimal maxAmount, final LocalDate validFrom,
            final LocalDate validTo, final Boolean active, final Boolean useCheckDigit,
            final FinantialInstitution finantialInstitution, DocumentNumberSeries seriesToUseInPayments,
            PaymentMethod paymentMethod, final PaymentCodeGeneratorInstance paymentCodeGeneratorInstance) {

        if (finantialInstitution.getSibsConfiguration() == null || finantialInstitution.getSibsConfiguration() == null
                && !entityReferenceCode.equals(finantialInstitution.getSibsConfiguration().getEntityReferenceCode())) {
            throw new TreasuryDomainException(
                    "error.administration.payments.sibs.managepaymentcodepool.invalid.entity.reference.code.from.finantial.institution");
        }
        return new PaymentCodePool(name, entityReferenceCode, minReferenceCode, maxReferenceCode, minAmount, maxAmount, validFrom,
                validTo, active, useCheckDigit, finantialInstitution, seriesToUseInPayments, paymentMethod, paymentCodeGeneratorInstance);

    }

    // TODO legidio, can we please change this to FenixFramework.getDomainRoot().getFinantialInstitutionsSet().flatMap(x -> x.getPaymentCodePoolsSet().stream()) ?
    public static Stream<PaymentCodePool> findAll() {
        Set<PaymentCodePool> codes = new HashSet<PaymentCodePool>();

        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().stream().map(x -> x.getPaymentCodePoolsSet())
                .reduce(codes, (a, b) -> {
                    a.addAll(b);
                    return a;
                }).stream();
    }

    public static Stream<PaymentCodePool> findByName(final java.lang.String name,
            final FinantialInstitution finantialInstitution) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> name.equalsIgnoreCase(i.getName()));
    }

    public static Stream<PaymentCodePool> findByMinPaymentCodes(final java.lang.Integer minPaymentCodes,
            final FinantialInstitution finantialInstitution) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> minPaymentCodes.equals(i.getMinReferenceCode()));
    }

    public static Stream<PaymentCodePool> findByMaxPaymentCodes(final java.lang.Integer maxPaymentCodes,
            final FinantialInstitution finantialInstitution) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> maxPaymentCodes.equals(i.getMaxReferenceCode()));
    }

    public static Stream<PaymentCodePool> findByMinAmount(final java.math.BigDecimal minAmount,
            final FinantialInstitution finantialInstitution) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> minAmount.equals(i.getMinAmount()));
    }

    // TODO legidio finantialInstitution not used
    public static Stream<PaymentCodePool> findByMaxAmount(final java.math.BigDecimal maxAmount,
            final FinantialInstitution finantialInstitution) {
        return findAll().filter(i -> maxAmount.equals(i.getMaxAmount()));
    }

    // TODO legidio finantialInstitution not used
    public static Stream<PaymentCodePool> findByActive(final java.lang.Boolean active,
            final FinantialInstitution finantialInstitution) {
        return findAll().filter(i -> active.equals(i.getActive()));
    }

    public static Stream<PaymentCodePool> findByFinantialInstitution(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getPaymentCodePoolsSet().stream();
    }

    public IPaymentCodeGenerator getReferenceCodeGenerator() {
        return getPaymentCodeGeneratorInstance().getPaymentCodeGenerator(this);
    }

    public Long getAndIncrementNextReferenceCode() {
        final Long nextReferenceCode = getNextReferenceCode();
        setNextReferenceCode(nextReferenceCode + 1);
        return nextReferenceCode;
    }

    @Atomic
    public void setNewValidPeriod(LocalDate validFrom, LocalDate validTo) {
        if (this.getPaymentReferenceCodesSet().size() > 0
                && (this.getValidFrom().compareTo(validFrom) != 0 || this.getValidTo().compareTo(validTo) != 0)) {
            throw new TreasuryDomainException("error.PaymentCodePool.invalid.change.state.with.generated.references");
        }
        this.setValidFrom(validFrom);
        this.setValidTo(validTo);
        checkRules();
    }

    @Atomic
    public void changePooltype(Boolean useCheckDigit) {
        if (this.getPaymentReferenceCodesSet().size() > 0 && (this.getUseCheckDigit() != useCheckDigit)) {
            throw new TreasuryDomainException("error.PaymentCodePool.invalid.change.state.with.generated.references");
        }

        this.setUseCheckDigit(useCheckDigit);
        checkRules();
    }

    @Atomic
    public void changeFinantialInstitution(FinantialInstitution finantialInstitution) {
        if (this.getPaymentReferenceCodesSet().size() > 0 && this.getFinantialInstitution() != finantialInstitution) {
            throw new TreasuryDomainException("error.PaymentCodePool.invalid.change.state.with.generated.references");
        }
        this.setFinantialInstitution(finantialInstitution);
        checkRules();

    }

    @Atomic
    public void changeReferenceCode(String entityReferenceCode, Long minReferenceCode, Long maxReferenceCode) {
        if (this.getPaymentReferenceCodesSet().size() > 0 && (!this.getEntityReferenceCode().equals(entityReferenceCode)
                || !this.getMinReferenceCode().equals(minReferenceCode)
                || !this.getMaxReferenceCode().equals(maxReferenceCode))) {
            throw new TreasuryDomainException("error.PaymentCodePool.invalid.change.state.with.generated.references");
        }
        this.setEntityReferenceCode(entityReferenceCode);
        this.setMinReferenceCode(minReferenceCode);
        this.setMaxReferenceCode(maxReferenceCode);
        checkRules();

    }

    @Atomic
    public void changeAmount(BigDecimal minAmount, BigDecimal maxAmount) {
        if (this.getPaymentReferenceCodesSet().size() > 0
                && (this.getMinAmount().compareTo(minAmount) != 0 || this.getMaxAmount().compareTo(maxAmount) != 0)) {
            throw new TreasuryDomainException("error.PaymentCodePool.invalid.change.state.with.generated.references");
        }
        this.setMinAmount(minAmount);
        this.setMaxAmount(maxAmount);
        checkRules();

    }

    @Atomic
    @Deprecated
    public void update(final FinantialInstitution finantialInstitution, final String name, final String entityReferenceCode,
            final Long minReferenceCode, final Long maxReferenceCode, final BigDecimal minAmount, final BigDecimal maxAmount, final LocalDate validFrom,
            final LocalDate validTo, final Boolean active, final Boolean useCheckDigit, final DocumentNumberSeries seriesToUseInPayments,
            final PaymentMethod paymentMethod) {

        edit(name, active, seriesToUseInPayments, paymentMethod);
        setNewValidPeriod(validFrom, validTo);
        changeFinantialInstitution(finantialInstitution);
        changePooltype(useCheckDigit);
        changeReferenceCode(entityReferenceCode, minReferenceCode, maxReferenceCode);
        changeAmount(minAmount, maxAmount);
    }
    
    public void update(final String name,
            final Long minReferenceCode, final Long maxReferenceCode, final BigDecimal minAmount, final BigDecimal maxAmount, final LocalDate validFrom,
            final LocalDate validTo, final Boolean active) {

        edit(name, active, getDocumentSeriesForPayments(), getPaymentMethod());
        setNewValidPeriod(validFrom, validTo);
        changeReferenceCode(getEntityReferenceCode(), minReferenceCode, maxReferenceCode);
        changeAmount(minAmount, maxAmount);
    }
    
    public static Stream<PaymentCodePool> findByEntityCode(String entityCode) {
        return findAll().filter(x -> x.getEntityReferenceCode().equals(entityCode));
    }

    public List<PaymentReferenceCode> getPaymentCodesToExport(LocalDate localDate) {
        if (this.getUseCheckDigit()) {
            return Collections.EMPTY_LIST;
        } else {
            return this.getPaymentReferenceCodesSet().stream()
                    .filter(x -> !x.isProcessed())
                    .filter(x -> !x.isAnnulled())
                    .filter(x -> !x.getEndDate().isBefore(localDate)).collect(Collectors.toList());
        }
    }

    public List<PaymentReferenceCode> getAnnulledPaymentCodesToExport(LocalDate localDate) {
        if (this.getUseCheckDigit()) {
            return Collections.EMPTY_LIST;
        } else {
            return this.getPaymentReferenceCodesSet().stream()
                    .filter(x -> x.getState().equals(PaymentReferenceCodeStateType.ANNULLED) == true)
                    .filter(x -> x.getValidInterval().contains(localDate.toDateTimeAtStartOfDay())).collect(Collectors.toList());
        }
    }

    public void updatePoolReferences() {
        if (this.getUseCheckDigit()) {
        } else {
        }

    }

    public boolean getIsFixedAmount() {
        //When using checkdigit, it's fixed amount 
        //HACK: there is also an option without check digit for fixed amount
        return this.getUseCheckDigit();
    }

    public boolean getIsVariableTimeWindow() {
        // When using checkdigit, it's FIXED TIMEWINDOW
        return !this.getUseCheckDigit();
    }

    public static boolean isReferenceCodesActiveForStudentPortal(FinantialInstitution finantialInstitution) {
        return SibsPaymentCodePool.findForSibsPaymentCodeServiceByActive(finantialInstitution, true).findFirst().isPresent()
                && ("502488603".equals(finantialInstitution.getCode()) || "FMV".equals(finantialInstitution.getCode()));
    }
}
