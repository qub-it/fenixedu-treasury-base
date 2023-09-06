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
package org.fenixedu.treasury.services.reports;

import java.util.List;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.document.TreasuryDocumentTemplate;
import org.fenixedu.treasury.services.reports.dataproviders.CustomerDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.DebtAccountDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.FinantialInstitutionDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.InvoiceDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.SettlementNoteDataProvider;
import org.fenixedu.treasury.services.reports.helpers.DateHelper;
import org.fenixedu.treasury.services.reports.helpers.EnumerationHelper;
import org.fenixedu.treasury.services.reports.helpers.LanguageHelper;
import org.fenixedu.treasury.services.reports.helpers.MoneyHelper;
import org.fenixedu.treasury.services.reports.helpers.NumbersHelper;
import org.fenixedu.treasury.services.reports.helpers.StringsHelper;

import com.qubit.terra.docs.core.DocumentGenerator;
import com.qubit.terra.docs.core.DocumentTemplateEngine;
import com.qubit.terra.docs.core.IDocumentTemplateService;

public class DocumentPrinter {
    static {
        registerService();
    }

    public static final String PDF = DocumentGenerator.PDF;
    public static final String ODT = DocumentGenerator.ODT;

    private static void registerService() {
        IDocumentTemplateService service = new DocumentPrinterConfiguration();
        DocumentTemplateEngine.registerServiceImplementations(service);
    }

    private static void registerHelpers(DocumentGenerator generator) {
        generator.registerHelper("dates", new DateHelper());
        generator.registerHelper("lang", new LanguageHelper());
        generator.registerHelper("numbers", new NumbersHelper());
        generator.registerHelper("enumeration", new EnumerationHelper());
        generator.registerHelper("strings", new StringsHelper());
        generator.registerHelper("money", new MoneyHelper());
    }

    public static byte[] printDebtAccountPaymentPlan(DebtAccount debtAccount, String outputMimeType) {

        return printDebitNotesPaymentPlan(debtAccount, null, outputMimeType);
    }

    public static byte[] printDebitNotesPaymentPlan(DebtAccount debtAccount, List<DebitNote> documents, String outputMimeType) {

        DocumentGenerator generator = null;

//      if (templateInEntity != null) {
//          generator = DocumentGenerator.create(templateInEntity, DocumentGenerator.ODT);
//
//      } else {
        //HACK...
        generator = DocumentGenerator.create(
                "F:\\O\\fenixedu\\fenixedu-academic-treasury\\src\\main\\resources\\templates\\tuitionsPaymentPlan.odt",
                outputMimeType);
//          throw new TreasuryDomainException("error.ReportExecutor.document.template.not.available");
//      }

        Customer customer = debtAccount.getCustomer();
        FinantialInstitution finst = debtAccount.getFinantialInstitution();

        registerHelpers(generator);
        generator.registerDataProvider(new DebtAccountDataProvider(debtAccount, documents));
        generator.registerDataProvider(new CustomerDataProvider(customer, "customer"));
        generator.registerDataProvider(new FinantialInstitutionDataProvider(finst));

        //... add more providers...

        byte[] outputReport = generator.generateReport();

        return outputReport;
    }

    //https://github.com/qub-it/fenixedu-qubdocs-reports/blob/master/src/main/java/org/fenixedu/academic/util/report/DocumentPrinter.java
    public static byte[] printFinantialDocument(FinantialDocument document, String outputMimeType) {

        TreasuryDocumentTemplate templateInEntity = TreasuryDocumentTemplate
                .findByFinantialDocumentTypeAndFinantialEntity(document.getFinantialDocumentType(),
                        document.getDebtAccount().getFinantialInstitution().getFinantialEntitiesSet().iterator().next())
                .filter(x -> x.isActive()).findFirst().orElse(null);
        DocumentGenerator generator = null;

        if (templateInEntity != null) {
            generator = DocumentGenerator.create(templateInEntity, outputMimeType);

        } else {
            generator = DocumentGenerator.create(
                    "F:\\O\\fenixedu\\fenixedu-treasury\\src\\main\\resources\\document_templates\\settlementNote.odt",
                    outputMimeType);
//            //HACK...
//            throw new TreasuryDomainException("error.ReportExecutor.document.template.not.available");
        }

        registerHelpers(generator);
        if (document.isInvoice()) {
            generator.registerDataProvider(new InvoiceDataProvider((Invoice) document));
        } else if (document.isSettlementNote()) {
            generator.registerDataProvider(new SettlementNoteDataProvider((SettlementNote) document));
        }
        generator.registerDataProvider(new DebtAccountDataProvider(document.getDebtAccount()));
        generator.registerDataProvider(new CustomerDataProvider(document.getDebtAccount().getCustomer(), "customer"));
        generator.registerDataProvider(new FinantialInstitutionDataProvider(document.getDebtAccount().getFinantialInstitution()));

        if (document.isInvoice()) {
            final Invoice invoice = (Invoice) document;

            final Customer payorCustomer = invoice.getPayorDebtAccount() != null ? 
                    invoice.getPayorDebtAccount().getCustomer() : invoice.getDebtAccount().getCustomer();
            generator.registerDataProvider(new CustomerDataProvider(payorCustomer, "payorCustomer"));
        } else if(document.isSettlementNote()) {
            final SettlementNote settlementNote = (SettlementNote) document;

            final Customer payorCustomer = settlementNote.getReferencedCustomers().iterator().next();
            generator.registerDataProvider(new CustomerDataProvider(payorCustomer, "payorCustomer"));
        }
        //... add more providers...

        byte[] outputReport = generator.generateReport();

        return outputReport;
    }
}
