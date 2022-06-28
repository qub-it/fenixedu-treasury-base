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

        return DebitEntry.findActive(this) //
             // TODO: This filter is superfluous, is already done by DebitEntry::findActive
                // TODO: Remove the following statement and test the result is equal
                .filter(d -> !d.isEventAnnuled()) 
                .count() > 0;
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

    @Deprecated
    /** Must be replaced by {@link IAcademicTreasuryEvent#getNetExemptedAmount()} */
    public BigDecimal getExemptedAmount() {
        BigDecimal result =
                DebitEntry.findActive(this).map(l -> l.getNetExemptedAmount()).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO);

        result = result.add(CreditEntry.findActive(this).filter(l -> l.isFromExemption()).map(l -> l.getNetAmount())
                .reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO));

        return result;
    }

    // TODO: Ensure ::getExemptedAmount is replaced by ::getNetExemptedAmount
    public BigDecimal getNetExemptedAmount() {
        return getExemptedAmount();
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
    
    public boolean isEventAccountedAsTuition() {
        return false;
    }
    
    public boolean isEventDiscountInTuitionFee() {
        return false;
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
    
    public Set<TreasuryExemption> getActiveTreasuryExemptions() {
        return DebitEntry.findActive(this).flatMap(d -> d.getTreasuryExemptionsSet().stream())
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
