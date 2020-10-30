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
package org.fenixedu.treasury.domain;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Product extends Product_Base {

    public static final int MAX_CODE_LENGTH = 20;
    public static final Comparator<Product> COMPARE_BY_NAME = new Comparator<Product>() {

        @Override
        public int compare(Product o1, Product o2) {
            int c = o1.getName().getContent().compareTo(o2.getName().getContent());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }

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

    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isLegacy() {
        return getLegacy();
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final LocalizedString unitOfMeasure, boolean active,
            final boolean legacy, final int tuitionInstallmentOrder, VatType vatType, final ProductGroup productGroup,
            final List<FinantialInstitution> finantialInstitutions, VatExemptionReason vatExemptionReason) {
        setCode(code);
        setName(name);
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

    public boolean isDeletable() {
//        for (FinantialInstitution finantialInstitution : getFinantialInstitutionsSet()) {
//            if (!canRemoveFinantialInstitution(finantialInstitution)) {
//                return false;
//            }
//        }
        return getInvoiceEntriesSet().isEmpty() && getTreasuryExemptionSet().isEmpty() && getTreasuryEventsSet().isEmpty()
                && getAdvancePaymentTreasurySettings() == null && getTreasurySettings() == null;
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
            final LocalizedString unitOfMeasure, final boolean active, final boolean legacy, final int tuitionInstallmentOrder,final VatType vatType,
            final List<FinantialInstitution> finantialInstitutions, final VatExemptionReason vatExemptionReason) {
        return new Product(productGroup, code, name, unitOfMeasure, active, legacy, tuitionInstallmentOrder, vatType, finantialInstitutions,
                vatExemptionReason);
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
