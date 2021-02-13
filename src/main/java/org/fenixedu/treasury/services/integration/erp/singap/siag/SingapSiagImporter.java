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
package org.fenixedu.treasury.services.integration.erp.singap.siag;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.ReimbursementEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.ERPImportOperation;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.AuditFile;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.PaymentMethod;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.SAFTPTSettlementType;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.SourceDocuments.Payments.Payment;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.SourceDocuments.Payments.Payment.Line;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.SourceDocuments.WorkingDocuments.WorkDocument;
import org.fenixedu.treasury.services.integration.erp.IERPImporter;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS.StatusType;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

// ******************************************************************************************************************************
// http://info.portaldasfinancas.gov.pt/NR/rdonlyres/3B4FECDB-2380-45D7-9019-ABCA80A7E99E/0/Comunicacao_Dados_Doc_Transporte.pdf
// http://info.portaldasfinancas.gov.pt/NR/rdonlyres/15D18787-8AA9-4060-90D5-79F168A927A4/0/Portaria_11922009.pdf
// (Documento Original)
// http://dre.pt/pdf1sdip/2012/11/22700/0672406740.pdf (Adenda para os
// Documentos de Transporte)
// Versão 1.0.3
// https://info.portaldasfinancas.gov.pt/NR/rdonlyres/BA9FB096-D482-445D-A5DB-C05B1980F7D7/0/Portaria_274_2013_21_09.pdf
// ******************************************************************************************************************************
public class SingapSiagImporter implements IERPImporter {

    private static JAXBContext jaxbContext = null;
    private static Logger logger = LoggerFactory.getLogger(SingapSiagImporter.class);
    private InputStream fileStream;

    public SingapSiagImporter(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    public AuditFile readAuditFileFromXML() {
        try {

            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(AuditFile.class);
            }
            javax.xml.bind.Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            StringWriter writer = new StringWriter();

            AuditFile auditFile = (AuditFile) jaxbUnmarshaller.unmarshal(fileStream);
            return auditFile;
        } catch (JAXBException e) {
            return null;
        }
    }

    @Atomic(mode = TxMode.WRITE)
    @Override
    public DocumentsInformationOutput processAuditFile(final ERPImportOperation eRPImportOperation) {
        throw new RuntimeException("not supported");
    }

    @Atomic
    private SettlementNote processErpPayment(Payment payment, ERPImportOperation eRPImportOperation,
            final IntegrationOperationLogBean logBean) {
        throw new RuntimeException("not supported");
    }

    //convert the saft payment method to fenixEdu payment entry
    private org.fenixedu.treasury.domain.PaymentMethod convertFromSAFTPaymentMethod(String paymentMechanism) {
        org.fenixedu.treasury.domain.PaymentMethod paymentMethod =
                org.fenixedu.treasury.domain.PaymentMethod.findByCode(paymentMechanism);
        if (paymentMethod == null) {
            throw new TreasuryDomainException("error.ERPImporter.unkown.payment.method", paymentMechanism);
        }
        return paymentMethod;
    }

    public Set<String> getRelatedDocumentsNumber() {
        Set<String> result = new HashSet<String>();

        AuditFile file = readAuditFileFromXML();

        for (WorkDocument w : file.getSourceDocuments().getWorkingDocuments().getWorkDocument()) {
            result.add(w.getDocumentNumber());
        }
        for (org.fenixedu.treasury.generated.sources.saft.singap.siag.SourceDocuments.SalesInvoices.Invoice i : file
                .getSourceDocuments().getSalesInvoices().getInvoice()) {
            result.add(i.getInvoiceNo());
        }
        for (Payment p : file.getSourceDocuments().getPayments().getPayment()) {
            result.add(p.getPaymentRefNo());
        }
        return result;
    }

    @Override
    public String readTaxRegistrationNumberFromAuditFile() {
        final AuditFile auditFile = readAuditFileFromXML();
        return auditFile.getHeader().getTaxRegistrationNumber() + "";
    }

}
