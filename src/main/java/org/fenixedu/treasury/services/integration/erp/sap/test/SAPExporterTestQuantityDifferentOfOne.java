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
package org.fenixedu.treasury.services.integration.erp.sap.test;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.generated.sources.saft.sap.AuditFile;
import org.fenixedu.treasury.generated.sources.saft.sap.OrderReferences;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line.Metadata;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporterUtils;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

/**
 * This extension is temporary and is just to test the integration of invoice entries
 * with quantity different from one #UL-ISEG-4541
 *
 */
public class SAPExporterTestQuantityDifferentOfOne extends SAPExporter {

    @Override
    protected String exportFinantialDocumentToXML(final FinantialInstitution finantialInstitution,
            List<FinantialDocument> documents, final UnaryOperator<AuditFile> preProcessFunctionBeforeSerialize) {

        if (documents.isEmpty()) {
            throw new TreasuryDomainException("error.ERPExporter.no.document.to.export");
        }

        checkForUnsetDocumentSeriesNumberInDocumentsToExport(documents);

        documents = processCreditNoteSettlementsInclusion(documents);

        DateTime beginDate =
                documents.stream().min((x, y) -> x.getDocumentDate().compareTo(y.getDocumentDate())).get().getDocumentDate();
        DateTime endDate =
                documents.stream().max((x, y) -> x.getDocumentDate().compareTo(y.getDocumentDate())).get().getDocumentDate();
        return generateERPFile(finantialInstitution, beginDate, endDate, documents, false, false,
                preProcessFunctionBeforeSerialize);
    }
    
