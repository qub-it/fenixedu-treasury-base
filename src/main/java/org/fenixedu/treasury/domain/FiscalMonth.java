package org.fenixedu.treasury.domain;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class FiscalMonth extends FiscalMonth_Base {

    public FiscalMonth() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public FiscalMonth(FiscalYear fiscalYear, int month) {
        this();

        super.setFiscalYear(fiscalYear);
        super.setMonth(month);

        Boolean fiscalOperationsClosed = isFiscalExerciseExpired(new LocalDate());
        super.setFiscalOperationsClosed(fiscalOperationsClosed);

        if (fiscalOperationsClosed) {
            String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            super.setFiscalOperationsClosedResponsible(loggedUsername);
            super.setFiscalOperationsClosedSetDate(new DateTime());
        }

        checkRules();
    }

    public boolean isFiscalExerciseExpired() {
        return isFiscalExerciseExpired(new LocalDate());
    }

    public boolean isFiscalExerciseExpired(LocalDate localDate) {
        LocalDate endDayOfMonthDate = new LocalDate(getFiscalYear().getYear(), getMonth(), 1).plusMonths(1).minusDays(1);

        return endDayOfMonthDate.isBefore(localDate);
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.FiscalMonth.domainRoot.required");
        }

        if (getFiscalYear() == null) {
            throw new TreasuryDomainException("error.FiscalMonth.fiscalYear.required");
        }

        if (getMonth() < 1 || getMonth() > 12) {
            throw new TreasuryDomainException("error.FiscalMonth.month.invalid");
        }

        if (find(getFiscalYear(), getMonth()).count() > 1) {
            throw new TreasuryDomainException("error.FiscalMonth.year.month.duplicated");
        }
    }

    public void closeOperations() {
        super.setFiscalOperationsClosed(true);

        String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
        super.setFiscalOperationsClosedResponsible(loggedUsername);
        super.setFiscalOperationsClosedSetDate(new DateTime());
    }

    /* Services */

    public static Stream<FiscalMonth> findAll() {
        return FenixFramework.getDomainRoot().getFiscalMonthsSet().stream();
    }

    public static Stream<FiscalMonth> find(FiscalYear fiscalYear) {
        return findAll().filter(f -> f.getFiscalYear() == fiscalYear);
    }

    public static Stream<FiscalMonth> find(FiscalYear fiscalYear, int month) {
        return find(fiscalYear).filter(f -> f.getMonth() == month);
    }

    public static Optional<FiscalMonth> findUnique(FiscalYear fiscalYear, int month) {
        return find(fiscalYear, month).findFirst();
    }

    public static boolean isFiscalOperationsOpenNow(FinantialInstitution finantialInstitution) {
        LocalDate now = new LocalDate();

        return isFiscalOperationsOpen(finantialInstitution, now.getYear(), now.getMonthOfYear());
    }

    public static boolean isFiscalOperationsOpen(FinantialInstitution finantialInstitution, int year, int month) {
        if (!FiscalYear.findUnique(finantialInstitution, year).isPresent()) {
            return false;
        }

        FiscalYear fiscalYear = FiscalYear.findUnique(finantialInstitution, year).get();

        if (!FiscalMonth.findUnique(fiscalYear, month).isPresent()) {
            return false;
        }

        return !Boolean.TRUE.equals(FiscalMonth.findUnique(fiscalYear, month).get().getFiscalOperationsClosed());
    }

    public static boolean isFiscalOperationsClosed(FinantialInstitution finantialInstitution, int year, int month) {
        if (!FiscalYear.findUnique(finantialInstitution, year).isPresent()) {
            return false;
        }

        FiscalYear fiscalYear = FiscalYear.findUnique(finantialInstitution, year).get();

        if (!FiscalMonth.findUnique(fiscalYear, month).isPresent()) {
            return false;
        }

        return Boolean.TRUE.equals(FiscalMonth.findUnique(fiscalYear, month).get().getFiscalOperationsClosed());
    }

    public static FiscalMonth create(FiscalYear fiscalYear, int month) {
        return new FiscalMonth(fiscalYear, month);
    }

    public static FiscalMonth getOrCreateFiscalMonth(FiscalYear fiscalYear, int month) {
        return findUnique(fiscalYear, month).orElseGet(() -> create(fiscalYear, month));
    }
}
