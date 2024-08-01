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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Series extends Series_Base {

    private static final Comparator<Series> COMPARATOR_BY_CODE = new Comparator<Series>() {

        @Override
        public int compare(Series o1, Series o2) {
            int c = o1.getCode().compareTo(o2.getCode());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    public static final Comparator<Series> COMPARATOR_BY_DEFAULT = new Comparator<Series>() {

        @Override
        public int compare(final Series o1, final Series o2) {
            if (o1.isDefaultSeries() && o2.isDefaultSeries()) {
                return 1;
            } else if (!o1.isDefaultSeries() && o2.isDefaultSeries()) {
                return -1;
            }

            return COMPARATOR_BY_CODE.compare(o1, o2);
        }
    };

    protected Series() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected Series(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String code,
            LocalizedString name, boolean externSeries, boolean legacy, boolean defaultSeries, boolean selectable) {
        this();

        setActive(true);
        setFinantialInstitution(finantialInstitution);

        if (Boolean.TRUE.equals(finantialInstitution.getSeriesByFinantialEntity())) {
            setFinantialEntity(finantialEntity);
        }

        setCode(code);
        setName(name);
        setExternSeries(externSeries);
        setLegacy(legacy);
        setDefaultSeries(defaultSeries);
        setSelectable(selectable);

        checkRules();
    }

    private void checkRules() {
        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.Series.finantialInstitution.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.Series.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.Series.name.required");
        }

        findByCode(getCode());
        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));

        //Check if the Series exists for All DocumentNumberSeries
        FinantialDocumentType.findAll().forEach(x -> {
            if (this.getDocumentNumberSeriesSet().stream().anyMatch(series -> series.getFinantialDocumentType().equals(x))) {
                //do nothing
            } else {
                this.addDocumentNumberSeries(new DocumentNumberSeries(x, this));
            }
        });

        if (Boolean.TRUE.equals(getFinantialInstitution().getSeriesByFinantialEntity())) {
            if (getFinantialEntity() != null && findDefault(getFinantialEntity()).count() > 1) {
                throw new TreasuryDomainException("error.Series.default.not.unique");
            }
        } else {
            if (findDefault(getFinantialInstitution()).count() > 1) {
                throw new TreasuryDomainException("error.Series.default.not.unique");
            }
        }
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean externSeries, final boolean legacy,
            final boolean active, final boolean selectable) {
        setName(name);
        setActive(active);

        if (!code.equalsIgnoreCase(getCode())) {
            if (this.isSeriesUsedForAnyDocument()) {
                throw new TreasuryDomainException("error.Series.invalid.series.type.in.used.series");
            }
            setCode(code);
        }

        if (externSeries != getExternSeries()) {
            if (this.isSeriesUsedForAnyDocument()) {
                throw new TreasuryDomainException("error.Series.invalid.series.type.in.used.series");
            }
            setExternSeries(externSeries);
        }

        if (legacy != getLegacy()) {
            if (this.isSeriesUsedForAnyDocument()) {
                throw new TreasuryDomainException("error.Series.invalid.series.type.in.used.series");
            }
            setLegacy(legacy);
        }

        setSelectable(selectable);

        checkRules();
    }

    private boolean isSeriesUsedForAnyDocument() {
        return this.getDocumentNumberSeriesSet().stream().anyMatch(x -> !x.getFinantialDocumentsSet().isEmpty());
    }

    public boolean isDeletable() {
        if (this.getDocumentNumberSeriesSet().stream().anyMatch(x -> x.isDeletable() == false)) {
            return false;
        }
        return true;
    }

    public boolean isDefaultSeries() {
        return super.getDefaultSeries();
    }

    public boolean isSelectable() {
        return super.getSelectable();
    }

    public boolean isRegulationSeries() {
        return getFinantialInstitution().getRegulationSeries() == this;
    }

    public boolean isActive() {
        return super.getActive();
    }

    public boolean isExternSeries() {
        return super.getExternSeries();
    }

    public boolean isLegacy() {
        return super.getLegacy();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Series.cannot.delete");
        }

        setDomainRoot(null);
        for (DocumentNumberSeries ser : getDocumentNumberSeriesSet()) {
            removeDocumentNumberSeries(ser);
            ser.delete();
        }

        setFinantialInstitution(null);
        setFinantialEntity(null);

        deleteDomainObject();
    }

    public static Set<Series> findAll() {
        return FenixFramework.getDomainRoot().getSeriesSet();
    }

    public static Set<Series> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getSeriesSet();
    }

    public static Set<Series> find(FinantialEntity finantialEntity) {
        return finantialEntity.getSeriesSet();
    }

    // ANIL 2024-07-05 
    //
    // Code should be unique regardless the finantial institution or other entity aggregator
    public static Series findByCode(final String code) {
        Series result = null;

        for (final Series it : findAll()) {
            if (!it.getCode().equalsIgnoreCase(code)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.Series.duplicated.code");
            }

            result = it;
        }

        return result;
    }

    // ANIL 2024-07-05 
    //
    // Like the code, the series name should be unique regardless the finantial institution or other entity aggregator
    public static Series findByName(String name) {
        Series result = null;

        for (final Series it : findAll()) {
            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.Series.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    private static Stream<Series> findDefault(final FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).stream() //
                .filter(s -> s.isDefaultSeries()) //
                .filter(s -> s.getFinantialEntity() == null);
    }

    private static Stream<Series> findDefault(FinantialEntity finantialEntity) {
        return find(finantialEntity).stream().filter(s -> s.isDefaultSeries());
    }

    private static Optional<Series> findUniqueDefault(final FinantialInstitution finantialInstitution) {
        return findDefault(finantialInstitution).filter(s -> s.getFinantialEntity() == null).findFirst();
    }

    private static Optional<Series> findUniqueDefault(FinantialEntity finantialEntity) {
        return findDefault(finantialEntity).findFirst();
    }

    public static Stream<Series> findActiveAndSelectableSeries(FinantialEntity finantialEntity) {
        if (Boolean.TRUE.equals(finantialEntity.getFinantialInstitution().getSeriesByFinantialEntity())) {
            return finantialEntity.getSeriesSet().stream() //
                    .filter(s -> s.isSelectable()) //
                    .filter(s -> s.isActive());
        } else {
            return finantialEntity.getFinantialInstitution().getSeriesSet().stream() //
                    .filter(s -> s.getFinantialEntity() == null) //
                    .filter(s -> s.isSelectable()) //
                    .filter(s -> s.isActive());
        }
    }

    public static Series findUniqueDefaultSeries(FinantialEntity finantialEntity) {
        FinantialInstitution finantialInstitution = finantialEntity.getFinantialInstitution();

        if (Boolean.TRUE.equals(finantialInstitution.getSeriesByFinantialEntity())) {
            return findUniqueDefault(finantialEntity).orElse(null);
        } else {
            return findUniqueDefault(finantialInstitution).orElse(null);
        }
    }

    @Atomic
    public static Series create(FinantialInstitution finantialInstitution, String code, LocalizedString name,
            boolean externSeries, boolean legacy, boolean defaultSeries, boolean selectable) {
        if (Boolean.TRUE.equals(finantialInstitution.getSeriesByFinantialEntity())) {
            throw new IllegalArgumentException("series are by finantial entity. this constructor should not be called");
        }

        return new Series(finantialInstitution, null, code, name, externSeries, legacy, defaultSeries, selectable);
    }

    @Atomic
    public static Series create(FinantialEntity finantialEntity, String code, LocalizedString name, boolean externSeries,
            boolean legacy, boolean defaultSeries, boolean selectable) {
        if (!Boolean.TRUE.equals(finantialEntity.getFinantialInstitution().getSeriesByFinantialEntity())) {
            throw new IllegalArgumentException("series are by finantial institution. this constructor should not be called");
        }

        return new Series(finantialEntity.getFinantialInstitution(), finantialEntity, code, name, externSeries, legacy,
                defaultSeries, selectable);
    }

}
