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

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class DocumentNumberSeries extends DocumentNumberSeries_Base {

    public static Comparator<DocumentNumberSeries> COMPARE_BY_DEFAULT = (x, y) -> {
        if (x.getSeries().isDefaultSeries()) {
            return -1;
        }
        return 1;
    };
    public static Comparator<DocumentNumberSeries> COMPARE_BY_NAME = (x, y) -> {
        int c = x.getSeries().getName().compareTo(y.getSeries().getName());
        return c != 0 ? c : x.getExternalId().compareTo(y.getExternalId());
    };

    protected DocumentNumberSeries() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected DocumentNumberSeries(final FinantialDocumentType finantialDocumentType, final Series series) {
        this();
        setCounter(0);
        setFinantialDocumentType(finantialDocumentType);
        setSeries(series);
        setReplacePrefix(false);
        setReplacingPrefix(null);

        checkRules();
    }

    private void checkRules() {
        if (getFinantialDocumentType() == null) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.finantialDocumentType.required");
        }

        if (getSeries() == null) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.series.required");
        }

        if (isReplacePrefix() && Strings.isNullOrEmpty(getReplacingPrefix())) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.replacePrefix.wrong.arguments");
        }

        if (!isReplacePrefix() && !Strings.isNullOrEmpty(getReplacingPrefix())) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.replacePrefix.wrong.arguments");
        }

        find(getFinantialDocumentType(), getSeries());
    }

    public int getSequenceNumber() {
        return getCounter();
    }

    @Atomic
    // TODO: The method name is misleading for two reasons:
    // 1) The method starting with getX is misleading because it has side effects. Getters should not have side effects
    // 2) The sequence number returned is after the counter is incremented
    // 3) The method should be called incrementAndGetSequenceNumber()
    public int getSequenceNumberAndIncrement() {
        if (this.getSeries().getActive() == false) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.document.is.in.closed.series");
        }
        int count = getCounter();
        count++;
        setCounter(count);

        return count;
    }

    public boolean isReplacePrefix() {
        return getReplacePrefix();
    }

    public boolean isDeletable() {
        return getFinantialDocumentsSet().isEmpty() && getPaymentCodePoolPaymentSeriesSet().isEmpty();
    }

    public void editReplacingPrefix(final boolean replacePrefix, final String replacingPrefix) {
        if (!getFinantialDocumentsSet().isEmpty()) {
            throw new RuntimeException("edit replacing prefix not possible. documentNumberSeries with finantial documents");
        }

        setReplacePrefix(replacePrefix);
        if (isReplacePrefix()) {
            setReplacingPrefix(replacingPrefix);
        } else {
            setReplacingPrefix(null);
        }

        checkRules();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.cannot.delete");
        }

        setDomainRoot(null);
        setFinantialDocumentType(null);
        setSeries(null);
        deleteDomainObject();
    }

    public static Stream<DocumentNumberSeries> findAll() {
        return FenixFramework.getDomainRoot().getDocumentNumberSeriesSet().stream();
    }

    public static DocumentNumberSeries find(final FinantialDocumentType finantialDocumentType, final Series series) {
        final Set<DocumentNumberSeries> result = finantialDocumentType.getDocumentNumberSeriesSet().stream()
                .filter(dns -> dns.getSeries().getCode().equals(series.getCode())
                        && dns.getSeries().getFinantialInstitution().equals(series.getFinantialInstitution()))
                .collect(Collectors.toSet());
        if (result.size() > 1) {
            throw new TreasuryDomainException("error.DocumentNumberSeries.not.unique.in.finantialDocumentType.and.series");
        }
        return result.stream().findFirst().orElse(null);
    }

    public static Stream<DocumentNumberSeries> find(final FinantialDocumentType finantialDocumentType,
            final FinantialInstitution finantialInstitution) {
        return finantialDocumentType.getDocumentNumberSeriesSet().stream()
                .filter(x -> x.getSeries().getFinantialInstitution().getCode().equals(finantialInstitution.getCode()));
    }

    public static DocumentNumberSeries findUniqueDefaultSeries(FinantialDocumentType finantialDocumentType,
            FinantialEntity finantialEntity) {
        if (Series.findUniqueDefaultSeries(finantialEntity) == null) {
            return null;
        }

        return find(finantialDocumentType, Series.findUniqueDefaultSeries(finantialEntity));
    }

    @Atomic
    public static DocumentNumberSeries create(final FinantialDocumentType finantialDocumentType, final Series series) {
        return new DocumentNumberSeries(finantialDocumentType, series);
    }

    public long getPreparingDocumentsCount() {
        return this.getFinantialDocumentsSet().stream().filter(x -> x.isPreparing()).count();
    }

    public long getDocumentsCount() {
        return this.getFinantialDocumentsSet().stream().count();
    }

    public long getClosedDocumentsCount() {
        return this.getFinantialDocumentsSet().stream().filter(x -> x.isClosed()).count();
    }

    public static Stream<DocumentNumberSeries> applyActiveSelectableAndDefaultSorting(Stream<DocumentNumberSeries> stream) {
        return stream.filter(x -> x.getSeries().getActive()).filter(d -> d.getSeries().isSelectable())
                .sorted(COMPARE_BY_DEFAULT.thenComparing(COMPARE_BY_NAME));
    }

    public String documentNumberSeriesPrefix() {
        if (isReplacePrefix()) {
            return getReplacingPrefix();
        }

        return getFinantialDocumentType().getDocumentNumberSeriesPrefix();
    }

}
