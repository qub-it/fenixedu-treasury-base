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

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class FiscalYear extends FiscalYear_Base {

    public static final Comparator<FiscalYear> COMPARE_BY_YEAR = (o1, o2) -> {
        int c = Integer.compare(o1.getYear(), o2.getYear());
        
        if(c != 0) {
            return c;
        }
        
        return o1.getExternalId().compareTo(o2.getExternalId());
    };
    
    public FiscalYear() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    public FiscalYear(final FinantialInstitution finantialInstitution, final int year, final LocalDate settlementAnnulmentLimitDate) {
        this();
        setFinantialInstitution(finantialInstitution);
        setYear(year);
        setSettlementAnnulmentLimitDate(settlementAnnulmentLimitDate);

        for(int i = 1; i <= 12; i++) {
            FiscalMonth.create(this, i);
        }
        
        checkRules();
    }
    
    public void checkRules() {
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.FiscalYear.domainRoot.required");
        }
        
        if(getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.FiscalYear.finantialInstitution.required");
        }
        
        if(getSettlementAnnulmentLimitDate() == null) {
            throw new TreasuryDomainException("error.FiscalYear.settlementAnnulmentLimitDate.required");
        }
        
        if(FiscalYear.find(getFinantialInstitution(), getYear()).count() > 1) {
            throw new TreasuryDomainException("error.FiscalYear.already.defined.for.finantial.institution.and.year");
        }
    }

    @Atomic
    public void editSettlementAnnulmentLimitDate(final LocalDate limitDate) {
        setSettlementAnnulmentLimitDate(limitDate);
        
        checkRules();
    }
    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static Stream<FiscalYear> findAll() {
        return FenixFramework.getDomainRoot().getFiscalYearsSet().stream();
    }
    
    public static Stream<FiscalYear> find(final FinantialInstitution finantialInstitution, final int year) {
        return finantialInstitution.getFiscalYearsSet().stream().filter(fy -> fy.getYear() == year);
    }
    
    public static Optional<FiscalYear> findUnique(final FinantialInstitution finantialInstitution, final int year) {
        return find(finantialInstitution, year).findFirst();
    }
    
    @Atomic
    public static FiscalYear create(final FinantialInstitution finantialInstitution, final int year, final LocalDate settlementAnnulmentLimitDate) {
        return new FiscalYear(finantialInstitution, year, settlementAnnulmentLimitDate);
    }
}
