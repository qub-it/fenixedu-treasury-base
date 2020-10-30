/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 * 
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.domain.document;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

    protected void init(final FinantialDocument finantialDocument, final FinantialEntryType finantialEntryType,
            final BigDecimal amount, String description, DateTime entryDateTime) {
        setFinantialDocument(finantialDocument);
        setFinantialEntryType(finantialEntryType);
        setAmount(amount);
        setDescription(description);
        setEntryDateTime(entryDateTime);
    }

    @Override
    public void setFinantialDocument(FinantialDocument finantialDocument) {
        if (finantialDocument != null && finantialDocument.isPreparing() == false) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.finantialDocument.is.not.preparing.");
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
    }

    public boolean isFinantialDocumentRequired() {
        return true;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            blockers.add(treasuryBundle("error.finantialdocumententry.cannot.be.deleted.document.is.not.preparing"));
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
}
