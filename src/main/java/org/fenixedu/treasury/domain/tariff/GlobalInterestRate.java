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
package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class GlobalInterestRate extends GlobalInterestRate_Base {

    public static final Comparator<? super GlobalInterestRate> FIRST_DATE_COMPARATOR = (o1, o2) -> {
        final int c = o1.getFirstDay().compareTo(o2.getFirstDay());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static Comparator<GlobalInterestRate> COMPARATOR_BY_YEAR = (o1, o2) -> {
        final int c = Integer.compare(o1.getYear(), o2.getYear());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected GlobalInterestRate() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(LocalDate firstDay, final LocalizedString description, final BigDecimal rate, final boolean applyPaymentMonth,
            final boolean applyInFirstWorkday) {
        setFirstDay(firstDay);
        setYear(firstDay.getYear());
        setDescription(description);
        setRate(rate);
        setApplyPaymentMonth(applyPaymentMonth);
        setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }

    private void checkRules() {
        if(getFirstDay() == null) {
            throw new TreasuryDomainException("error.GlobalInterestRate.firstDay.required");
        }
        
        if (findByFirstDay(getFirstDay()).count() > 1) {
            throw new TreasuryDomainException("error.GlobalInterestRate.firstDay.duplicated");
        }
        
        if(getRate() == null) {
            throw new TreasuryDomainException("error.GlobalInterestRate.rate.with.valid.value.required");
        }
        
        if(StringUtils.isEmpty(getDescription().getContent())) {
            throw new TreasuryDomainException("error.GlobalInterestRate.description.required");
        }
        
        if(TreasuryConstants.isLessThan(getRate(), BigDecimal.ZERO) || 
                TreasuryConstants.isGreaterThan(getRate(), TreasuryConstants.HUNDRED_PERCENT)) {
            throw new TreasuryDomainException("error.GlobalInterestRate.rate.with.valid.value.required");
        }
    }

    @Atomic
    public void edit(LocalDate firstDay, final LocalizedString description, final BigDecimal rate, final boolean applyPaymentMonth,
            final boolean applyInFirstWorkday) {
        setYear(firstDay.getYear());
        setFirstDay(firstDay);
        setDescription(description);
        setRate(rate);
        setApplyPaymentMonth(applyPaymentMonth);
        setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }

    @Deprecated
    public int getYear() {
        return super.getYear();
    }

    public boolean isApplyPaymentMonth() {
        return super.getApplyPaymentMonth();
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.GlobalInterestRate.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    @Atomic
    public static GlobalInterestRate create(LocalDate firstDay, final LocalizedString description, final BigDecimal rate,
            boolean applyPaymentMonth, boolean applyInFirstWorkday) {
        GlobalInterestRate globalInterestRate = new GlobalInterestRate();
        globalInterestRate.init(firstDay, description, rate, applyPaymentMonth, applyInFirstWorkday);
        return globalInterestRate;
    }

    public static Stream<GlobalInterestRate> findAll() {
        return FenixFramework.getDomainRoot().getGlobalInterestRatesSet().stream();
    }

    public static Stream<GlobalInterestRate> findByYear(final int year) {
        return findAll().filter(i -> year == i.getYear());
    }

    public static Optional<GlobalInterestRate> findUniqueByYear(final int year) {
        return findByYear(year).findFirst();
    }

    public static Stream<GlobalInterestRate> findByDescription(final LocalizedString description) {
        return findAll().filter(i -> description.equals(i.getDescription()));
    }

    public static Stream<GlobalInterestRate> findByRate(final BigDecimal rate) {
        return findAll().filter(i -> rate.equals(i.getRate()));
    }
    
    public static Stream<GlobalInterestRate> findByFirstDay(LocalDate date) {
        return findAll().filter(r -> r.getFirstDay().equals(date));
    }
    
    public static Optional<GlobalInterestRate> findUniqueAppliedForDate(LocalDate date) {
        return findAll().filter(r -> !r.getFirstDay().isAfter(date)).sorted(FIRST_DATE_COMPARATOR.reversed()).findFirst();
    }

}
