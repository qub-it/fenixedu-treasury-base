package org.fenixedu.treasury.finantialdocuments.debitnote;

import org.fenixedu.treasury.base.BasicTreasuryUtils;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.*;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.*;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class DebitNote_anullDebitNoteWithCreditNote_In_Closed_State_With_Credit_Treasury_Exemptions_Test {

    public static final String DEBT_PRODUCT = "PAGAMENTO";

    @BeforeClass
    public static void startUp() {
        BasicTreasuryUtils.startup(() -> {

            AdhocCustomer customer =
                    AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(), Customer.DEFAULT_FISCAL_NUMBER,
                            "Cliente", "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));

            DebtAccount.create(getFinatialInstitution(), customer);

            TreasuryExemptionType.create("TreasuryExemptionType", BasicTreasuryUtils.ls("TreasuryExemptionType"),
                    TreasuryConstants.HUNDRED_PERCENT, true);

            FinantialInstitution institution = FinantialInstitution.findUnique().get();
            institution.setSupportCreditTreasuryExemptions(false);

            Vat.findActiveUnique(VatType.findByCode("INT"), institution, new LocalDate(2021, 1, 1).toDateTimeAtStartOfDay()).get()
                    .setTaxRate(new BigDecimal("0"));

            InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();

            for (int year = 2020; year <= 2024; year++) {
                InterestRateEntry globalInterestRate =
                        InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(year, 1, 1)).get();
                globalInterestRate.setRate(new BigDecimal("4.705"));
                globalInterestRate.setApplyPaymentMonth(false);
            }

            return null;
        });
    }

    @Test
    public void testCreateSimpleCreditNote() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote =
                DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date, date.toLocalDate(), null,
                        Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate, null, product,
                        "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal(500), debitEntry);

        debitNote.closeDocument();

        debitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, debitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                debitEntry.getAvailableNetAmountForCredit());
    }

    @Test
    public void testcase1_twoPositiveDebitEntries() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote debitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        DebitEntry debitEntry1 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("1000"), "debt description 1");
        DebitEntry debitEntry2 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("500"), "debt description 2");

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("400"), debitEntry1);

        debitNote.closeDocument();

        debitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, debitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                debitEntry1.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry1);
        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry2);
    }

    @Test
    public void testcase2_debitEntryWithInterestDebitEntry() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote mainDebitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        LocalDate interestStartDate = new LocalDate(2021, 9, 5);
        DebitEntry mainDebitEntry =
                createDebitEntryWithInterestRate(finantialEntity, date, mainDebitNote, new BigDecimal("1000"), interestStartDate,
                        "debt with interest");

        DocumentNumberSeries interestSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote interestDebitNote = createDebitNote(finantialEntity, date, interestSeries);

        LocalDate interestCalcDate = new LocalDate(2023, 4, 1);
        DebitEntry interestDebitEntry = createInterestDebitEntry(mainDebitEntry, interestCalcDate, interestDebitNote);

        assertTrue("Interest amount should be positive", TreasuryConstants.isPositive(interestDebitEntry.getTotalAmount()));

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("300"), mainDebitEntry);

        mainDebitNote.closeDocument();
        interestDebitNote.closeDocument();

        mainDebitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, mainDebitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                mainDebitEntry.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(mainDebitEntry);
        assertCreditEntriesSumToDebitEntryNetAmount(interestDebitEntry);
    }

    @Test
    public void testcase3_debitEntryWithInterestDebitEntry_annulInterestFirst() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote mainDebitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        LocalDate interestStartDate = new LocalDate(2021, 9, 5);
        DebitEntry mainDebitEntry =
                createDebitEntryWithInterestRate(finantialEntity, date, mainDebitNote, new BigDecimal("1000"), interestStartDate,
                        "debt with interest");

        DocumentNumberSeries interestSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote interestDebitNote = createDebitNote(finantialEntity, date, interestSeries);

        LocalDate interestCalcDate = new LocalDate(2023, 4, 1);
        DebitEntry interestDebitEntry = createInterestDebitEntry(mainDebitEntry, interestCalcDate, interestDebitNote);

        assertTrue("Interest amount should be positive", TreasuryConstants.isPositive(interestDebitEntry.getTotalAmount()));

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("300"), mainDebitEntry);

        mainDebitNote.closeDocument();
        interestDebitNote.closeDocument();

        interestDebitNote.anullDebitNoteWithCreditNote("test", false);

        mainDebitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, mainDebitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                mainDebitEntry.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(mainDebitEntry);
        assertCreditEntriesSumToDebitEntryNetAmount(interestDebitEntry);
    }

    @Test
    public void testcase4_positiveAndZeroDebitEntries() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote debitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        DebitEntry debitEntry1 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("1000"), "positive debit");
        DebitEntry debitEntry2 = createDebitEntry(finantialEntity, dueDate, date, debitNote, BigDecimal.ZERO, "zero debit");

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("500"), debitEntry1);

        debitNote.closeDocument();

        debitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, debitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                debitEntry1.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry1);

        long creditEntryCount = debitEntry2.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).count();
        assertEquals("Debit entry with zero amount should have no credit entries", 0, creditEntryCount);
    }

    @Test
    public void testcase5_oneDebitEntryFullyCreditedBeforeAnnul() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote debitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        DebitEntry debitEntry1 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("1000"), "debit to be fully credited");
        DebitEntry debitEntry2 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("500"), "other debit");

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("300"), debitEntry1);

        debitNote.closeDocument();

        debitEntry1.creditDebitEntry(new BigDecimal("700"), "manual credit", false, Collections.emptyMap());

        debitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, debitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                debitEntry1.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry1);
        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry2);
    }

    @Test
    public void testcase6_oneDebitEntryPartiallyCreditedBeforeAnnul() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

        DebitNote debitNote = createDebitNote(finantialEntity, date, documentNumberSeries);

        DebitEntry debitEntry1 = createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("1000"),
                "debit to be partially credited");
        DebitEntry debitEntry2 =
                createDebitEntry(finantialEntity, dueDate, date, debitNote, new BigDecimal("500"), "other debit");

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();
        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "test", new BigDecimal("200"), debitEntry1);

        debitNote.closeDocument();

        debitEntry1.creditDebitEntry(new BigDecimal("300"), "partial manual credit", false, Collections.emptyMap());

        debitNote.anullDebitNoteWithCreditNote("test", true);

        assertEquals(FinantialDocumentStateType.CLOSED, debitNote.getState());

        assertEquals("Available net exempted amount should be zero after annulment", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
        assertEquals("Available net amount for credit should be zero after annulment", new BigDecimal("0.00"),
                debitEntry1.getAvailableNetAmountForCredit());

        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry1);
        assertEquals("Partially credited debit entry should have two credit entries", 2,
                debitEntry1.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).count());

        assertCreditEntriesSumToDebitEntryNetAmount(debitEntry2);
        assertEquals("Other debit entry should have one credit entry", 1,
                debitEntry2.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).count());
    }

    private static void assertCreditEntriesSumToDebitEntryNetAmount(DebitEntry debitEntry) {
        BigDecimal creditedNetAmount =
                debitEntry.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).map(CreditEntry::getNetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals("Credit entries sum does not match debit entry net amount for " + debitEntry.getDescription(),
                debitEntry.getNetAmount(), creditedNetAmount);
    }

    private static DebitNote createDebitNote(FinantialEntity finantialEntity, DateTime date,
            DocumentNumberSeries documentNumberSeries) {
        return DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date, date.toLocalDate(), null,
                Collections.emptyMap(), null, null);
    }

    private static DebitEntry createDebitEntry(FinantialEntity finantialEntity, LocalDate dueDate, DateTime date,
            DebitNote debitNote, BigDecimal amount, String description) {
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();
        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        return DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, amount, dueDate, null, product, description,
                BigDecimal.ONE, null, date, false, false, debitNote);
    }

    private static DebitEntry createDebitEntryWithInterestRate(FinantialEntity finantialEntity, DateTime date,
            DebitNote debitNote, BigDecimal amount, LocalDate dueDate, String description) {
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();
        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, amount, dueDate, null, product, description,
                        BigDecimal.ONE, null, date, false, false, debitNote);

        InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();

        InterestRate interestRate =
                InterestRate.createForDebitEntry(debitEntry, globalInterestRateType, 1, false, 0, BigDecimal.ZERO, null);
        debitEntry.changeInterestRate(interestRate);

        return debitEntry;
    }

    private static DebitEntry createInterestDebitEntry(DebitEntry originDebitEntry, LocalDate calcDate,
            DebitNote interestDebitNote) {
        return originDebitEntry.createInterestRateDebitEntry(
                originDebitEntry.calculateAllInterestValue(calcDate).iterator().next(), calcDate.toDateTimeAtStartOfDay(),
                interestDebitNote);
    }

    private static DebtAccount getDebtAccount() {
        return FenixFramework.getDomainRoot().getDebtAccountsSet().iterator().next();
    }

    private static FinantialInstitution getFinatialInstitution() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().iterator().next();
    }

}
