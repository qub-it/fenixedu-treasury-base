/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

        super.setDebitNote(null);
        super.setAdvancedPaymentSettlementNote(null);

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
