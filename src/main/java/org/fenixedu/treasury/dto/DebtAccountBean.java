/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com
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

package org.fenixedu.treasury.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
