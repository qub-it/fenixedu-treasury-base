package org.fenixedu.treasury.finantialdocuments.debitnote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fenixedu.treasury.base.BasicTreasuryUtils;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentStateType;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DebitNote_anullAndCopyDebitNote_Test {

    public static final String DEBT_PRODUCT = "PAGAMENTO";

    private static DebtAccount DEBT_ACCOUNT;

    @BeforeClass
    public static void startUp() {
        BasicTreasuryUtils.startup(() -> {

            AdhocCustomer customer =
                    AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(), Customer.DEFAULT_FISCAL_NUMBER,
                            "Cliente", "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));

            DEBT_ACCOUNT = DebtAccount.create(getFinatialInstitution(), customer);

            TreasuryExemptionType.create("TreasuryExemptionType", BasicTreasuryUtils.ls("TreasuryExemptionType"),
                    TreasuryConstants.HUNDRED_PERCENT, true);

            FinantialInstitution institution = FinantialInstitution.findUnique().get();
            institution.setSupportCreditTreasuryExemptions(false);

            Vat.findActiveUnique(VatType.findByCode("INT"), institution, new LocalDate(2021, 1, 1).toDateTimeAtStartOfDay()).get()
                    .setTaxRate(new BigDecimal("0"));

            return null;
        });
    }

    // ###################################################################################
    // Tests on a closed debit note (the copy is created)
    // ###################################################################################

    @Test
    public void givenClosedDebitNote_whenUpdatePayorDebtAccount_thenReturnsNewPreparingDebitNote() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description");
        debitNote.closeDocument();

        DebtAccount payorDebtAccount = createPayorDebtAccount();
        DebitNote copiedDebitNote = debitNote.updatePayorDebtAccount(payorDebtAccount);

        assertNotSame("A closed debit note should be copied instead of updated", debitNote, copiedDebitNote);
        assertEquals("The copied debit note should be in preparing state", FinantialDocumentStateType.PREPARING,
                copiedDebitNote.getState());
        assertSame("The new payor debt account should be set on the copied debit note", payorDebtAccount,
                copiedDebitNote.getPayorDebtAccount());
        assertSame("The copied debit note should belong to the same debt account", debitNote.getDebtAccount(),
                copiedDebitNote.getDebtAccount());
    }

    @Test
    public void givenClosedDebitNote_whenUpdatePayorDebtAccount_thenCopiedDebitNoteKeepsOriginDocumentNumberAndSeries() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description");
        debitNote.edit(debitNote.getDocumentDate().toLocalDate(), debitNote.getDocumentDueDate(), "ORIGIN-123", null, null, null);
        debitNote.closeDocument();

        DebitNote copiedDebitNote = debitNote.updatePayorDebtAccount(createPayorDebtAccount());

        assertEquals("The origin document number should be copied", "ORIGIN-123", copiedDebitNote.getOriginDocumentNumber());
        assertSame("The document number series should be kept", debitNote.getDocumentNumberSeries(),
                copiedDebitNote.getDocumentNumberSeries());
    }

    @Test
    public void givenClosedDebitNoteWithDebitEntries_whenUpdatePayorDebtAccount_thenDebitEntriesAreCopiedToNewDebitNote() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description 1", "debt description 2");
        debitNote.closeDocument();

        Set<? extends DebitEntry> originalEntries = debitNote.getDebitEntriesSet();
        assertEquals("The closed debit note should have two debit entries", 2, originalEntries.size());

        DebitNote copiedDebitNote = debitNote.updatePayorDebtAccount(createPayorDebtAccount());

        Set<? extends DebitEntry> copiedEntries = copiedDebitNote.getDebitEntriesSet();
        assertEquals("The copied debit note should have a copy of each debit entry", originalEntries.size(), copiedEntries.size());

        for (DebitEntry originalEntry : originalEntries) {
            DebitEntry copiedEntry = copiedEntries.stream()
                    .filter(e -> e.getDescription().equals(originalEntry.getDescription())).findFirst().orElse(null);

            assertNotNull("Debit entry " + originalEntry.getDescription() + " was not copied", copiedEntry);
            assertNotSame("The debit entry should be copied and not shared", originalEntry, copiedEntry);
            assertSame("The copied debit entry should be associated with the new debit note", copiedDebitNote,
                    copiedEntry.getFinantialDocument());
            assertAmountEquals("The amount should be copied", originalEntry.getAmount(), copiedEntry.getAmount());
            assertAmountEquals("The net amount should be copied", originalEntry.getNetAmount(), copiedEntry.getNetAmount());
            assertEquals("The due date should be copied", originalEntry.getDueDate(), copiedEntry.getDueDate());
            assertEquals("The vat should be copied", originalEntry.getVat(), copiedEntry.getVat());
            assertEquals("The product should be copied", originalEntry.getProduct(), copiedEntry.getProduct());
            assertEquals("The quantity should be copied", originalEntry.getQuantity(), copiedEntry.getQuantity());
        }
    }

    @Test
    public void givenClosedDebitNoteWithDebitEntries_whenUpdatePayorDebtAccount_thenOriginalDebitNoteIsAnnulledWithCreditNote() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description 1", "debt description 2");
        debitNote.closeDocument();

        Set<? extends DebitEntry> originalEntries = debitNote.getDebitEntriesSet();

        debitNote.updatePayorDebtAccount(createPayorDebtAccount());

        assertNotNull("The original debit note should be marked as annulled", debitNote.getAnnulledReason());
        assertNotNull("The annulment date should be set on the original debit note", debitNote.getAnnullmentDate());

        for (DebitEntry originalEntry : originalEntries) {
            assertCreditEntriesSumToDebitEntryNetAmount(originalEntry);
        }
    }

    // ###################################################################################
    // Tests on a preparing debit note (the same debit note is updated)
    // ###################################################################################

    @Test
    public void givenPreparingDebitNote_whenUpdatePayorDebtAccount_thenSameDebitNoteIsUpdated() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description");
        long debitNotesBefore = DebitNote.find(getDebtAccount()).count();

        DebtAccount payorDebtAccount = createPayorDebtAccount();
        DebitNote updatedDebitNote = debitNote.updatePayorDebtAccount(payorDebtAccount);

        assertSame("A preparing debit note should be updated instead of copied", debitNote, updatedDebitNote);
        assertEquals("The updated debit note should remain in preparing state", FinantialDocumentStateType.PREPARING,
                updatedDebitNote.getState());
        assertSame("The new payor debt account should be set on the updated debit note", payorDebtAccount,
                updatedDebitNote.getPayorDebtAccount());
        assertEquals("No extra debit note should be created", debitNotesBefore,
                DebitNote.find(debitNote.getDebtAccount()).count());
        assertEquals("The debit entries should be kept in the updated debit note", 1, updatedDebitNote.getDebitEntriesSet().size());
    }

    // ###################################################################################
    // Tests on the validation of the copy operation
    // ###################################################################################

    @Test
    public void givenAnnulledDebitNote_whenUpdatePayorDebtAccount_thenThrowsTreasuryDomainException() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebitNote debitNote = createDebitNoteWithEntries(finantialEntity, "debt description");
        debitNote.anullDebitNoteWithCreditNote("test", false);
        assertEquals(FinantialDocumentStateType.ANNULED, debitNote.getState());

        DebtAccount payorDebtAccount = createPayorDebtAccount();
        TreasuryDomainException exception =
                assertThrows(TreasuryDomainException.class, () -> debitNote.updatePayorDebtAccount(payorDebtAccount));

        assertEquals("error.DebitNote.updatePayorDebtAccount.not.preparing.nor.closed", exception.getKey());
    }

    @Test
    public void givenDebitNoteWithSamePayor_whenUpdatePayorDebtAccount_thenThrowsTreasuryDomainException() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DebtAccount payorDebtAccount = createPayorDebtAccount();

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), payorDebtAccount, documentNumberSeries,
                new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(2021, 9, 30), null, Collections.emptyMap(),
                null, null);

        TreasuryDomainException exception =
                assertThrows(TreasuryDomainException.class, () -> debitNote.updatePayorDebtAccount(payorDebtAccount));

        assertEquals("error.DebitNote.updatePayorDebtAccount.payor.not.changed", exception.getKey());
    }

    // ###################################################################################
    // Auxiliary methods
    // ###################################################################################

    private static DebitNote createDebitNoteWithEntries(FinantialEntity finantialEntity, String... descriptions) {
        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        for (int i = 0; i < descriptions.length; i++) {
            createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal(1000).add(new BigDecimal(500).multiply(
                    new BigDecimal(i))), descriptions[i]);
        }

        return debitNote;
    }

    private static DebitEntry createDebitEntry(FinantialEntity finantialEntity, LocalDate dueDate, DateTime date,
            DebitNote debitNote, BigDecimal amount, String description) {
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();
        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        return DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, amount, dueDate, null, product, description,
                BigDecimal.ONE, null, date, false, false, debitNote);
    }

    private static void assertCreditEntriesSumToDebitEntryNetAmount(DebitEntry debitEntry) {
        BigDecimal creditedNetAmount =
                debitEntry.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).map(CreditEntry::getNetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAmountEquals("Credit entries sum does not match debit entry net amount for " + debitEntry.getDescription(),
                debitEntry.getNetAmount(), creditedNetAmount);
    }

    private static void assertAmountEquals(String message, BigDecimal expected, BigDecimal actual) {
        assertTrue(message + " (expected " + expected + " but was " + actual + ")", expected.compareTo(actual) == 0);
    }

    private static DebtAccount createPayorDebtAccount() {
        AdhocCustomer payorCustomer =
                AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(), Customer.DEFAULT_FISCAL_NUMBER, "Payor",
                        "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));
        return DebtAccount.create(getFinatialInstitution(), payorCustomer);
    }

    private static DebtAccount getDebtAccount() {
        return DEBT_ACCOUNT;
    }

    private static FinantialInstitution getFinatialInstitution() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().iterator().next();
    }

}
