package org.fenixedu.treasury.domain.debt.balancetransfer;

import static org.fenixedu.treasury.services.integration.erp.sap.SAPExporter.ERP_INTEGRATION_START_DATE;

import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.AdvancedPaymentCreditNote;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

public class BalanceTransferForSAPAndSINGAPWithCompensationForAdvancePaymentCredits
        extends StandardBalanceTransferServiceForSAPAndSINGAP {

    public BalanceTransferForSAPAndSINGAPWithCompensationForAdvancePaymentCredits(DebtAccount fromDebtAccount,
            DebtAccount destinyDebtAccount) {
        super(fromDebtAccount, destinyDebtAccount);
    }

    @Override
    protected void transferCreditEntry(CreditEntry creditEntry) {
        if (isAdvancedPaymentCredit(creditEntry)) {
            BigDecimal creditOpenAmount = creditEntry.getOpenAmount();

            reimburseAdvancePaymentCredit(creditEntry, creditOpenAmount);
            createAdvancePaymentCreditInDestinyDebtAccount(creditEntry, creditOpenAmount);

        } else {
            super.transferCreditEntry(creditEntry);
        }
    }

    private void createAdvancePaymentCreditInDestinyDebtAccount(CreditEntry creditEntry, BigDecimal creditOpenAmount) {
        FinantialInstitution finantialInstitution = this.destinyDebtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = creditEntry.getFinantialEntity();
        Series defaultSeries = Series.findUniqueDefaultSeries(finantialEntity);
        DocumentNumberSeries settlementNoteSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(), defaultSeries);

        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(this.destinyDebtAccount, false, false);
        settlementNoteBean.setDate(new DateTime());
        settlementNoteBean.setDocNumSeries(settlementNoteSeries);
        settlementNoteBean.setAdvancePayment(true);
        settlementNoteBean.setFinantialEntity(finantialEntity);

        settlementNoteBean.getPaymentEntries().add(new PaymentEntryBean(creditOpenAmount,
                finantialInstitution.getErpIntegrationConfiguration().getBalanceCompensationPaymentMethod(), null));

        SettlementNote settlementNote = SettlementNote.createSettlementNote(settlementNoteBean);

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            if (creditEntry.getFinantialDocument().isExportedInLegacyERP()
                    || (creditEntry.getFinantialDocument().getCloseDate() != null
                            && creditEntry.getFinantialDocument().getCloseDate().isBefore(ERP_INTEGRATION_START_DATE))) {

                settlementNote.setExportedInLegacyERP(true);
                settlementNote.setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                settlementNote.getAdvancedPaymentCreditNote().setExportedInLegacyERP(true);
                settlementNote.getAdvancedPaymentCreditNote().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }
        }
    }

    private void reimburseAdvancePaymentCredit(CreditEntry creditEntry, BigDecimal creditOpenAmount) {
        FinantialInstitution finantialInstitution = this.fromDebtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = creditEntry.getFinantialEntity();
        Series defaultSeries = Series.findUniqueDefaultSeries(finantialEntity);
        DocumentNumberSeries reimbursementNoteSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForReimbursementNote(), defaultSeries);
        DateTime entryDateTime = new DateTime();

        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(this.fromDebtAccount, true, true);
        settlementNoteBean.setDate(entryDateTime);
        settlementNoteBean.setDocNumSeries(reimbursementNoteSeries);
        settlementNoteBean.setFinantialEntity(finantialEntity);

        settlementNoteBean.getCreditEntries().stream().filter(ce -> ce.getInvoiceEntry() == creditEntry)
                .forEach(ce -> ce.setIncluded(true));

        settlementNoteBean.getPaymentEntries().add(new PaymentEntryBean(creditOpenAmount,
                finantialInstitution.getErpIntegrationConfiguration().getBalanceCompensationPaymentMethod(), null));

        SettlementNote reimbursementNote = SettlementNote.createSettlementNote(settlementNoteBean);

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            if (creditEntry.getFinantialDocument().isExportedInLegacyERP()
                    || (creditEntry.getFinantialDocument().getCloseDate() != null
                            && creditEntry.getFinantialDocument().getCloseDate().isBefore(ERP_INTEGRATION_START_DATE))) {

                reimbursementNote.setExportedInLegacyERP(true);
                reimbursementNote.setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                reimbursementNote.getSettlemetEntriesSet().stream()
                        .forEach(se -> se.setEntryDateTime(ERP_INTEGRATION_START_DATE.minusSeconds(1)));
            }
        }
    }

    private boolean isAdvancedPaymentCredit(CreditEntry invoiceEntry) {
        return invoiceEntry.getFinantialDocument() != null
                && invoiceEntry.getFinantialDocument() instanceof AdvancedPaymentCreditNote;
    }

    public static LocalizedString getPresentationName() {
        return TreasuryConstants.treasuryBundleI18N(
                "label.BalanceTransferForSAPAndSINGAPWithCompensationForAdvancePaymentCredits.presentationName");
    }

}
