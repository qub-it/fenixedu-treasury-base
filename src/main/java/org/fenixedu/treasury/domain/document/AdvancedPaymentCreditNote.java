package org.fenixedu.treasury.domain.document;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class AdvancedPaymentCreditNote extends AdvancedPaymentCreditNote_Base {

    protected AdvancedPaymentCreditNote(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate) {
        super();

        super.init(debtAccount, documentNumberSeries, documentDate);
        checkRules();
    }

    protected AdvancedPaymentCreditNote(final DebtAccount debtAccount, final DebtAccount payorDebtAccount,
            final DocumentNumberSeries documentNumberSeries, final DateTime documentDate) {
        super();

        super.init(debtAccount, payorDebtAccount, documentNumberSeries, documentDate);
        checkRules();
    }

    @Override
    public boolean isCreditNote() {
        return true;
    }

    @Override
    protected void checkRules() {
        if (!getDocumentNumberSeries().getFinantialDocumentType().getType().equals(FinantialDocumentTypeEnum.CREDIT_NOTE)) {
            throw new TreasuryDomainException("error.AdvancedPaymentCreditNote.finantialDocumentType.invalid");
        }

        if (getAdvancedPaymentSettlementNote() != null
                && !getAdvancedPaymentSettlementNote().getDebtAccount().equals(getDebtAccount())) {
            throw new TreasuryDomainException("error.AdvancedPaymentCreditNote.invalid.debtaccount.with.settlementnote");
        }
        super.checkRules();
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean isAdvancePayment() {
        return true;
    }

    @Override
    public void anullDocument(final String reason) {
        if (this.isClosed()) {

            if (getCreditEntries().anyMatch(ce -> !ce.getSettlementEntriesSet().isEmpty())) {
                throw new TreasuryDomainException("error.CreditNote.cannot.delete.has.settlemententries");
            }

            setState(FinantialDocumentStateType.ANNULED);

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            
            if (!Strings.isNullOrEmpty(loggedUsername)) {
                setAnnulledReason(reason + " - [" + loggedUsername + "]" + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            } else {
                setAnnulledReason(reason + " - " + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            }
        } else {
            throw new TreasuryDomainException(treasuryBundle("error.FinantialDocumentState.invalid.state.change.request"));
        }

        checkRules();
    }

    @Override
    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.CreditNote.cannot.delete");
        }

        setDebitNote(null);
        super.delete(deleteEntries);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    private static AdvancedPaymentCreditNote create(final DebtAccount debtAccount, final DebtAccount payorDebtAccount,
            final DocumentNumberSeries documentNumberSeries, final DateTime documentDate) {
        AdvancedPaymentCreditNote note =
                new AdvancedPaymentCreditNote(debtAccount, payorDebtAccount, documentNumberSeries, documentDate);

        note.checkRules();

        return note;
    }

    @Atomic
    public static AdvancedPaymentCreditNote createCreditNoteForAdvancedPayment(DocumentNumberSeries documentNumberSeries,
            DebtAccount debtAccount, BigDecimal availableAmount, DateTime documentDate, String description, String originDocumentNumber,
            final DebtAccount payorDebtAccount) {
        AdvancedPaymentCreditNote note = create(debtAccount, 
                payorDebtAccount != debtAccount ? payorDebtAccount : null,
                documentNumberSeries,  documentDate);

        note.setOriginDocumentNumber(originDocumentNumber);
        Product advancedPaymentProduct = TreasurySettings.getInstance().getAdvancePaymentProduct();
        if (advancedPaymentProduct == null) {
            throw new TreasuryDomainException("error.AdvancedPaymentCreditNote.invalid.product.for.advanced.payment");
        }
        Vat vat = Vat.findActiveUnique(advancedPaymentProduct.getVatType(), debtAccount.getFinantialInstitution(), new DateTime())
                .orElse(null);
        if (vat == null) {
            throw new TreasuryDomainException("error.AdvancedPaymentCreditNote.invalid.vat.type.for.advanced.payment");
        }
        String lineDescription =
                treasuryBundleI18N("label.AdvancedPaymentCreditNote.advanced.payment.description").getContent(TreasuryConstants.DEFAULT_LANGUAGE)
                        + description;
        CreditEntry entry = CreditEntry.create(note, lineDescription, advancedPaymentProduct, vat, availableAmount, documentDate,
                null, BigDecimal.ONE);

        return note;
    }

}
