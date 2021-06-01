package org.fenixedu.treasury.domain.paymentpenalty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltyTaxSettings extends PaymentPenaltyTaxSettings_Base {

    public PaymentPenaltyTaxSettings() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setActive(false);
        setCreatePaymentCode(false);
        setApplyPenaltyOnDebitsWithoutInterest(false);
    }
    
    public PaymentPenaltyTaxSettings(FinantialEntity finantialEntity, Product penaltyProduct) {
        this();
        
        this.setFinantialEntity(finantialEntity);
        this.setPenaltyProduct(penaltyProduct);
        
        checkRules();
    }

    private void checkRules() {
        if (super.getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.domainRoot.required");
        }

        if(getFinantialEntity() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.finantialEntity.required");
        }
        
        if (super.getActive() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.active.required");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getEmolumentDescription() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.is.active.but.emolumentDescription.is.null");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getPenaltyProduct() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.is.active.but.penaltyProduct.is.null");
        }

        if (super.getCreatePaymentCode() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.createPaymentCode.required");
        }

        if (super.getApplyPenaltyOnDebitsWithoutInterest() == null) {
            throw new IllegalStateException("error.PaymentPenaltyTaxSettings.applyPenaltyOnDebitsWithoutInterest.required");
        }

    }

    public LocalizedString buildEmolumentDescription(DebitEntry originDebitEntry) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("debitEntryDescription", originDebitEntry.getDescription());
            valueMap.put("penaltyProductName", getPenaltyProduct().getName().getContent(locale));

            result = result.with(locale, StrSubstitutor.replace(getEmolumentDescription().getContent(locale), valueMap));
        }

        return result;
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setFinantialEntity(null);
        setPenaltyProduct(null);

        super.deleteDomainObject();
    }

    public void edit(boolean active, Product penaltyProduct, LocalizedString emolumentDescription, boolean createPaymentCode,
            boolean applyPenaltyOnDebitsWithoutInterest) {
        super.setActive(active);

        super.setPenaltyProduct(penaltyProduct);
        super.setEmolumentDescription(emolumentDescription);
        super.setCreatePaymentCode(createPaymentCode);
        super.setApplyPenaltyOnDebitsWithoutInterest(applyPenaltyOnDebitsWithoutInterest);

        checkRules();
    }

    /*
     * SERVICES
     */

    public static PaymentPenaltyTaxSettings create(FinantialEntity finantialEntity, Product penaltyProduct) {
        return new PaymentPenaltyTaxSettings(finantialEntity, penaltyProduct);
    }

    public static Stream<PaymentPenaltyTaxSettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltyTaxSettingsSet().stream();
    }

    public static Stream<PaymentPenaltyTaxSettings> findActive() {
        return findAll().filter(s -> Boolean.TRUE.equals(s.getActive()));
    }
    
    public static Stream<PaymentPenaltyTaxSettings> findActiveForOriginDebitEntry(DebitEntry originDebitEntry) {
        return findActive().filter(s -> s.getTargetProductsSet().contains(originDebitEntry.getProduct()));
    }

}
