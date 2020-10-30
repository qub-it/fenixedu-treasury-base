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
