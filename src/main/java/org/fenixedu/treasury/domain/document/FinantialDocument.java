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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPExportOperation;
import org.fenixedu.treasury.domain.integration.ERPImportOperation;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.ERPExporterManager;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class FinantialDocument extends FinantialDocument_Base {

    protected static final Comparator<FinantialDocument> COMPARE_BY_DOCUMENT_DATE = new Comparator<FinantialDocument>() {

        @Override
        public int compare(final FinantialDocument o1, final FinantialDocument o2) {
            int c = o1.getDocumentDate().compareTo(o2.getDocumentDate());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected static final Comparator<String> COMPARE_BY_DOCUMENT_NUMBER_STRING = new Comparator<String>() {

        @Override
        public int compare(final String o1, final String o2) {
            return Ordering.<String> natural().compare(o1, o2);
        }
    };

    protected static final Comparator<FinantialDocument> COMPARE_BY_DOCUMENT_NUMBER = new Comparator<FinantialDocument>() {

        @Override
        public int compare(FinantialDocument o1, FinantialDocument o2) {
            int c = Ordering.<String> natural().compare(o1.getDocumentNumber(), o2.getDocumentNumber());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }

    };

    protected FinantialDocument() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setState(FinantialDocumentStateType.PREPARING);
    }

    protected void init(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate) {

        setDebtAccount(debtAccount);
        setFinantialDocumentType(documentNumberSeries.getFinantialDocumentType());
        setDocumentNumberSeries(documentNumberSeries);
        setDocumentNumber("000000000");
        setDocumentDate(documentDate);
        setDocumentDueDate(documentDate.toLocalDate());
        setCurrency(debtAccount.getFinantialInstitution().getCurrency());
        setState(FinantialDocumentStateType.PREPARING);
        setAddress(debtAccount.getCustomer().getAddress());

        super.setCode(String.format("FF-%s-FD-%d", debtAccount.getCustomer().getCode(),
                debtAccount.getCustomer().nextFinantialDocumentNumber()));
        checkRules();
    }



    protected void checkRules() {

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.debtAccount.required");
        }

        if (getFinantialDocumentType() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.finantialDocumentType.required");
        }

        if (getDocumentNumberSeries() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.documentNumber.required");
        }

        if (getDocumentDate() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.documentDate.required");
        }

        if (getDocumentDueDate() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.documentDueDate.required");
        }

        if (getCurrency() == null) {
            throw new TreasuryDomainException("error.FinantialDocument.currency.required");
        }
        if (!getDocumentNumberSeries().getSeries().getFinantialInstitution().equals(getDebtAccount().getFinantialInstitution())) {
            throw new TreasuryDomainException("error.FinantialDocument.finantialinstitution.mismatch");
        }

        if (getDocumentNumberSeries().getSeries().getLegacy() == false) {
            if (getDocumentDueDate().isBefore(getDocumentDate().toLocalDate())) {
                throw new TreasuryDomainException("error.FinantialDocument.documentDueDate.invalid");
            }
        }

        if (isClosed() && isDocumentEmpty()) {
            throw new TreasuryDomainException("error.FinantialDocument.closed.but.empty.entries");
        }

        if (isClosed() && getDocumentNumberSeries().getSeries().getCertificated()) {
            // 2017-02-03: Document order check is taking too much time. The close date will be the certification
            // date for documents
//            final Stream<? extends FinantialDocument> stream =
//                    findClosedUntilDocumentNumberExclusive(getDocumentNumberSeries(), getDocumentNumber());
//
//            final FinantialDocument previousFinantialDocument =
//                    stream.sorted(COMPARE_BY_DOCUMENT_NUMBER).findFirst().orElse(null);
//
//            if (previousFinantialDocument != null && !(previousFinantialDocument.getDocumentDate().toLocalDate()
//                    .compareTo(getDocumentDate().toLocalDate()) <= 0)) {
//                throw new TreasuryDomainException("error.FinantialDocument.documentDate.is.not.after.than.previous.document");
//            }
//
        }

        if (getDocumentDate().isAfterNow()) {
            throw new TreasuryDomainException("error.FinantialDocument.documentDate.cannot.be.after.now");
        }

        if (!Strings.isNullOrEmpty(getOriginDocumentNumber())
                && !TreasuryConstants.isOriginDocumentNumberValid(getOriginDocumentNumber())) {
            throw new TreasuryDomainException("error.FinantialDocument.originDocumentNumber.invalid");
        }

        // Check that all finantial document entries are from the same debt account
        for (final FinantialDocumentEntry entry : getFinantialDocumentEntriesSet()) {
            if (!(entry instanceof InvoiceEntry)) {
                continue;
            }

            if (((InvoiceEntry) entry).getDebtAccount() != getDebtAccount()) {
                throw new TreasuryDomainException("error.FinantialDocument.entries.belongs.different.debt.account");
            }
        }
        if (FinantialDocument.findByCode(getDebtAccount(), getCode()).count() > 1) {
            throw new TreasuryDomainException("error.FinantialDocument.code.must.be.unique");
        }
    }

    protected boolean isDocumentEmpty() {
        return this.getFinantialDocumentEntriesSet().isEmpty();
    }

    public String getUiDocumentNumber() {
        return String.format("%s %s/%s", this.getDocumentNumberSeries().documentNumberSeriesPrefix(),
                this.getDocumentNumberSeries().getSeries().getCode(), Strings.padStart(this.getDocumentNumber(), 7, '0'));
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        for (FinantialDocumentEntry entry : this.getFinantialDocumentEntriesSet()) {
            amount = amount.add(entry.getTotalAmount());
        }

        return getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    public String getUiTotalAmount() {
        return this.getDebtAccount().getFinantialInstitution().getCurrency().getValueFor(this.getTotalAmount());
    }

    public BigDecimal getTotalNetAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        for (FinantialDocumentEntry entry : this.getFinantialDocumentEntriesSet()) {
            amount = amount.add(entry.getNetAmount());
        }

        return getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    public String getUiTotalNetAmount() {
        return this.getDebtAccount().getFinantialInstitution().getCurrency().getValueFor(this.getTotalNetAmount());
    }

    public boolean isClosed() {
        return this.getState().isClosed();
    }

    public boolean isInvoice() {
        return false;
    }

    public boolean isDebitNote() {
        return false;
    }

    public boolean isCreditNote() {
        return false;
    }

    public boolean isSettlementNote() {
        return false;
    }

    public boolean isDeletable() {
        return this.isPreparing() && getPaymentCodesSet().isEmpty();
    }

    public boolean isAnnulled() {
        return this.getState().equals(FinantialDocumentStateType.ANNULED);
    }

    public boolean isPreparing() {
        return this.getState().equals(FinantialDocumentStateType.PREPARING);
    }

    public Map<String, String> getPropertiesMap() {
        return TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public void editPropertiesMap(final Map<String, String> propertiesMap) {
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
    }
    
    @Atomic
    public final void closeDocument() {
        closeDocument(true);
    }
    
    public abstract SortedSet<? extends FinantialDocumentEntry> getFinantialDocumentEntriesOrderedByTuitionInstallmentOrderAndDescription();

    @Atomic
    public void closeDocument(boolean markDocumentToExport) {
        if (this.isPreparing()) {
            this.setDocumentNumber("" + this.getDocumentNumberSeries().getSequenceNumberAndIncrement());
            setState(FinantialDocumentStateType.CLOSED);
            
            final SortedSet<? extends FinantialDocumentEntry> orderedEntries = getFinantialDocumentEntriesOrderedByTuitionInstallmentOrderAndDescription();
            
            int order = 1;
            for (final FinantialDocumentEntry entry : orderedEntries) {
                entry.setEntryOrder(Integer.valueOf(order));
                order = order + 1;
            }
            
            this.setAddress(this.getDebtAccount().getCustomer().getAddress() + this.getDebtAccount().getCustomer().getZipCode());
            if (markDocumentToExport) {
                this.markDocumentToExport();
            }
        } else {
            throw new TreasuryDomainException(
                    TreasuryConstants.treasuryBundle("error.FinantialDocumentState.invalid.state.change.request"));

        }

        /* 
         * 2017-02-03
         * Close date will be the certification date and not document date. Document date is used
         * when document is created.
         * 
         */
        if (getCloseDate() == null) {
            setCloseDate(new DateTime());
        }

        checkRules();
    }

    @Atomic
    public void markDocumentToExport() {

        if (getInstitutionForExportation() == null) {
            this.setInstitutionForExportation(this.getDocumentNumberSeries().getSeries().getFinantialInstitution());
        }

        if (getDebtAccount().getFinantialInstitution().getErpIntegrationConfiguration().getActive()) {
            ERPExporterManager.scheduleSingleDocument(this);
        }
    }

    @Atomic
    public void clearDocumentToExport(final String reason) {
        if (getInstitutionForExportation() != null) {
            this.setInstitutionForExportation(null);
            
            String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            final String username = StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "unknown";
            final DateTime now = new DateTime();

            super.setClearDocumentToExportReason(String.format("%s - [%s] %s", reason, username, now.toString("YYYY-MM-dd HH:mm:ss")));
            super.setClearDocumentToExportDate(now);
            
        }
    }

    @Atomic
    public void clearDocumentToExportAndSaveERPCertificationData(final String reason, final LocalDate erpCertificationDate,
            final String erpCertificateDocumentReference) {
        if (getInstitutionForExportation() != null) {
            this.setInstitutionForExportation(null);

            String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            final String username = StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "unknown";
            final DateTime now = new DateTime();

            super.setClearDocumentToExportReason(String.format("%s - [%s] %s", reason, username, now.toString("YYYY-MM-dd HH:mm:ss")));
            super.setClearDocumentToExportDate(now);

            this.editERPCertificationData(erpCertificationDate, erpCertificateDocumentReference);
        }
    }

    private void editERPCertificationData(final LocalDate erpCertificationDate, final String erpCertificateDocumentReference) {
        if (erpCertificationDate == null) {
            throw new TreasuryDomainException("error.FinantialDocument.erpCertificationDate.required");
        }

        if (Strings.isNullOrEmpty(erpCertificateDocumentReference)) {
            throw new TreasuryDomainException("error.FinantialDocument.erpCertificateDocumentReference.required");
        }

        this.setErpCertificationDate(erpCertificationDate);
        this.setErpCertificateDocumentReference(erpCertificateDocumentReference);
    }

    public boolean isDocumentToExport() {
        return getInstitutionForExportation() != null;
    }

    public boolean isExportedInLegacyERP() {
        return getExportedInLegacyERP();
    }

    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FinantialDocument.cannot.delete");
        }

        setDomainRoot(null);
        setDocumentNumberSeries(null);
        setCurrency(null);
        setDebtAccount(null);
        setFinantialDocumentType(null);
        setInstitutionForExportation(null);
        for (FinantialDocumentEntry entry : getFinantialDocumentEntriesSet()) {
            this.removeFinantialDocumentEntries(entry);
            if (deleteEntries) {
                entry.delete();
            } else {
                entry.setFinantialDocument(null);
            }
        }

        for (ERPExportOperation oper : getErpExportOperationsSet()) {
            this.removeErpExportOperations(oper);
            oper.delete();
        }

        for (ERPImportOperation oper : getErpImportOperationsSet()) {
            this.removeErpImportOperations(oper);
            oper.delete();
        }
        deleteDomainObject();
    }

    public abstract Set<FinantialDocument> findRelatedDocuments(Set<FinantialDocument> documentsBaseList,
            Boolean includeAnulledDocuments);

    public Boolean getClosed() {
        return this.getState().equals(FinantialDocumentStateType.CLOSED);
    }

    public BigDecimal getOpenAmount() {
        if (this.getState().isPreparing() || this.getState().isClosed()) {
            return getTotalAmount();
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getOpenAmountWithInterests() {
        if (this.getState().isPreparing() || this.getState().isClosed()) {
            return getTotalAmount();
        } else {
            return BigDecimal.ZERO;
        }
    }

    public boolean isDocumentSeriesNumberSet() {
        return Long.parseLong(getDocumentNumber()) != 0;
    }

    public boolean isCertifiedPrintedDocumentAvailable() {
        if (!isClosed()) {
            return false;
        }

        if (isExportedInLegacyERP()) {
            return false;
        }

        if (isDocumentToExport()) {
            return false;
        }

        if (isSettlementNote() && ((SettlementNote) this).isReimbursement()) {
            return false;
        }

        if (getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            return false;
        }

        if (isCreditNote() && ((CreditNote) this).isAdvancePayment()) {
            return false;
        }

        return true;
    }

    public Optional<ERPExportOperation> getLastERPExportOperation() {
        if (getErpExportOperationsSet().isEmpty()) {
            return Optional.empty();
        }

        return getErpExportOperationsSet().stream().sorted(ERPExportOperation.COMPARE_BY_VERSIONING_CREATION_DATE.reversed())
                .findFirst();
    }

    public String getUiLastERPExportationErrorMessage() {
        try {
            Optional<ERPExportOperation> lastERPExportOperation = getLastERPExportOperation();

            if (!lastERPExportOperation.isPresent()) {
                return "";
            }

            if (lastERPExportOperation.get().getSuccess()) {
                return "";
            }

            final String[] lines = lastERPExportOperation.get().getErrorLog()
                    .replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z", "").split("\n");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                sb.append(lines[i]).append("<br />");
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends FinantialDocument> findAll() {
        return FenixFramework.getDomainRoot().getFinantialDocumentsSet().stream();
    }

    public static Stream<? extends FinantialDocument> find(final FinantialDocumentType finantialDocumentType) {
        return finantialDocumentType.getFinantialDocumentsSet().stream();
    }

    public static Stream<? extends FinantialDocument> find(final DocumentNumberSeries documentNumberSeries) {
        return documentNumberSeries.getFinantialDocumentsSet().stream();
    }

    public static Optional<? extends FinantialDocument> findUniqueByDocumentNumber(final String documentNumber) {
        return findAll().filter(x -> documentNumber.equals(x.getUiDocumentNumber())).findFirst();
    }

    protected static Stream<? extends FinantialDocument> findClosedUntilDocumentNumberExclusive(
            final DocumentNumberSeries documentNumberSeries, final String documentNumber) {
        return find(documentNumberSeries).filter(
                d -> d.isClosed() && COMPARE_BY_DOCUMENT_NUMBER_STRING.compare(d.getDocumentNumber(), documentNumber) < 0);
    }

    public static FinantialDocument findByUiDocumentNumber(final FinantialInstitution finantialInstitution,
            final String docNumber) {

        if (finantialInstitution == null || Strings.isNullOrEmpty(docNumber)) {
            return null;
        }

        //parse the Document Number in {DOCUMENT_TYPE} {DOCUMENT_SERIES}/{DOCUMENT_NUMBER}
        String documentType;
        String seriesNumber;

        try {
            List<String> values = Splitter.on(' ').splitToList(docNumber);
            List<String> values2 = Splitter.on('/').splitToList(values.get(1));

            documentType = values.get(0);
            seriesNumber = values2.get(0);
//            documentNumber = values2.get(1);

            FinantialDocumentType type = FinantialDocumentType.findByCode(documentType);

            if (type != null) {
                Series series = Series.findByCode(finantialInstitution, seriesNumber);
                if (series != null) {
                    DocumentNumberSeries dns = DocumentNumberSeries.find(type, series);

                    if (dns != null) {
                        return dns.getFinantialDocumentsSet().stream().filter(x -> x.getUiDocumentNumber().equals(docNumber))
                                .findFirst().orElse(null);
                    }
                }
            } else {
                Set<FinantialDocument> docs = FinantialDocument.findAll()
                        .filter(d -> d.getDebtAccount().getFinantialInstitution() == finantialInstitution)
                        .filter(d -> d.getUiDocumentNumber().equals(docNumber)).collect(Collectors.<FinantialDocument> toSet());

                if (docs.isEmpty()) {
                    return null;
                }

                if (docs.size() > 1) {
                    throw new TreasuryDomainException("error.FinantialDocument.findByUiDocumentNumber.found.more.than.one");
                }

                final FinantialDocument finantialDocument = docs.iterator().next();
                if (!finantialDocument.getDocumentNumberSeries().isReplacePrefix()) {
                    throw new TreasuryDomainException("error.FinantialDocument.findByUiDocumentNumber.not.from.replacing.prefix");
                }

                if (!documentType.equals(finantialDocument.getDocumentNumberSeries().getReplacingPrefix())) {
                    throw new TreasuryDomainException("error.FinantialDocument.findByUiDocumentNumber.documentType.not.equal");
                }

                return finantialDocument;
            }
        } catch (Exception ex) {
        }

        return null;
    }

    @Override
    public void setCode(String code) {
        super.setCode(code);
        if (FinantialDocument.findByCode(getDebtAccount(), code).count() > 1) {
            throw new TreasuryDomainException("error.FinantialDocument.code.must.be.unique");
        }
    }

    public static Stream<FinantialDocument> findByCode(String code) {
        return FenixFramework.getDomainRoot().getFinantialDocumentsSet().stream()
                .filter(document -> document.getCode() != null && document.getCode().equals(code));
    }

    public static Optional<FinantialDocument> findUniqueByCode(String code) {
        return findByCode(code).findFirst();
    }


    public static Stream<FinantialDocument> findByCode(DebtAccount debtAccount, String code) {
        return debtAccount.getFinantialDocumentsSet().stream().filter(document -> document.getCode().equals(code));
    }

    public static Optional<FinantialDocument> findUniqueByCode(DebtAccount debtAccount, String code) {
        return findByCode(debtAccount, code).findFirst();
    }
}
