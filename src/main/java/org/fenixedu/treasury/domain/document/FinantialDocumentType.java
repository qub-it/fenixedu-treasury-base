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
package org.fenixedu.treasury.domain.document;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class FinantialDocumentType extends FinantialDocumentType_Base {

    protected FinantialDocumentType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected FinantialDocumentType(final FinantialDocumentTypeEnum type, final String code, final LocalizedString name,
            final String documentNumberSeriesPrefix, final boolean invoice) {
        this();
        setType(type);
        setCode(code);
        setName(name);
        setDocumentNumberSeriesPrefix(documentNumberSeriesPrefix);
        setInvoice(invoice);

        checkRules();
    }

    private void checkRules() {
        if (getType() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentType.type.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.FinantialDocumentType.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.FinantialDocumentType.name.required");
        }

        final Set<FinantialDocumentType> stream =
                findAll().filter(fdt -> fdt.getType() == this.getType()).collect(Collectors.toSet());

        if (stream.size() > 1) {
            throw new TreasuryDomainException("error.FinantialDocumentType.not.unique.in.finantial.document.type");
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
        return getFinantialDocumentsSet().isEmpty() && getDocumentNumberSeriesSet().isEmpty()
                && getTreasuryDocumentTemplatesSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FinantialDocumentType.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<FinantialDocumentType> findAll() {
        return FenixFramework.getDomainRoot().getFinantialDocumentTypesSet().stream();
    }

    public static FinantialDocumentType findByCode(final String code) {
        FinantialDocumentType result = null;

        for (final FinantialDocumentType it : findAll().collect(Collectors.toList())) {
            if (!it.getCode().equalsIgnoreCase(code)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.FinantialDocumentType.duplicated.code");
            }

            result = it;
        }

        return result;
    }

    protected static FinantialDocumentType findByName(final String name) {
        FinantialDocumentType result = null;

        for (final FinantialDocumentType it : findAll().collect(Collectors.toList())) {

            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.FinantialDocumentType.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    protected static FinantialDocumentType findByDocumentNumberSeriesPrefix(final String documentNumberSeriesPrefix) {
        FinantialDocumentType result = null;

        for (final FinantialDocumentType it : findAll().collect(Collectors.toList())) {
            if (!it.getDocumentNumberSeriesPrefix().equalsIgnoreCase(documentNumberSeriesPrefix)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.FinantialDocumentType.duplicated.documentNumberSeriesPrefix");
            }

            result = it;
        }

        return result;
    }

    protected static FinantialDocumentType findByFinantialDocumentType(final FinantialDocumentTypeEnum type) {
        final Set<FinantialDocumentType> stream = findAll().filter(fdt -> fdt.getType() == type).collect(Collectors.toSet());

        if (stream.size() > 1) {
            throw new TreasuryDomainException("error.FinantialDocumentType.not.unique.in.finantial.document.type");
        }
        if (stream.size() == 0) {
            return null;
        }
        return stream.iterator().next();
    }

    public static FinantialDocumentType findForDebitNote() {
        return findByFinantialDocumentType(FinantialDocumentTypeEnum.DEBIT_NOTE);
    }

    public static FinantialDocumentType findForCreditNote() {
        return findByFinantialDocumentType(FinantialDocumentTypeEnum.CREDIT_NOTE);
    }

    public static FinantialDocumentType findForSettlementNote() {
        return findByFinantialDocumentType(FinantialDocumentTypeEnum.SETTLEMENT_NOTE);
    }

    public static FinantialDocumentType findForReimbursementNote() {
        return findByFinantialDocumentType(FinantialDocumentTypeEnum.REIMBURSEMENT_NOTE);
    }

    @Atomic
    public static FinantialDocumentType createForDebitNote(final String code, final LocalizedString name,
            final String documentNumberSeriesPrefix, boolean invoice) {
        return new FinantialDocumentType(FinantialDocumentTypeEnum.DEBIT_NOTE, code, name, documentNumberSeriesPrefix, invoice);
    }

    @Atomic
    public static FinantialDocumentType createForCreditNote(final String code, final LocalizedString name,
            final String documentNumberSeriesPrefix, boolean invoice) {
        return new FinantialDocumentType(FinantialDocumentTypeEnum.CREDIT_NOTE, code, name, documentNumberSeriesPrefix, invoice);
    }

    @Atomic
    public static FinantialDocumentType createForSettlementNote(final String code, final LocalizedString name,
            final String documentNumberSeriesPrefix, boolean invoice) {
        return new FinantialDocumentType(FinantialDocumentTypeEnum.SETTLEMENT_NOTE, code, name, documentNumberSeriesPrefix,
                invoice);
    }

    @Atomic
    public static FinantialDocumentType createForReimbursementNote(final String code, final LocalizedString name,
            final String documentNumberSeriesPrefix, boolean invoice) {
        return new FinantialDocumentType(FinantialDocumentTypeEnum.REIMBURSEMENT_NOTE, code, name, documentNumberSeriesPrefix,
                invoice);
    }

    @Atomic
    public static void initializeFinantialDocumentType() {
        if (FinantialDocumentType.findAll().count() == 0) {
            FinantialDocumentType.createForCreditNote(
                    "NA",
                    treasuryBundleI18N("label.FinantialDocumentType.CreditNote"), "NA", true);
            FinantialDocumentType.createForDebitNote(
                    "ND",
                    treasuryBundleI18N("label.FinantialDocumentType.DebitNote"), "ND", true);
            FinantialDocumentType.createForSettlementNote(
                    "NP",
                    treasuryBundleI18N("label.FinantialDocumentType.SettlementNote"), "NP", true);

            FinantialDocumentType.createForReimbursementNote(
                    "NR",
                    treasuryBundleI18N("label.FinantialDocumentType.ReimbursementNote"), "NR", true);

        }

    }

}
