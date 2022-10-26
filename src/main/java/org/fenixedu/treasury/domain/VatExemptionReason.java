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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class VatExemptionReason extends VatExemptionReason_Base {

    public static final Comparator<VatExemptionReason> COMPARE_BY_CODE = (o1, o2) -> {
        int c = o1.getCode().compareTo(o2.getCode());
        
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };
    
    protected VatExemptionReason() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected VatExemptionReason(String code, LocalizedString name, String legalArticle, boolean active) {
        this();
        setCode(code);
        setName(name);
        setLegalArticle(legalArticle);
        setActive(active);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.VatExemptionReason.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.VatExemptionReason.name.required");
        }
        
        if(getActive() == null) {
            throw new TreasuryDomainException("error.VatExemptionReason.active.required");
        }

        findByCode(getCode());
    }

    @Atomic
    public void edit(String code, LocalizedString name, String legalArticle, boolean active) {
        setCode(code);
        setName(name);
        setLegalArticle(legalArticle);
        setActive(active);

        checkRules();
    }

    public boolean isDeletable() {
        return getProductsSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.VatExemptionReason.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    public static Stream<VatExemptionReason> findAll() {
        return FenixFramework.getDomainRoot().getVatExemptionReasonsSet().stream();
    }

    public static VatExemptionReason findByCode(final String code) {
        VatExemptionReason result = null;

        for (final VatExemptionReason it : findAll().collect(Collectors.toList())) {
            if (!it.getCode().equalsIgnoreCase(code)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.VatExemptionReason.duplicated.code");
            }

            result = it;
        }

        return result;
    }

    public static VatExemptionReason findByName(final String name) {
        VatExemptionReason result = null;

        for (final VatExemptionReason it : findAll().collect(Collectors.toList())) {

            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            result = it;
        }

        return result;
    }

    public static VatExemptionReason create(String code, LocalizedString name, String legalArticle, boolean active) {
        return new VatExemptionReason(code, name, legalArticle, active);
    }

}
