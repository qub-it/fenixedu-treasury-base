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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class FinantialDocumentEntry extends FinantialDocumentEntry_Base {

    public abstract BigDecimal getTotalAmount();

    protected FinantialDocumentEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final DebtAccount debtAccount, final FinantialDocument finantialDocument,
            final FinantialEntryType finantialEntryType, final BigDecimal amount, String description, DateTime entryDateTime) {
        setFinantialDocument(finantialDocument);
        setFinantialEntryType(finantialEntryType);
        setAmount(amount);
        setDescription(description);
        setEntryDateTime(entryDateTime);
        super.setCode(String.format("FF-%s-FE-%d", debtAccount.getCustomer().getCode(),
                debtAccount.getCustomer().nextFinantialDocumentEntryNumber()));
    }

    // TODO ANIL 2023-01-30: Replace all references of setFinantialDocument(FinantialDocument) by
    // this method
    public void addToFinantialDocument(FinantialDocument finantialDocument) {
        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException(
                    "error.FinantialDocumentEntry.addToFinantialDocument.already.has.closed.or.annulled.finantialDocument");
        }

        setFinantialDocument(finantialDocument);
    }

    @Override
    // TODO See the todo comment in addToFinantialDocument
    public void setFinantialDocument(FinantialDocument finantialDocument) {
        if (finantialDocument != null && finantialDocument.isClosed()) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.finantialDocument.is.already.closed");
        }

        super.setFinantialDocument(finantialDocument);
    }

    protected void checkRules() {
        if (isFinantialDocumentRequired() && getFinantialDocument() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.finantialDocument.required");
        }

        if (getFinantialEntryType() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.finantialEntryType.required");
        }

        if (getAmount() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.amount.required");
        }

        if (TreasuryConstants.isNegative(getAmount())) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.amount.less.than.zero");
        }

        // TODO: Apply only when code is filled in all entries
//        if (FinantialDocumentEntry.findByCode(getDebtAccount(), getCode()).count() > 1) {
//            throw new TreasuryDomainException("error.FinantialDocumentEntry.code.must.be.unique");
//        }
    }

    public boolean isFinantialDocumentRequired() {
        return true;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            blockers.add(
                    TreasuryConstants.treasuryBundle("error.finantialdocumententry.cannot.be.deleted.document.is.not.preparing"));
        }
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        setDomainRoot(null);
        if (getFinantialDocument() != null) {
            getFinantialDocument().removeFinantialDocumentEntries(this);
        }

        setFinantialDocument(null);
        setFinantialEntryType(null);

        deleteDomainObject();
    }

    protected boolean isNegative(final BigDecimal value) {
        return !isZero(value) && !isPositive(value);
    }

    protected boolean isZero(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) == 0;
    }

    protected boolean isPositive(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    protected boolean isGreaterThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) > 0;
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }

    public void editInternalComments(String internalComments) {
        setInternalComments(internalComments);
    }

    public static Stream<? extends FinantialDocumentEntry> findAll() {
        return FenixFramework.getDomainRoot().getFinantialDocumentEntriesSet().stream();
    }

    public static Optional<? extends FinantialDocumentEntry> findUniqueByEntryOrder(final FinantialDocument finantialDocument,
            int entryOrder) {
        return finantialDocument.getFinantialDocumentEntriesSet().stream()
                .filter(e -> e.getEntryOrder() != null && e.getEntryOrder() == entryOrder).findFirst();
    }

    public boolean isAnnulled() {
        return this.getFinantialDocument() != null && this.getFinantialDocument().isAnnulled();
    }

    public abstract BigDecimal getNetAmount();

    @Override
    public void setCode(String code) {
        super.setCode(code);

        // TODO: Apply only when code is filled in all entries
//        if (FinantialDocument.findByCode(getDebtAccount(), code).count() > 1) {
//            throw new TreasuryDomainException("error.FinantialDocumentEntry.code.must.be.unique");
//        }
    }

    // TODO: Add relation for finantial document entry with debt account
    public DebtAccount getDebtAccount() {
        if (getFinantialDocument() != null) {
            return getFinantialDocument().getDebtAccount();
        }
        return null;
    }

    public static Stream<FinantialDocumentEntry> findByCode(String code) {
        return FenixFramework.getDomainRoot().getFinantialDocumentEntriesSet().stream().filter(entry -> entry.getCode() != null)
                .filter(entry -> entry.getCode().equals(code));
    }

    public static Optional<FinantialDocumentEntry> findUniqueByCode(String code) {
        return findByCode(code).findFirst();
    }

    public static Stream<FinantialDocumentEntry> findByCode(DebtAccount debtAccount, String code) {
        Set<FinantialDocumentEntry> invoiceEntrySet = new HashSet<>();
        Set<FinantialDocumentEntry> entriesOfFinantialDocuments = debtAccount.getFinantialDocumentsSet().stream()
                .flatMap(document -> document.getFinantialDocumentEntriesSet().stream()).collect(Collectors.toSet());

        invoiceEntrySet.addAll(debtAccount.getInvoiceEntrySet());
        invoiceEntrySet.addAll(entriesOfFinantialDocuments);

        return invoiceEntrySet.stream().filter(document -> document.getCode() != null)
                .filter(document -> document.getCode().equals(code));
//        return Stream.empty();
    }

    public static Optional<FinantialDocumentEntry> findUniqueByCode(DebtAccount debtAccount, String code) {
        return findByCode(debtAccount, code).findFirst();
    }
}
