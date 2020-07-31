package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class SibsReferenceCode extends SibsReferenceCode_Base {
    
    public SibsReferenceCode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    public SibsReferenceCode(SibsPaymentCodePool sibsPaymentCodePool, String referenceCode, LocalDate validFrom,
            LocalDate validTo, BigDecimal minAmount, BigDecimal maxAmount) {
        this();
        
        setDigitalPaymentPlatform(sibsPaymentCodePool);
        setEntityReferenceCode(sibsPaymentCodePool.castToSibsPaymentCodePoolService().getEntityReferenceCode());
        setReferenceCode(referenceCode);
        setValidFrom(validFrom);
        setValidTo(validTo);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        
        checkRules();
    }

    public void checkRules() {
        
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.domainRoot.required");
        }
        
        if(StringUtils.isEmpty(getReferenceCode())) {
            throw new TreasuryDomainException("error.SibsReferenceCode.referenceCode.required");
        }
        
        if(getValidFrom() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.validFrom.required");
        }
        
        if(getValidTo() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.validTo.required");
        }
        
        if(getMinAmount() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.minAmount.required");
        }
        
        if(getMaxAmount() == null) {
            throw new TreasuryDomainException("error.SibsReferenceCode.maxAmount.required");
        }

        if(findByReferenceCode(getDigitalPaymentPlatform().getEntityReferenceCode(), getReferenceCode()).count() > 1) {
            throw new TreasuryDomainException("error.SibsReferenceCode.referenceCode.duplicate");
        }
    }

    public String getFormattedCode() {
        final StringBuilder result = new StringBuilder();
        int i = 1;
        for (char character : getReferenceCode().toCharArray()) {
            result.append(character);
            if (i % 3 == 0) {
                result.append(" ");
            }
            i++;
        }

        return result.charAt(result.length() - 1) == ' ' ? result.deleteCharAt(result.length() - 1).toString() : result
                .toString();
    }

    public PaymentReferenceCodeStateType getState() {
        if(getSibsPaymentRequest() == null) {
            return PaymentReferenceCodeStateType.UNUSED;
        }
        
        return getSibsPaymentRequest().getState();
    }

    public boolean isInCreatedState() {
        return getSibsPaymentRequest() == null || getSibsPaymentRequest().isInCreatedState();
    }

    public boolean isInRequestedState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInRequestedState();
    }

    public boolean isInAnnuledState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInAnnuledState();
    }

    public boolean isInPaidState() {
        return getSibsPaymentRequest() != null && getSibsPaymentRequest().isInPaidState();
    }

    public Interval getValidInterval() {
        DateTime validFromDateTime = getValidFrom().toDateTimeAtStartOfDay();
        DateTime validToDateTime = getValidTo().plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1);
        return new Interval(validFromDateTime, validToDateTime);
    }
    
    @Override
    public SibsPaymentCodePool getDigitalPaymentPlatform() {
        return (SibsPaymentCodePool) super.getDigitalPaymentPlatform();
    }
    
    public DebtAccount getDebtAccount() {
        if(getSibsPaymentRequest() != null) {
            return getSibsPaymentRequest().getDebtAccount();
        }
        
        return null;
    }
    
    public void delete() {
        super.setDomainRoot(null);
        super.setDigitalPaymentPlatform(null);
        
        super.deleteDomainObject();
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsReferenceCode> findAll() {
        return FenixFramework.getDomainRoot().getSibsReferenceCodesSet().stream();
    }

    public static Stream<SibsReferenceCode> findByReferenceCode(String entityReferenceCode, String referenceCode) {
        return findAll()
                .filter(p -> entityReferenceCode.equals(p.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode()))
                .filter(p -> referenceCode.equals(p.getReferenceCode()));
    }

    public static SibsReferenceCode create(SibsPaymentCodePool sibsPaymentCodePool, String referenceCode, LocalDate validFrom,
            LocalDate validTo, BigDecimal minAmount, BigDecimal maxAmount) {
        return new SibsReferenceCode(sibsPaymentCodePool, referenceCode, validFrom, validTo, minAmount, maxAmount);
    }

}