    @Override
    protected org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line convertToSAFTWorkDocumentLine(
            InvoiceEntry entry, Map<String, org.fenixedu.treasury.generated.sources.saft.sap.Product> baseProducts) {
        final FinantialInstitution institution = entry.getDebtAccount().getFinantialInstitution();

        org.fenixedu.treasury.generated.sources.saft.sap.Product currentProduct = null;

        Product product = entry.getProduct();

        if (product.getCode() != null && baseProducts.containsKey(product.getCode())) {
            currentProduct = baseProducts.get(product.getCode());
        } else {
            currentProduct = convertProductToSAFTProduct(product);
            baseProducts.put(currentProduct.getProductCode(), currentProduct);
        }

        XMLGregorianCalendar documentDateCalendar = null;
        try {
            DatatypeFactory dataTypeFactory = DatatypeFactory.newInstance();
            DateTime documentDate = entry.getFinantialDocument().getDocumentDate();

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            documentDateCalendar = convertToXMLDate(dataTypeFactory, documentDate);

        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line line =
                new org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line();

        // Consider in replacing amount with net amount (check SAFT)
        if (entry.isCreditNoteEntry()) {
            line.setCreditAmount(entry.getNetAmount().setScale(2, RoundingMode.HALF_EVEN));
        } else if (entry.isDebitNoteEntry()) {
            line.setDebitAmount(entry.getNetAmount().setScale(2, RoundingMode.HALF_EVEN));
        }

        // If document was exported in legacy ERP than the amount is open amount when integration started
        if (entry.getFinantialDocument().isExportedInLegacyERP()) {
            if (entry.isCreditNoteEntry()) {
                line.setCreditAmount(
                        SAPExporterUtils.openAmountAtDate(entry, ERP_INTEGRATION_START_DATE).setScale(2, RoundingMode.HALF_EVEN));
            } else if (entry.isDebitNoteEntry()) {
                line.setDebitAmount(
                        SAPExporterUtils.openAmountAtDate(entry, ERP_INTEGRATION_START_DATE).setScale(2, RoundingMode.HALF_EVEN));
            }
        }

        // Description
        line.setDescription(StringUtils.abbreviate(entry.getDescription(), 200));
        List<OrderReferences> orderReferences = line.getOrderReferences();

        //Add the references on the document creditEntries <-> debitEntries
        if (entry.isCreditNoteEntry()) {
            CreditEntry creditEntry = (CreditEntry) entry;
            if (creditEntry.getDebitEntry() != null) {
                //Metadata
                Metadata metadata = new Metadata();
                metadata.setDescription(creditEntry.getDebitEntry().getERPIntegrationMetadata());
                line.setMetadata(metadata);

                OrderReferences reference = new OrderReferences();

                if (!creditEntry.getFinantialDocument().isExportedInLegacyERP()) {
                    reference.setOriginatingON(creditEntry.getDebitEntry().getFinantialDocument().getUiDocumentNumber());
                } else {
                    reference.setOriginatingON("");
                }

                reference.setOrderDate(documentDateCalendar);

                if (((DebitNote) creditEntry.getDebitEntry().getFinantialDocument()).isExportedInLegacyERP()) {
                    final DebitNote debitNote = (DebitNote) creditEntry.getDebitEntry().getFinantialDocument();
                    if (!Strings.isNullOrEmpty(debitNote.getLegacyERPCertificateDocumentReference())) {
                        if (!creditEntry.getFinantialDocument().isExportedInLegacyERP()) {
                            reference.setOriginatingON(debitNote.getLegacyERPCertificateDocumentReference());
                        }
                    } else {
                        if (!creditEntry.getFinantialDocument().isExportedInLegacyERP() && !institution
                                .getErpIntegrationConfiguration().isCreditsOfLegacyDebitWithoutLegacyInvoiceExportEnabled()) {
                            throw new TreasuryDomainException(
                                    "error.ERPExporter.credit.note.of.legacy.debit.note.without.legacyERPCertificateDocumentReference",
                                    debitNote.getUiDocumentNumber(), creditEntry.getFinantialDocument().getUiDocumentNumber());
                        }

                        reference.setOriginatingON("");
                    }
                }

                reference.setLineNumber(BigInteger.ONE);

                orderReferences.add(reference);
            }

        } else if (entry.isDebitNoteEntry()) {
            DebitEntry debitEntry = (DebitEntry) entry;

            Metadata metadata = new Metadata();
            metadata.setDescription(debitEntry.getERPIntegrationMetadata());
            line.setMetadata(metadata);
        }

        // ProductCode
        line.setProductCode(currentProduct.getProductCode());

        // ProductDescription
        line.setProductDescription(currentProduct.getProductDescription());

        // Quantity
        line.setQuantity(entry.getQuantity());

        // SettlementAmount
        // In case of DebitEntry should be filled with the DebitEntry::netExemptedAmount
        line.setSettlementAmount(BigDecimal.ZERO);

        // Tax
        line.setTax(getSAFTWorkingDocumentsTax(product, entry));

        line.setTaxPointDate(documentDateCalendar);

        // TaxExemptionReason
        /*
         * Motivo da isen??o de imposto (TaxExemptionReason). Campo de
         * preenchimento obrigat?rio, quando os campos percentagem da taxa de
         * imposto (TaxPercentage) ou montante do imposto (TaxAmount) s?o iguais
         * a zero. Deve ser referido o preceito legal aplic?vel. . . . . . . . .
         * . Texto 60
         */
        if (TreasuryConstants.isEqual(line.getTax().getTaxPercentage(), BigDecimal.ZERO) || (line.getTax().getTaxAmount() != null
                && TreasuryConstants.isEqual(line.getTax().getTaxAmount(), BigDecimal.ZERO))) {
            if (product.getVatExemptionReason() != null) {
                line.setTaxExemptionReason(
                        product.getVatExemptionReason().getCode() + "-" + product.getVatExemptionReason().getName().getContent());
            } else {
                // HACK : DEFAULT
                line.setTaxExemptionReason(treasuryBundle("warning.ERPExporter.vat.exemption.unknown"));
            }
        }

        // UnitOfMeasure
        line.setUnitOfMeasure(product.getUnitOfMeasure().getContent());

        // UnitPrice
        //
        // 2022-11-25: The unit price keeps the unit amount before discount. The SAFT specifies that the unit amount
        // is after the discount. It is safer to calculate the unit amount, as the net amount divided by the quantity.
        // The entry.getNetAmount() already takes into account the discount (which is kept in entry.getNetExemptedAmount())
        line.setUnitPrice(TreasuryConstants.divide(entry.getNetAmount(), entry.getQuantity()).setScale(4, RoundingMode.HALF_UP));
        
        if (entry.getFinantialDocument().isExportedInLegacyERP()) {
            line.setUnitPrice(
                    SAPExporterUtils.openAmountAtDate(entry, ERP_INTEGRATION_START_DATE).setScale(2, RoundingMode.HALF_EVEN));
        }

        return line;
    }
    
}
