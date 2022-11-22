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
package org.fenixedu.treasury.domain.exemption;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentStateType;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryExemption extends TreasuryExemption_Base {

    public TreasuryExemption() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TreasuryExemption(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal netAmountToExempt, final DebitEntry debitEntry) {
        this();

        if(Boolean.TRUE.equals(debitEntry.getCalculatedAmountsOverriden())) {
            throw new TreasuryDomainException("error.DebitEntry.exempt.not.possible.due.to.overriden.calculated.amounts");
        }
        
        for (final CreditEntry creditEntry : debitEntry.getCreditEntriesSet()) {
            if (!creditEntry.getFinantialDocument().isAnnulled() && !creditEntry.isFromExemption()) {
                throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.with.credit.not.from.exemption");
            }
        }

        setTreasuryExemptionType(treasuryExemptionType);
        setTreasuryEvent(treasuryEvent);
        setReason(reason);

        /*
         * For now percentages are not supported because they are complex to deal with
         */
        setExemptByPercentage(false);

        // Check the scale of netAmountToExempt has at most the decimal places for cents
        if (netAmountToExempt.scale() > debitEntry.getDebtAccount().getFinantialInstitution().getCurrency()
                .getDecimalPlacesForCents()) {
            throw new IllegalStateException("The netAmountToExempt has scale above the currency decimal places for cents");
        }

        setNetAmountToExempt(netAmountToExempt);

        setDebitEntry(debitEntry);
        setProduct(debitEntry.getProduct());

        checkRules();

        if (getNetAmountToExempt().scale() > getDebitEntry().getDebtAccount().getFinantialInstitution().getCurrency()
                .getDecimalPlacesForCents()) {
            throw new IllegalStateException("The netAmountToExempt has scale above the currency decimal places for cents");
        }

        getDebitEntry().exempt(this);
    }

    private void checkRules() {
        if (getTreasuryExemptionType() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.treasuryExemptionType.required");
        }

        if (getNetAmountToExempt() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.required");
        }

        if (!TreasuryConstants.isPositive(getNetAmountToExempt())) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.positive.required");
        }

        if (getDebitEntry() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.required");
        }

        if (getProduct() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.product.required");
        }

        if (Strings.isNullOrEmpty(getReason())) {
            throw new TreasuryDomainException("error.TreasuryExemption.reason.empty");
        }

        if (getDebitEntry().isEventAnnuled()) {
            throw new TreasuryDomainException("error.TreasuryExemption.debit.entry.annuled.in.event");
        }

        if (TreasuryConstants.isGreaterThan(getNetAmountToExempt(),
                getDebitEntry().getAmountWithVat().add(getDebitEntry().getNetExemptedAmount()))) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.higher.than.debitEntry");
        }
    }

    public boolean isExemptByPercentage() {
        return super.getExemptByPercentage();
    }

    @Deprecated
    // TODO: Remove this method when all references are cleaned
    public BigDecimal getExemptedAmount() {
        return this.getNetExemptedAmount();
    }

    public BigDecimal getNetExemptedAmount() {
        if (isExemptByPercentage()) {
            throw new TreasuryDomainException("error.TreasuryExemption.exempted.by.percentage.not.supported");
        }

        return getNetAmountToExempt();
    }

    public boolean isDeletable() {
        boolean creditNoteIsPreparing = getCreditEntry() != null
                && (getCreditEntry().getFinantialDocument() == null || getCreditEntry().getFinantialDocument().isPreparing());

        boolean debitNoteIsPreparing = getCreditEntry() == null
                && (getDebitEntry().getFinantialDocument() == null || getDebitEntry().getFinantialDocument().isPreparing());

        return getDebitEntry().isAnnulled() || creditNoteIsPreparing || debitNoteIsPreparing;
    }

    /**
     * Should not be called directly except by DebitEntry when deleting debit entry
     */
    public void delete() {

        super.setDomainRoot(null);

        super.setTreasuryExemptionType(null);
        super.setTreasuryEvent(null);
        super.setProduct(null);
        super.setCreditEntry(null);

        super.deleteDomainObject();
    }

    @Atomic
    public void revertExemption() {

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }

        if (getCreditEntry() != null) {
            getCreditEntry().getCreditNote().setState(FinantialDocumentStateType.ANNULED);

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

            String reason = "Exemption deleted";
            getCreditEntry().getCreditNote().setAnnulledReason(reason);
            getCreditEntry().getCreditNote().setAnnullmentDate(new DateTime());

            getCreditEntry().getCreditNote()
                    .setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");
        } else if (!getDebitEntry().isAnnulled()) {
            if (getDebitEntry().isProcessedInClosedDebitNote()) {
                throw new TreasuryDomainException(
                        "error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
            }

            getDebitEntry().revertExemptionIfPossibleInPreparingState(this);
        }

        // This is to avoid calling delete directly
        super.setDebitEntry(null);
        delete();
    }

    @Override
    @Deprecated(forRemoval = true)
    // TODO: deprecated - this should be renamed as netAmountToExempt 
    public BigDecimal getValueToExempt() {
        return super.getValueToExempt();
    }

    @Override
    @Deprecated(forRemoval = true)
    // TODO: deprecated - this should be renamed as netAmountToExempt
    public void setValueToExempt(BigDecimal valueToExempt) {
        super.setValueToExempt(valueToExempt);
    }

    // TODO: Replace valueToExempt this by this
    public BigDecimal getNetAmountToExempt() {
        return super.getValueToExempt();
    }

    // TODO: Replace valueToExempt this by this
    public void setNetAmountToExempt(BigDecimal value) {
        super.setValueToExempt(value);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<TreasuryExemption> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream();
    }

    public static Stream<TreasuryExemption> find(final TreasuryExemptionType treasuryExemptionType) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getTreasuryExemptionType() == treasuryExemptionType);
    }

    public static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getTreasuryEvent() == treasuryEvent);
    }

    protected static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent).filter(t -> t.getProduct() == product);
    }

    public static java.util.Optional<TreasuryExemption> findUnique(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent, product).findFirst();
    }

    public static Stream<TreasuryExemption> findByDebtAccount(final DebtAccount debtAccount) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getDebitEntry().getDebtAccount() == debtAccount);
    }

    @Atomic
    public static TreasuryExemption create(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal netAmountToExempt, final DebitEntry debitEntry) {
        return new TreasuryExemption(treasuryExemptionType, treasuryEvent, reason, netAmountToExempt, debitEntry);
    }

}
