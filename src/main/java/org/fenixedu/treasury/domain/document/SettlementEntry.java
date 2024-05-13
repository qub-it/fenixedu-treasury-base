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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SettlementEntry extends SettlementEntry_Base {

    public static final Comparator<SettlementEntry> COMPARATOR_BY_ENTRY_DATE_TIME = (o1, o2) -> {
        int c = o1.getEntryDateTime().compareTo(o2.getEntryDateTime());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<SettlementEntry> COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION = (o1, o2) -> {
        if (o1.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() != 0
                && o2.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() != 0) {
            int c1 = Integer.compare(o1.getInvoiceEntry().getProduct().getTuitionInstallmentOrder(),
                    o2.getInvoiceEntry().getProduct().getTuitionInstallmentOrder());

            return c1 != 0 ? c1 : o1.getExternalId().compareTo(o2.getExternalId());
        } else if (o1.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() != 0
                && o2.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() == 0) {
            return -1;
        } else if (o1.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() == 0
                && o2.getInvoiceEntry().getProduct().getTuitionInstallmentOrder() != 0) {
            return 1;
        }

        final int c2 = o1.getDescription().compareTo(o2.getDescription());

        return c2 != 0 ? c2 : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<SettlementEntry> COMPARATOR_BY_ENTRY_ORDER = (o1, o2) -> {
        if (o1.getEntryOrder() == null || o2.getEntryOrder() == null) {
            throw new RuntimeException("error");
        }

        int c = o1.getEntryOrder().compareTo(o2.getEntryOrder());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected SettlementEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCloseDate(new DateTime());
    }

    @Override
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        this.setInvoiceEntry(null);

        super.delete();
    }

    protected SettlementEntry(final InvoiceEntry invoiceEntry, final SettlementNote finantialDocument, final BigDecimal amount,
            final String description, final DateTime entryDateTime, final boolean createInterestIfNeeded) {
        this();
        init(invoiceEntry, finantialDocument, amount, description, entryDateTime);

        if (invoiceEntry.isDebitNoteEntry()) {
            if (TreasuryConstants.isEqual(invoiceEntry.getOpenAmount(), amount) && createInterestIfNeeded) {
                //Check if we need to create more interest for this debitEntry
                DebitEntry debitEntry = (DebitEntry) invoiceEntry;

                List<InterestRateBean> undebitedInterestRateBeansList =
                        debitEntry.calculateUndebitedInterestValue(entryDateTime.toLocalDate());
                for (InterestRateBean undebitedInterestValue : undebitedInterestRateBeansList) {
                    if (TreasuryConstants.isPositive(undebitedInterestValue.getInterestAmount())) {
                        DateTime whenInterestDebitEntryDateTime =
                                undebitedInterestValue.getInterestDebitEntryDateTime() != null ? undebitedInterestValue
                                        .getInterestDebitEntryDateTime() : entryDateTime;

                        DocumentNumberSeries debitNoteSeries = DocumentNumberSeries
                                .find(FinantialDocumentType.findForDebitNote(),
                                        debitEntry.getDebtAccount().getFinantialInstitution())
                                .filter(x -> Boolean.TRUE.equals(x.getSeries().getDefaultSeries())).findFirst().orElse(null);

                        DebitNote interestDebitNote = DebitNote.create(debitEntry.getFinantialEntity(),
                                debitEntry.getDebtAccount(), debitEntry.getDebitNote().getPayorDebtAccount(), debitNoteSeries,
                                new DateTime(), new LocalDate(), null, Collections.emptyMap(), null, null);

                        debitEntry.createInterestRateDebitEntry(undebitedInterestValue, whenInterestDebitEntryDateTime,
                                interestDebitNote);
                    }
                }
            }
        }
    }

    @Override
    protected void init(final DebtAccount debtAccount, final FinantialDocument finantialDocument,
            final FinantialEntryType finantialEntryType, final BigDecimal amount, String description, DateTime entryDateTime) {
        throw new RuntimeException("error.SettlementEntry.use.init.without.finantialEntryType");
    }

    protected void init(final InvoiceEntry invoiceEntry, final FinantialDocument finantialDocument, final BigDecimal amount,
            String description, final DateTime entryDateTime) {
        super.init(finantialDocument.getDebtAccount(), finantialDocument, FinantialEntryType.SETTLEMENT_ENTRY, amount,
                description, entryDateTime);
        setInvoiceEntry(invoiceEntry);
        setFinantialEntity(invoiceEntry.getFinantialEntity());
        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (!(getFinantialDocument() instanceof SettlementNote)) {
            throw new TreasuryDomainException("error.SettlementEntry.finantialDocument.not.settlement.note.type");
        }

        if (getInvoiceEntry().isCreditNoteEntry() && !TreasuryConstants.isEqual(getAmount(), getTotalAmount())) {
            throw new TreasuryDomainException("error.SettlementEntry.creditNoteEntry.total.amount.not.equal");
        }

        if (!TreasuryConstants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.amount.less.than.zero");
        }
    }

    @Override
    public BigDecimal getTotalAmount() {
        return this.getAmount();
    }

    @Override
    @Deprecated
    // TODO ANIL 2024-01-25: The netAmount should not be used and should be removed
    // The amount always includes VAT
    public BigDecimal getNetAmount() {
        return this.getAmount();
    }

    /*
     * ANIL 2024-01-25 :  This method must only be used only in UI or reports
     *
     */
    public BigDecimal getUiAmount() {
        if (getInvoiceEntry().isCreditNoteEntry()
                && Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return getAmount().negate();
        }

        return getAmount();
    }

    /*
     * ANIL 2024-01-25 :  This method must only be used only in UI or reports
     *
     */
    public BigDecimal getUiTotalAmount() {
        if (getInvoiceEntry().isCreditNoteEntry()
                && Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return getTotalAmount().negate();
        }

        return getTotalAmount();
    }

    public String getInvoiceEntryAmountSignal() {
        if (this.getInvoiceEntry().isDebitNoteEntry()) {
            return "";
        } else {
            return "-";
        }
    }

    /* Avoid cast from FinantialDocument to SettlementNote */
    public SettlementNote getSettlementNote() {
        return (SettlementNote) getFinantialDocument();
    }

    public Set<InstallmentSettlementEntry> getSortedInstallmentSettlementEntries() {
        Set<InstallmentSettlementEntry> result = new TreeSet<>((o1, o2) -> InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR
                .compare(o1.getInstallmentEntry(), o2.getInstallmentEntry()));
        return super.getInstallmentSettlementEntriesSet().stream().collect(Collectors.toCollection(() -> result));
    }

    /* SERVICES */

    public static Stream<SettlementEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof SettlementEntry).map(SettlementEntry.class::cast);
    }

    @Atomic
    public static SettlementEntry create(final InvoiceEntry invoiceEntry, final SettlementNote finantialDocument,
            final BigDecimal amount, final String description, final DateTime entryDateTime,
            final boolean createInterestIfNeeded) {
        return new SettlementEntry(invoiceEntry, finantialDocument, amount, description, entryDateTime, createInterestIfNeeded);
    }

    @Atomic
    public static SettlementEntry create(final DebitEntry debitEntry, final BigDecimal debtAmount,
            final SettlementNote settlementNote, DateTime entryDate) {
        boolean createInterestIfNeeded = true;

        // Check if any treasury debt process is preventing from interests being created
        if (TreasuryDebtProcessMainService.isInterestCreationWhenTotalSettledPrevented(debitEntry)) {
            createInterestIfNeeded = false;
        }

        return new SettlementEntry(debitEntry, settlementNote, debtAmount, debitEntry.getDescription(), entryDate,
                createInterestIfNeeded);
    }

    @Atomic
    public static SettlementEntry create(final CreditEntry creditEntry, final BigDecimal creditAmount,
            final String creditDescription, final SettlementNote settlementNote, final DateTime entryDate) {
        return new SettlementEntry(creditEntry, settlementNote, creditAmount, creditDescription, entryDate, false);
    }
}
