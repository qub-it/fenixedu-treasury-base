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
package org.fenixedu.treasury.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Vat extends Vat_Base {

    protected Vat() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected Vat(final VatType vatType, final FinantialInstitution finantialInstitution, final BigDecimal taxRate,
            final DateTime beginDate, final DateTime endDate) {
        this();
        setVatType(vatType);
        setFinantialInstitution(finantialInstitution);

        setTaxRate(taxRate);
        setBeginDate(beginDate);
        setEndDate(endDate);

        checkRules();
    }

    private void checkRules() {
        if (getTaxRate() == null) {
            throw new TreasuryDomainException("error.Vat.taxRate.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.Vat.finantialInstitution.required");
        }

        if (getVatType() == null) {
            throw new TreasuryDomainException("error.Vat.vatType.required");
        }

        if (getTaxRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new TreasuryDomainException("error.Vat.taxRate.cannot.be.negative");
        }

        if (getBeginDate() == null) {
            throw new TreasuryDomainException("error.Vat.beginDate.required");
        }

        if (getEndDate() == null) {
            throw new TreasuryDomainException("error.Vat.endDate.required");
        }

        if (!getEndDate().isAfter(getBeginDate())) {
            throw new TreasuryDomainException("error.Vat.endDate.end.date.must.be.after.begin.date");
        }

        if (findActive(getFinantialInstitution(), getVatType(), getBeginDate(), getEndDate()).count() > 1) {
            throw new TreasuryDomainException("error.Vat.date.interval.overlap.with.another");
        }
    }

    @Atomic
    public void edit(final BigDecimal taxRate, final DateTime beginDate, final DateTime endDate) {
        if(!getInvoiceEntriesSet().isEmpty()) {
            throw new TreasuryDomainException("error.Vat.edition.not.possible.due.to.existing.invoice.entries");
        }
        
        setTaxRate(taxRate);

        setBeginDate(beginDate);
        setEndDate(endDate);

        checkRules();
    }

    public boolean isDeletable() {
        return getInvoiceEntriesSet().isEmpty();

    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Vat.cannot.delete");
        }

        setDomainRoot(null);
        setVatType(null);

        setFinantialInstitution(null);

        deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<Vat> findAll() {
        return FenixFramework.getDomainRoot().getVatsSet().stream();
    }

    public static Stream<Vat> find(final VatType vatType) {
        return vatType.getVatsSet().stream();
    }

    protected static Stream<Vat> findActive(final VatType vatType, final DateTime when) {
        return find(vatType).filter(v -> v.interval().contains(when));
    }

    protected static Stream<Vat> findActive(final FinantialInstitution finantialInstitution, final VatType vatType,
            final DateTime begin, final DateTime end) {
        final Interval interval = new Interval(begin, end);
        return find(vatType).filter(v -> v.getFinantialInstitution().equals(finantialInstitution))
                .filter(v -> v.interval().overlaps(interval));
    }

    public static Optional<Vat> findActiveUnique(final VatType vatType, final FinantialInstitution finantialInstitution,
            final DateTime when) {
        return findActive(vatType, when).filter(x -> x.getFinantialInstitution().equals(finantialInstitution)).findFirst();
    }

    @Atomic
    public static Vat create(final VatType vatType, final FinantialInstitution finantialInstitution, final BigDecimal taxRate,
            final DateTime beginDate, final DateTime endDate) {
        return new Vat(vatType, finantialInstitution, taxRate, beginDate, endDate);
    }

    /* -----
     * UTILS
     * -----
     */

    private Interval interval() {
        // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
        return new Interval(getBeginDate(), getEndDate() != null ? getEndDate() : TreasuryConstants.INFINITY_DATE);
    }

    public boolean isActiveNow() {
        return isActive(new DateTime());
    }

    public boolean isActive(DateTime when) {
        return this.getBeginDate().isBefore(when) && (this.getEndDate() == null || this.getEndDate().isAfter(when));
    }

}
