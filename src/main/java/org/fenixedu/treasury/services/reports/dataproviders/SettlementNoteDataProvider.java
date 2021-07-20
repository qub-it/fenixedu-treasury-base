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
package org.fenixedu.treasury.services.reports.dataproviders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;

import com.qubit.terra.docs.util.IDocumentFieldsData;
import com.qubit.terra.docs.util.IFieldsExporter;
import com.qubit.terra.docs.util.IReportDataProvider;

public class SettlementNoteDataProvider extends AbstractDataProvider implements IReportDataProvider {

    protected static final String DOCUMENT_TYPE_KEY = "settlementNoteDocumentType";
    protected static final String DOCUMENT_KEY = "settlementNote";
    protected static final String LINES_KEY = "settlementNoteLines";
    protected static final String PAYMENT_LINES_KEY = "settlementNotePaymentLines";
    protected final List<String> allKeys = new ArrayList<String>();
    protected Map<String, Function<IReportDataProvider, Object>> keysDictionary =
            new HashMap<String, Function<IReportDataProvider, Object>>();

    private SettlementNote note;

    public SettlementNoteDataProvider(final SettlementNote note) {
        this.note = note;
        registerKey(DOCUMENT_TYPE_KEY, SettlementNoteDataProvider::handleDocumentTypeKey);
        registerKey(DOCUMENT_KEY, SettlementNoteDataProvider::handleDocument);
        registerKey(LINES_KEY, SettlementNoteDataProvider::handleLines);
        registerKey(PAYMENT_LINES_KEY, SettlementNoteDataProvider::handlePaymentLines);
    }

    private static Object handleDocumentTypeKey(IReportDataProvider provider) {
        SettlementNoteDataProvider invoiceProvider = (SettlementNoteDataProvider) provider;
        return invoiceProvider.note.getFinantialDocumentType().getType().getDescriptionI18N();
    }

    private static Object handleLines(IReportDataProvider provider) {
        SettlementNoteDataProvider invoiceProvider = (SettlementNoteDataProvider) provider;
        TreeSet<SettlementEntry> settlementNoteEntriesSet = new TreeSet<>(SettlementEntry.COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION);
        settlementNoteEntriesSet.addAll(invoiceProvider.note.getSettlemetEntriesSet());
        
        return settlementNoteEntriesSet;
    }

    private static Object handlePaymentLines(IReportDataProvider provider) {
        SettlementNoteDataProvider invoiceProvider = (SettlementNoteDataProvider) provider;
        return invoiceProvider.note.getPaymentEntriesSet();
    }

    private static Object handleDocument(IReportDataProvider provider) {
        SettlementNoteDataProvider invoiceProvider = (SettlementNoteDataProvider) provider;
        return invoiceProvider.note;
    }

    @Override
    public void registerFieldsAndImages(IDocumentFieldsData arg0) {
        // TODO Auto-generated method stub
        arg0.registerCollectionAsField(LINES_KEY);
        arg0.registerCollectionAsField(PAYMENT_LINES_KEY);
    }

}
