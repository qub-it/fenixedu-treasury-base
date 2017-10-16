package org.fenixedu.treasury.domain.document.reimbursement;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import com.google.common.base.Strings;

public class ReimbursementProcessStatusType extends ReimbursementProcessStatusType_Base {

    public static final Comparator<ReimbursementProcessStatusType> COMPARE_BY_ORDER_NUMBER =
            new Comparator<ReimbursementProcessStatusType>() {

                @Override
                public int compare(final ReimbursementProcessStatusType o1, final ReimbursementProcessStatusType o2) {
                    final int c = Integer.compare(o1.getOrderNumber(), o2.getOrderNumber());

                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }

            };

    public ReimbursementProcessStatusType() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected ReimbursementProcessStatusType(final String code, final String description, final int orderNumber,
            final boolean initialStatus, final boolean finalStatus, final boolean rejectedStatus) {
        this();
        super.setCode(code);
        super.setDescription(description);
        super.setOrderNumber(orderNumber);
        super.setInitialStatus(initialStatus);
        super.setFinalStatus(finalStatus);
        super.setRejectedStatus(rejectedStatus);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.bennu.required");
        }

        if (Strings.isNullOrEmpty(getCode())) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.code.required");
        }

        if (Strings.isNullOrEmpty(getDescription())) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.description.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.code.already.defined", getCode());
        }

        if (findByOrderNumber(getOrderNumber()).count() > 1) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.order.number.unique", getCode());
        }

        if (findByInitialStatus().count() > 1) {
            throw new TreasuryDomainException("error.ReimbursementProcessStatusType.initial.unique");
        }

    }

    public boolean isInitialStatus() {
        return getInitialStatus();
    }

    public boolean isFinalStatus() {
        return getFinalStatus();
    }

    public boolean isRejectedStatus() {
        return getRejectedStatus();
    }

    public boolean isAfter(final ReimbursementProcessStatusType currentReimbursementProcessStatus) {
        return COMPARE_BY_ORDER_NUMBER.compare(this, currentReimbursementProcessStatus) > 0;
    }

    // @formatter:off
    /* --------
     * SERVICES
     * --------
     */
    // @formatter:on

    public static Stream<ReimbursementProcessStatusType> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getReimbursementProcessStatusTypesSet().stream();
    }

    public static Stream<ReimbursementProcessStatusType> findByCode(final String code) {
        return findAll().filter(r -> code.equals(r.getCode()));
    }

    public static Stream<ReimbursementProcessStatusType> findByOrderNumber(final int orderNumber) {
        return findAll().filter(r -> r.getOrderNumber() == orderNumber);
    }

    public static Stream<ReimbursementProcessStatusType> findByInitialStatus() {
        return findAll().filter(r -> r.isInitialStatus());
    }

    public static Optional<ReimbursementProcessStatusType> findUniqueByCode(final String code) {
        return findByCode(code).findFirst();
    }

    public static Optional<ReimbursementProcessStatusType> findUniqueByInitialStatus() {
        return findByInitialStatus().findFirst();
    }

    public static ReimbursementProcessStatusType create(final String code, final String description, final int orderNumber,
            final boolean initialStatus, final boolean finalStatus, final boolean annuledStatus) {
        return new ReimbursementProcessStatusType(code, description, orderNumber, initialStatus, finalStatus, annuledStatus);
    }

}
