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
package org.fenixedu.treasury.domain.event;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class TreasuryEvent extends TreasuryEvent_Base {

    public static enum TreasuryEventKeys {
        EXECUTION_YEAR, EXECUTION_SEMESTER, DEGREE_CODE, COPIED_FROM_DEBIT_ENTRY_ID, COPY_DEBIT_ENTRY_RESPONSIBLE;

        public LocalizedString getDescriptionI18N() {
            return treasuryBundleI18N("label." + TreasuryEvent.class.getSimpleName() + "." + name());
        }

    }

    public abstract String getERPIntegrationMetadata();

    protected TreasuryEvent() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final Product product, final LocalizedString description) {
        setProduct(product);
        setDescription(description);
    }

    protected void checkRules() {
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.TreasuryEvent.bennu.required");
        }
    }

    /* -----------------------------
     * FINANTIAL INFORMATION RELATED
     * -----------------------------
     */

    public boolean isExempted(final Product product) {
        return TreasuryExemption.findUnique(this, product).isPresent();
    }

    public boolean isChargedWithDebitEntry() {
        return isChargedWithDebitEntry(null);
    }

    public boolean isChargedWithDebitEntry(final Product product) {
        if (product != null) {
            return DebitEntry.findActive(this, product).count() > 0;
        }

        return DebitEntry.findActive(this).filter(d -> !d.isEventAnnuled()).count() > 0;
    }

    // TODO: getTotalAmount()
    public BigDecimal getAmountToPay() {
        return getAmountToPay(null, null);
    }

    // TODO: getTotalAmount()
    public BigDecimal getAmountToPay(final Customer customer, final Product product) {
        Stream<? extends DebitEntry> s = product != null ? DebitEntry.findActive(this, product) : DebitEntry.findActive(this);
        if (customer != null) {
            s = s.filter(d -> d.getDebtAccount().getCustomer() == customer);
        }

        final BigDecimal result = s.map(d -> d.getTotalAmount()).reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO)
                .subtract(getCreditAmount(customer, product));

        return TreasuryConstants.isPositive(result) ? result : BigDecimal.ZERO;
    }

    public BigDecimal getInterestsAmountToPay() {
        return getInterestsAmountToPay(null, null);
    }
    
    public BigDecimal getInterestsAmountToPay(final Customer customer) {
        return getInterestsAmountToPay(customer, null);
    }

    public BigDecimal getInterestsAmountToPay(final Customer customer, final Product product) {
        final Product interestProduct = TreasurySettings.getInstance().getInterestProduct();
        Stream<? extends DebitEntry> s = DebitEntry.findActive(this)
                .filter(d -> d.getProduct() == interestProduct)
                .filter(d -> product == null || (d.getDebitEntry() != null && d.getDebitEntry().getProduct() == product));

        if (customer != null) {
            s = s.filter(d -> d.getDebtAccount().getCustomer() == customer);
        }

        final BigDecimal result = s.map(d -> d.getTotalAmount()).reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO)
                .subtract(getInterestsCreditAmount(product));

        return TreasuryConstants.isPositive(result) ? result : BigDecimal.ZERO;

    }

    public BigDecimal getCreditAmount() {
        return getCreditAmount(null, null);
    }

    public BigDecimal getCreditAmount(final Customer customer, final Product product) {
        Stream<? extends CreditEntry> s = product != null ? CreditEntry.findActive(this, product) : CreditEntry.findActive(this);

        if (customer != null) {
            s = s.filter(d -> d.getDebtAccount().getCustomer() == customer);
        }

        return s.map(c -> c.getAmountWithVat()).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getInterestsCreditAmount() {
        return getInterestsCreditAmount(null);
    }

    public BigDecimal getInterestsCreditAmount(final Product product) {
        final Product interestProduct = TreasurySettings.getInstance().getInterestProduct();

        return CreditEntry.findActive(this).filter(c -> c.getDebitEntry().getProduct() == interestProduct)
                .filter(c -> product == null || (c.getDebitEntry().getDebitEntry() != null
                        && c.getDebitEntry().getDebitEntry().getProduct() == product))
                .map(c -> c.getAmountWithVat()).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getPayedAmount() {
        return getAmountToPay().subtract(getRemainingAmountToPay());
    }

    public BigDecimal getRemainingAmountToPay() {
        return getRemainingAmountToPay(null);
    }

    public BigDecimal getRemainingAmountToPay(final Product product) {
        BigDecimal result = BigDecimal.ZERO;

        for (final DebitEntry debitEntry : DebitEntry.findActive(this).collect(Collectors.<DebitEntry> toSet())) {
            result = result.add(debitEntry.getOpenAmount());
        }

        return TreasuryConstants.isPositive(result) ? result : BigDecimal.ZERO;
    }

    public BigDecimal getExemptedAmount() {
        BigDecimal result =
                DebitEntry.findActive(this).map(l -> l.getExemptedAmount()).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO);

        result = result.add(CreditEntry.findActive(this).filter(l -> l.isFromExemption()).map(l -> l.getAmountWithVat())
                .reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO));

        return result;
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public Set<Product> getPossibleProductsToExempt() {
        return Sets.newHashSet(getProduct());
    }

    public boolean isDeletable() {
        return true;
    }

    public abstract LocalDate getTreasuryEventDate();

    public abstract String getDegreeCode();

    public abstract String getDegreeName();

    public abstract String getExecutionYearName();
    
    public abstract void copyDebitEntryInformation(final DebitEntry sourceDebitEntry, final DebitEntry copyDebitEntry);

    /**
     * This method is used to find a tariff, which might be academic or other, in which the types are not known by this module
     * 
     * @param product
     * @param when
     * @return
     */
    public abstract Optional<Tariff> findMatchTariff(FinantialEntity finantialEntity, Product product, LocalDate when);
    
    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryEvent.cannot.delete");
        }

        setDebtAccount(null);
        setProduct(null);
        setDomainRoot(null);

        super.deleteDomainObject();
    }

    @Atomic
    public void annulAllDebitEntries(final String reason) {

        while (DebitEntry.findActive(this).map(DebitEntry.class::cast).count() > 0) {
            final DebitEntry debitEntry = DebitEntry.findActive(this).map(DebitEntry.class::cast).findFirst().get();

            if (debitEntry.isProcessedInClosedDebitNote() && TreasuryConstants.isEqual(debitEntry.getAvailableAmountForCredit(), BigDecimal.ZERO)) {
                debitEntry.annulOnEvent();
                continue;
            }

            if (debitEntry.isAnnulled()) {
                continue;
            }

            if (!debitEntry.isProcessedInDebitNote()) {
                final DebitNote debitNote = DebitNote.create(debitEntry.getDebtAccount(),
                        DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                debitEntry.getDebtAccount().getFinantialInstitution()).get(),
                        new DateTime());

                debitNote.addDebitNoteEntries(Lists.newArrayList(debitEntry));
            }

            if (!debitEntry.isProcessedInClosedDebitNote()) {
                ((DebitNote) debitEntry.getFinantialDocument()).anullDebitNoteWithCreditNote(reason, false);
            }

            // ensure interest debit entries are closed in document entry
            for (final DebitEntry otherDebitEntry : ((DebitNote) debitEntry.getFinantialDocument()).getDebitEntriesSet()) {
                for (final DebitEntry interestDebitEntry : otherDebitEntry.getInterestDebitEntriesSet()) {
                    if(interestDebitEntry.isAnnulled()) {
                        continue;
                    }
                    
                    if (!interestDebitEntry.isProcessedInDebitNote()) {
                        final DebitNote debitNoteForUnprocessedEntries =
                                DebitNote
                                        .create(debitEntry.getDebtAccount(),
                                                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                                        debitEntry.getDebtAccount().getFinantialInstitution()).get(),
                                new DateTime());
                        interestDebitEntry.setFinantialDocument(debitNoteForUnprocessedEntries);
                    }

                    if (!interestDebitEntry.isProcessedInClosedDebitNote()) {
                        ((DebitNote) interestDebitEntry.getFinantialDocument()).anullDebitNoteWithCreditNote(reason,
                                false);
                    }
                }
            }

            if (debitEntry.isProcessedInClosedDebitNote()) {
                ((DebitNote) debitEntry.getFinantialDocument()).anullDebitNoteWithCreditNote(reason, false);
            }

            for (final DebitEntry otherDebitEntry : ((DebitNote) debitEntry.getFinantialDocument()).getDebitEntriesSet()) {
                for (final DebitEntry interestDebitEntry : otherDebitEntry.getInterestDebitEntriesSet()) {
                    interestDebitEntry.annulOnEvent();
                }

                otherDebitEntry.annulOnEvent();
            }
        }

        for (final DebitEntry debitEntry : getDebitEntriesSet()) {
            for (final DebitEntry interestDebitEntry : debitEntry.getInterestDebitEntriesSet()) {
                interestDebitEntry.annulOnEvent();
            }

            debitEntry.annulOnEvent();
        }

    }

    public boolean isAbleToDeleteAllDebitEntries() {
        return DebitEntry.findActive(this).map(l -> l.isDeletable()).reduce((a, c) -> a && c).orElse(Boolean.TRUE);
    }

    public void invokeSettlementCallbacks() {
    }
    
    public Set<TreasuryExemption> getActiveTreasuryEvents() {
        return getTreasuryExemptionsSet().stream().filter(t -> !t.getDebitEntry().isEventAnnuled())
                .collect(Collectors.<TreasuryExemption> toSet());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        super.setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<? extends TreasuryEvent> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryEventsSet().stream();
    }
    
    public static Stream<? extends TreasuryEvent> find(final Customer customer) {
        return customer.getTreasuryEventsSet().stream();
    }

}
