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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.document.TreasuryDocumentTemplate;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class FinantialEntity extends FinantialEntity_Base {

    public static final Comparator<FinantialEntity> COMPARE_BY_NAME = new Comparator<FinantialEntity>() {

        @Override
        public int compare(FinantialEntity o1, FinantialEntity o2) {
            if (FinantialInstitution.COMPARATOR_BY_NAME.compare(o1.getFinantialInstitution(),
                    o2.getFinantialInstitution()) != 0) {
                return FinantialInstitution.COMPARATOR_BY_NAME.compare(o1.getFinantialInstitution(),
                        o2.getFinantialInstitution());
            }

            int c = o1.getName().getContent().compareTo(o2.getName().getContent());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected FinantialEntity() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected FinantialEntity(final FinantialInstitution finantialInstitution, final String code, final LocalizedString name) {
        this();
        setFinantialInstitution(finantialInstitution);
        setCode(code);
        setName(name);

        checkRules();
    }

    public void checkRules() {
        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.FinantialEntity.finantialInstitution.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.FinantialEntity.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.FinantialEntity.name.required");
        }

        if (findByCode(getFinantialInstitution(), getCode()).count() > 1) {
            throw new TreasuryDomainException("error.FinantialEntity.code.duplicated");
        }

        getName().getLocales().stream().forEach(l -> {
            if (findByName(getFinantialInstitution(), getName().getContent(l)).count() > 1) {
                throw new TreasuryDomainException("error.FinantialEntity.name.duplicated", l.toString());
            } ;
        });
    }

    @Atomic
    public void edit(final String code, final LocalizedString name) {
        setCode(code);
        setName(name);

        checkRules();
    }

    public boolean isDeletable() {
        return getTariffSet().isEmpty() && getFixedTariffSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FinantialEntity.cannot.delete");
        }

        this.setDomainRoot(null);
        this.setFinantialInstitution(null);

        for (TreasuryDocumentTemplate template : this.getTreasuryDocumentTemplatesSet()) {
            template.delete();
        }

        super.deleteDomainObject();
    }

    public TreasuryDocumentTemplate getDocumentTemplate(FinantialDocumentType type) {
        for (TreasuryDocumentTemplate documentTemplate : getTreasuryDocumentTemplatesSet()) {
            if (documentTemplate.getFinantialDocumentType().getType().equals(type.getType())) {
                return documentTemplate;
            }
        }

        return null;
    }

    @Atomic
    public static FinantialEntity create(final FinantialInstitution finantialInstitution, final String code,
            final LocalizedString name) {
        return new FinantialEntity(finantialInstitution, code, name);
    }

    public Set<FixedTariff> getFixedTariffSet() {
        return this.getTariffSet().stream().filter(x -> x instanceof FixedTariff).map(FixedTariff.class::cast)
                .collect(Collectors.toSet());
    }

    public void markSeriesAsDefault(final Series series) {
        if (!Boolean.TRUE.equals(getFinantialInstitution().getSeriesByFinantialEntity())) {
            throw new IllegalStateException("default series is not by finantial entity");
        }

        for (final Series s : getSeriesSet()) {
            s.setDefaultSeries(false);
        }

        series.setDefaultSeries(true);
    }

    /*
     * SERVICES
     */

    public static Stream<FinantialEntity> findAll() {
        return FenixFramework.getDomainRoot().getFinantialEntitiesSet().stream();
    }

    public static Stream<FinantialEntity> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getFinantialEntitiesSet().stream();
    }

    public static Stream<FinantialEntity> findByCode(final FinantialInstitution finantialInstitution, final String code) {
        return find(finantialInstitution).filter(fe -> fe.getCode().equalsIgnoreCase(code));
    }

    // TODO legidio finantialInstitution not used
    public static Stream<FinantialEntity> findByName(final FinantialInstitution finantialInstitution, final String name) {
        return findAll().filter(fe -> LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(fe.getName(), name));
    }

    public static Stream<FinantialEntity> findWithFrontOfficeAccessFor(String username) {
        return findAll().filter(e -> TreasuryAccessControlAPI.isFrontOfficeMember(username, e));
    }

    public static Stream<FinantialEntity> findWithBackOfficeAccessFor(String username) {
        return findAll().filter(l -> TreasuryAccessControlAPI.isBackOfficeMember(username, l));
    }

}
