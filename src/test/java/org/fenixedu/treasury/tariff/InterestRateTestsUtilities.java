package org.fenixedu.treasury.tariff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.BasicTreasuryUtils;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class InterestRateTestsUtilities {
    public static final String DEBT_PRODUCT = "PAGAMENTO";

    public static void startUp() {

        BasicTreasuryUtils.startup(() -> {
            createDebtAccount();

            Vat.findActiveUnique(VatType.findByCode("INT"), FinantialInstitution.findUnique().get(),
                    new LocalDate(2021, 1, 1).toDateTimeAtStartOfDay()).get().setTaxRate(new BigDecimal("0"));

            InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();

            // Taxas de juro oficiais desde 2020 incluindo as medidas sobre COVID-19
            InterestRateEntry globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2020, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.786"));
            globalInterestRate.setApplyPaymentMonth(true);

            InterestRateEntry.create(globalInterestRateType, new LocalDate(2020, 3, 12), ls("Isencao de Juro DL1-A/2020"),
                    BigDecimal.ZERO, true, false);

            InterestRateEntry.create(globalInterestRateType, new LocalDate(2020, 7, 1),
                    ls("Juro oficial para o ano 2020 (retoma)"), new BigDecimal("4.786"), true, false);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2021, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(true);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2022, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.510"));
            globalInterestRate.setApplyPaymentMonth(true);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2023, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("5.997"));
            globalInterestRate.setApplyPaymentMonth(true);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2024, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("5.997"));
            globalInterestRate.setApplyPaymentMonth(true);

            return null;
        });
    }

    public static DebitEntry createDebitEntry(BigDecimal debitAmount, LocalDate dueDate, boolean applyInterest) {
        Vat vat =
                Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), dueDate.toDateTimeAtStartOfDay()).get();
        TreasuryEvent treasuryEvent = null;
        DebitEntry debitEntry = DebitEntry.create(Optional.empty(), getDebtAccount(), treasuryEvent, vat, debitAmount, dueDate,
                null, Product.findUniqueByCode(DEBT_PRODUCT).get(), "debt " + debitAmount, BigDecimal.ONE, null,
                dueDate.toDateTimeAtStartOfDay());
        if (applyInterest) {
            final int numberOfDaysAfterDueDate = 1;
            final boolean applyInFirstWorkday = false;
            final int maximumDaysToApplyPenalty = 0;
            final BigDecimal rate = null;

            InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();
            
            InterestRate interestRate = InterestRate.createForDebitEntry(debitEntry, globalInterestRateType,
                    numberOfDaysAfterDueDate, applyInFirstWorkday, maximumDaysToApplyPenalty, BigDecimal.ZERO, rate);
            debitEntry.changeInterestRate(interestRate);
        }
        return debitEntry;
    }

    public static DebtAccount getDebtAccount() {
        return FenixFramework.getDomainRoot().getDebtAccountsSet().iterator().next();
    }

    public static void createDebtAccount() {
        AdhocCustomer create = AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(),
                Customer.DEFAULT_FISCAL_NUMBER, "Diogo", "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));
        DebtAccount.create(getFinatialInstitution(), create);
    }

    public static FinantialInstitution getFinatialInstitution() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}
