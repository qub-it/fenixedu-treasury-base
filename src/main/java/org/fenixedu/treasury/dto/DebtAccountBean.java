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
package org.fenixedu.treasury.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;

public class DebtAccountBean implements ITreasuryBean {

    private FinantialInstitution finantialInstitution;
    private List<TreasuryTupleDataSourceBean> finantialInstitutionDataSource;
    private Customer customer;
    private List<TreasuryTupleDataSourceBean> customerDataSource;

    private Set<FinantialDocument> finantialDocuments;
    private List<TreasuryTupleDataSourceBean> finantialDocumentsDataSource;
    private List<Invoice> invoice;
    private List<TreasuryTupleDataSourceBean> invoiceDataSource;
    private List<InvoiceEntry> invoiceEntry;
    private List<TreasuryTupleDataSourceBean> invoiceEntryDataSource;

    public FinantialInstitution getFinantialInstitution() {
        return finantialInstitution;
    }

    public void setFinantialInstitution(FinantialInstitution value) {
        finantialInstitution = value;
    }

    public List<TreasuryTupleDataSourceBean> getFinantialInstitutionDataSource() {
        return finantialInstitutionDataSource;
    }

    public void setFinantialInstitutionDataSource(List<FinantialInstitution> value) {
        this.finantialInstitutionDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer value) {
        customer = value;
    }

    public List<TreasuryTupleDataSourceBean> getCustomerDataSource() {
        return customerDataSource;
    }

    public void setCustomerDataSource(List<Customer> value) {
        this.customerDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public Set<FinantialDocument> getFinantialDocuments() {
        return finantialDocuments;
    }

    public void setFinantialDocuments(Set<FinantialDocument> value) {
        finantialDocuments = value;
    }

    public List<TreasuryTupleDataSourceBean> getFinantialDocumentsDataSource() {
        return finantialDocumentsDataSource;
    }

    public void setFinantialDocumentsDataSource(List<FinantialDocument> value) {
        this.finantialDocumentsDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public List<Invoice> getInvoice() {
        return invoice;
    }

    public void setInvoice(List<Invoice> value) {
        invoice = value;
    }

    public List<TreasuryTupleDataSourceBean> getInvoiceDataSource() {
        return invoiceDataSource;
    }

    public void setInvoiceDataSource(List<Invoice> value) {
        this.invoiceDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public List<InvoiceEntry> getInvoiceEntry() {
        return invoiceEntry;
    }

    public void setInvoiceEntry(List<InvoiceEntry> value) {
        invoiceEntry = value;
    }

    public List<TreasuryTupleDataSourceBean> getInvoiceEntryDataSource() {
        return invoiceEntryDataSource;
    }

    public void setInvoiceEntryDataSource(List<InvoiceEntry> value) {
        this.invoiceEntryDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public DebtAccountBean() {
    }

    public DebtAccountBean(DebtAccount debtAccount) {
        this.setFinantialInstitution(debtAccount.getFinantialInstitution());
        this.setCustomer(debtAccount.getCustomer());
        this.setFinantialDocuments(debtAccount.getFinantialDocumentsSet());
    }
}
