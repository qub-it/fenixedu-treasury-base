package org.fenixedu.treasury.domain.tariff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public abstract class InterestRateType extends InterestRateType_Base {

    public static Comparator<InterestRateType> COMPARE_BY_NAME =
            (o1, o2) -> o1.getDescription().getContent().compareTo(o2.getDescription().getContent());

    public InterestRateType() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setRequiresInterestFixedAmount(false);
    }

    public void init(String code, LocalizedString description) {
        setCode(code);
        setDescription(description);
    }

    protected void checkRules() {
        if (getDomainRoot() == null) {
            throw new RuntimeException("error.InterestRateType.domainRoot.required");
        }

        if (getCode() == null) {
            throw new RuntimeException("error.InterestRateType.code.required");
        }

        if (getDescription() == null) {
            throw new RuntimeException("error.InterestRateType.description.required");
        }

    }

    public abstract InterestRateBean calculateInterests(DebitEntry debitEntry, LocalDate paymentDate,
            boolean withAllInterestValues);

    public abstract InterestRateBean calculateAllInterestsByLockingAtDate(DebitEntry debitEntry, LocalDate lockDate);

    public boolean isInterestRateTypeAvailable() {
        return TreasurySettings.getInstance().getAvailableInterestRateTypesSet().contains(this);
    }
    
    public void makeAvailable() {
        TreasurySettings.getInstance().getAvailableInterestRateTypesSet().add(this);
    }
    
    public void makeUnavailable() {
        TreasurySettings.getInstance().getAvailableInterestRateTypesSet().remove(this);
    }
    
    public void delete() {
        super.setDomainRoot(null);
        
        getInterestRateEntriesSet().forEach(e -> e.delete());
        
        super.deleteDomainObject();
    }

    protected TreeMap<LocalDate, BigDecimal> createdInterestEntriesMap(DebitEntry debitEntry) {
        TreeMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();

        for (DebitEntry interestDebitEntry : debitEntry.getInterestDebitEntriesSet()) {
            if (interestDebitEntry.isAnnulled()) {
                continue;
            }

            if (!TreasuryConstants.isPositive(interestDebitEntry.getAvailableAmountForCredit())) {
                continue;
            }

            LocalDate interestEntryDateTime = interestDebitEntry.getEntryDateTime().toLocalDate();
            result.putIfAbsent(interestEntryDateTime, BigDecimal.ZERO);
            result.put(interestEntryDateTime,
                    result.get(interestEntryDateTime).add(interestDebitEntry.getAvailableAmountForCredit()));
        }

        return result;
    }

    //
    // SERVICES
    //

    public static LocalizedString getPresentationName(Class<? extends InterestRateType> interestRateTypeClass) {
        try {
            final Method method = interestRateTypeClass.getMethod("getPresentationName", new Class[] {});
            return (LocalizedString) method.invoke(null, new Object[] {});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<? extends InterestRateType> findAll() {
        return FenixFramework.getDomainRoot().getInterestRateTypesSet().stream();
    }

    public static Stream<? extends InterestRateType> findByCode(String code) {
        return findAll().filter(t -> t.getCode().equals(code));
    }

    public static Optional<? extends InterestRateType> findUniqueByCode(String code) {
        return findByCode(code).findFirst();
    }

    public static Optional<GlobalInterestRateType> findAvailableGlobalInterestRateType() {
        return TreasurySettings.getInstance().getAvailableInterestRateTypesSet().stream()
                .filter(type -> type instanceof GlobalInterestRateType).map(GlobalInterestRateType.class::cast).findFirst();
    }

    public static <T extends InterestRateType> T create(Class<T> clazz, String code, LocalizedString description) {
        try {
            return clazz.getConstructor(String.class, LocalizedString.class).newInstance(code, description);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
