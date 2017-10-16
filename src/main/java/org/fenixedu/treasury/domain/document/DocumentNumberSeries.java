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

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;;

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
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
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
        setReplacePrefix(replacePrefix);

        if (isReplacePrefix()) {
            setReplacingPrefix(replacingPrefix);
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
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getDocumentNumberSeriesSet().stream();
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

    public static Optional<DocumentNumberSeries> findUniqueDefault(final FinantialDocumentType finantialDocumentType,
            final FinantialInstitution finantialInstitution) {
        if (!Series.findUniqueDefault(finantialInstitution).isPresent()) {
            return Optional.<DocumentNumberSeries> empty();
        }

        return Optional.of(find(finantialDocumentType, Series.findUniqueDefault(finantialInstitution).get()));
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
