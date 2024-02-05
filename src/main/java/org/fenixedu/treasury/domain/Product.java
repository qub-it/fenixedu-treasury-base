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

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Product extends Product_Base {

    private static final int MIN_DESCRIPTION_LENGTH = 2;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    public static final int MAX_CODE_LENGTH = 20;

    public static final Comparator<Product> COMPARE_BY_NAME = (o1, o2) -> {
        int c = o1.getName().getContent().compareTo(o2.getName().getContent());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<Product> COMPARE_BY_INSTALLMENT_NUMBER_AND_NAME = (o1, o2) -> {
        int c = Integer.compare(o1.getTuitionInstallmentOrder(), o2.getTuitionInstallmentOrder());

        return c != 0 ? c : COMPARE_BY_NAME.compare(o1, o2);
    };

    protected Product() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected Product(final ProductGroup productGroup, final String code, final LocalizedString name,
            final LocalizedString unitOfMeasure, final boolean active, final boolean legacy, final int tuitionInstallmentOrder,
            final VatType vatType, final List<FinantialInstitution> finantialInstitutions,
            VatExemptionReason vatExemptionReason) {
        this();
        setProductGroup(productGroup);
        setCode(code);
        setName(name);
        setUnitOfMeasure(unitOfMeasure);
        setActive(active);
        setLegacy(legacy);
        setTuitionInstallmentOrder(tuitionInstallmentOrder);
        setVatType(vatType);
        setVatExemptionReason(vatExemptionReason);
        setSystemProduct(false);
        updateFinantialInstitutions(finantialInstitutions);

        checkRules();
    }

    public void checkRules() {
        if (getVatType() == null) {
            throw new TreasuryDomainException("error.Product.vatType.required");
        }
        if (getProductGroup() == null) {
            throw new TreasuryDomainException("error.Product.productGroup.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.Product.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.Product.name.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.Product.code.duplicated", getCode());
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getUnitOfMeasure())) {
            throw new TreasuryDomainException("error.Product.unitOfMeasure.required");
        }

        if (getCode().length() > MAX_CODE_LENGTH) {
            throw new TreasuryDomainException("error.Product.code.size.exceded");
        }

        getName().getLocales().stream().forEach(lc -> {
            if (StringUtils.isNotEmpty(getName().getContent(lc)) && getName().getContent(lc).length() < MIN_DESCRIPTION_LENGTH) {
                throw new TreasuryDomainException("error.Product.description.length.minimum");
            }

            if (StringUtils.isNotEmpty(getName().getContent(lc)) && getName().getContent(lc).length() > MAX_DESCRIPTION_LENGTH) {
                throw new TreasuryDomainException("error.Product.description.length.maximum");
            }
        });

    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isLegacy() {
        return getLegacy();
    }

    @Atomic
    public void edit(LocalizedString name, LocalizedString unitOfMeasure, boolean active, boolean legacy,
            int tuitionInstallmentOrder, VatType vatType, ProductGroup productGroup,
            List<FinantialInstitution> finantialInstitutions, VatExemptionReason vatExemptionReason) {

        if (!TreasuryPlataformDependentServicesFactory.implementation().isProductCertified(this)) {
            setName(name);
        }

        setUnitOfMeasure(unitOfMeasure);
        setActive(active);
        setLegacy(legacy);
        setTuitionInstallmentOrder(tuitionInstallmentOrder);
        setVatType(vatType);
        setProductGroup(productGroup);
        setVatExemptionReason(vatExemptionReason);

        updateFinantialInstitutions(finantialInstitutions);

        checkRules();
    }

    private static List<Predicate<Product>> IS_DELETABLE_PRODUCT_PREDICATES_LIST = new ArrayList<>();

    public static void registerIsDeletableProductPredicate(Predicate<Product> deletablePredicate) {
        IS_DELETABLE_PRODUCT_PREDICATES_LIST.add(deletablePredicate);
    }

    public boolean isDeletable() {
        return getInvoiceEntriesSet().isEmpty() && //
                getTreasuryExemptionSet().isEmpty() && //
                getTreasuryEventsSet().isEmpty() && //
                getAdvancePaymentTreasurySettings() == null && //
                getTreasurySettings() == null && //
                getTariffSet().isEmpty() && //
                getPaymentPlanSettings() == null && //
                getPaymentPlanConfiguratorsSet().isEmpty() && //
                !Boolean.TRUE.equals(getSystemProduct()) && //
                (IS_DELETABLE_PRODUCT_PREDICATES_LIST == null || IS_DELETABLE_PRODUCT_PREDICATES_LIST.isEmpty()
                        || IS_DELETABLE_PRODUCT_PREDICATES_LIST.stream().allMatch(p -> p.test(this)));
    }

    public boolean isTransferBalanceProduct() {
        return this == TreasurySettings.getInstance().getTransferBalanceProduct();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Product.cannot.delete");
        }
        setProductGroup(null);
        setDomainRoot(null);
        setVatType(null);
        for (FinantialInstitution inst : getFinantialInstitutionsSet()) {
            for (Tariff t : this.getTariffsSet(inst)) {
                t.delete();
            }
            this.removeFinantialInstitutions(inst);
        }
        setVatExemptionReason(null);

        deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<Product> findAll() {
        return FenixFramework.getDomainRoot().getProductsSet().stream();
    }

    public static Stream<Product> findAllActive() {
        return FenixFramework.getDomainRoot().getProductsSet().stream().filter(x -> x.getActive() == true);
    }

    public static Stream<Product> findByCode(final String code) {
        return findAll().filter(p -> p.getCode().equalsIgnoreCase(code));
    }

    public static Optional<Product> findUniqueByCode(final String code) {
        return findByCode(code).findFirst();
    }

    public static Stream<Product> findByName(final String name) {
        return findAll().filter(p -> LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(p.getName(), name));
    }

    public static LocalizedString defaultUnitOfMeasure() {
        return treasuryBundleI18N("label.unitOfMeasure.default");
    }

    public static Stream<Product> findAllLegacy() {
        return findAll().filter(p -> p.isLegacy());
    }

    @Atomic
    public static Product create(final ProductGroup productGroup, final String code, final LocalizedString name,
            final LocalizedString unitOfMeasure, final boolean active, final boolean legacy, final int tuitionInstallmentOrder,
            final VatType vatType, final List<FinantialInstitution> finantialInstitutions,
            final VatExemptionReason vatExemptionReason) {
        return new Product(productGroup, code, name, unitOfMeasure, active, legacy, tuitionInstallmentOrder, vatType,
                finantialInstitutions, vatExemptionReason);
    }

    public Stream<Tariff> getTariffs(FinantialInstitution finantialInstitution) {
        return this.getTariffSet().stream()
                .filter(x -> x.getFinantialEntity().getFinantialInstitution().equals(finantialInstitution));
    }

    public Set<Tariff> getTariffsSet(FinantialInstitution finantialInstitution) {
        return getTariffs(finantialInstitution).collect(Collectors.toSet());
    }

    public Stream<FixedTariff> getFixedTariffs(FinantialInstitution finantialInstitution) {
        return this.getTariffSet().stream().filter(x -> x instanceof FixedTariff)
                .filter(x -> x.getFinantialEntity().getFinantialInstitution().equals(finantialInstitution))
                .map(FixedTariff.class::cast);
    }

    public Set<FixedTariff> getFixedTariffsSet(FinantialInstitution finantialInstitution) {
        return getFixedTariffs(finantialInstitution).collect(Collectors.toSet());
    }

    public Stream<Tariff> getActiveTariffs(FinantialInstitution finantialInstitution, DateTime when) {
        return this.getTariffSet().stream()
                .filter(x -> x.getFinantialEntity().getFinantialInstitution().equals(finantialInstitution))
                .filter(x -> x.getBeginDate() != null && x.getBeginDate().isBefore(when)
                        && (x.getEndDate() == null || x.getEndDate().isAfter(when)));

    }

    public Set<Tariff> getActiveTariffsSet(FinantialInstitution finantialInstitution) {
        return getActiveTariffs(finantialInstitution, new DateTime()).collect(Collectors.toSet());
    }

    public void updateFinantialInstitutions(List<FinantialInstitution> finantialInstitutions) {
        if (finantialInstitutions == null) {
            finantialInstitutions = Collections.emptyList();
        }
        for (FinantialInstitution inst : this.getFinantialInstitutionsSet()) {
            if (!finantialInstitutions.contains(inst)) {
                if (this.canRemoveFinantialInstitution(inst)) {
                    this.removeFinantialInstitutions(inst);
                    inst.removeAvailableProducts(this);
                } else {
                    throw new TreasuryDomainException("error.product.cannot.remove.finantialentity");
                }
            }
        }

        for (FinantialInstitution inst2 : finantialInstitutions) {
            if (!this.getFinantialInstitutionsSet().contains(inst2)) {
                this.addFinantialInstitutions(inst2);
                inst2.addAvailableProducts(this);
            }
        }
    }

    private boolean canRemoveFinantialInstitution(FinantialInstitution inst) {
        return true;
//        return !inst.getFinantialEntitiesSet().stream()
//                .anyMatch(x -> x.getTariffSet().stream().anyMatch(y -> y.getProduct().equals(this)));
    }

    @Atomic
    public static int deleteOrphanProducts() {
        int count = 0;
        for (Product x : Product.findAll().collect(Collectors.toList())) {
            if (x.getActive() == false && x.isDeletable()) {
                x.delete();
                count++;
            }
        }
        return count;
    }
}
