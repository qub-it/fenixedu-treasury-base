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

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class VatExemptionReason extends VatExemptionReason_Base {

    protected VatExemptionReason() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    @Atomic
    public static void initializeVatExemption() {
        if (VatExemptionReason.findAll().count() == 0) {
            String[] codes =
                    new String[] { "M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11", "M12", "M13",
                            "M14", "M15", "M16", "M99" };

            for (String code : codes) {
                if (code.equals("M07")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Isento Artigo 9.º do CIVA (Ou similar)"));
                } else if (code.equals("M01")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Artigo 16.º n.º 6 alínea c) do CIVA (Ou similar)"));
                } else if (code.equals("M02")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Artigo 6.º do Decreto‐Lei n.º 198/90, de 19 de Junho"));
                } else if (code.equals("M03")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "Exigibilidade de caixa"));
                } else if (code.equals("M04")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Isento Artigo 13.º do CIVA (Ou similar)"));
                } else if (code.equals("M05")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Isento Artigo 14.º do CIVA (Ou similar)"));
                } else if (code.equals("M06")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Isento Artigo 15.º do CIVA (Ou similar)"));
                } else if (code.equals("M08")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "IVA - Autoliquidação"));
                } else if (code.equals("M09")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "IVA ‐ Não confere direito a dedução"));
                } else if (code.equals("M10")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "IVA - Regime de isenção"));
                } else if (code.equals("M16")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Isento Artigo 14.º do RITI (Ou similar)"));
                } else if (code.equals("M99")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(),
                            "Não sujeito; não tributado (Ou similar)"));
                } else {

                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), code));
                }
            }
        }
    }

    protected VatExemptionReason(final String code, final LocalizedString name) {
        this();
        setCode(code);
        setName(name);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.VatExemptionReason.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.VatExemptionReason.name.required");
        }

        findByCode(getCode());
        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));
    }

    @Atomic
    public void edit(final String code, final LocalizedString name) {
        setCode(code);
        setName(name);

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

            if (result != null) {
                throw new TreasuryDomainException("error.VatExemptionReason.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    @Atomic
    public static VatExemptionReason create(final String code, final LocalizedString name) {
        return new VatExemptionReason(code, name);
    }

}
