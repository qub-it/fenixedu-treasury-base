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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.tariff.Tariff;

public class CreditEntryBean implements ITreasuryBean {

    private Vat vat;
    private List<TreasuryTupleDataSourceBean> vatDataSource;
    private Product product;
    private List<TreasuryTupleDataSourceBean> productDataSource;
    private DebtAccount debtAccount;
    private List<TreasuryTupleDataSourceBean> debtAccountDataSource;
    private Currency currency;
    private List<TreasuryTupleDataSourceBean> currencyDataSource;
    private CreditNote finantialDocument;
    private boolean eventAnnuled;
    private String description;
    private BigDecimal amount;
    private BigDecimal quantity;

    private Tariff tariff;

    public Vat getVat() {
        return vat;
    }

    public void setVat(Vat value) {
        vat = value;
    }

    public List<TreasuryTupleDataSourceBean> getVatDataSource() {
        return vatDataSource;
    }

    public void setVatDataSource(List<Vat> value) {
        this.vatDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product value) {
        product = value;
    }

    public List<TreasuryTupleDataSourceBean> getProductDataSource() {
        return productDataSource;
    }

    public void setProductDataSource(List<Product> value) {
        this.productDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.getName().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public void setDebtAccount(DebtAccount value) {
        debtAccount = value;
    }

    public List<TreasuryTupleDataSourceBean> getDebtAccountDataSource() {
        return debtAccountDataSource;
    }

    public void setDebtAccountDataSource(List<DebtAccount> value) {
        this.debtAccountDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency value) {
        currency = value;
    }

    public List<TreasuryTupleDataSourceBean> getCurrencyDataSource() {
        return currencyDataSource;
    }

    public void setCurrencyDataSource(List<Currency> value) {
        this.currencyDataSource = value.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            tuple.setText(x.toString());
            return tuple;
        }).collect(Collectors.toList());
    }

    public CreditNote getFinantialDocument() {
        return finantialDocument;
    }

    public void setFinantialDocument(CreditNote value) {
        finantialDocument = value;
    }

    public boolean getEventAnnuled() {
        return eventAnnuled;
    }

    public void setEventAnnuled(boolean value) {
        eventAnnuled = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal value) {
        amount = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal value) {
        quantity = value;
    }

    public CreditEntryBean() {
        this.setQuantity(BigDecimal.ONE);
        this.setAmount(BigDecimal.ZERO);
    }

    public CreditEntryBean(CreditEntry creditEntry) {
        this();
        this.setVat(creditEntry.getVat());
        this.setProduct(creditEntry.getProduct());
        this.setDebtAccount(creditEntry.getDebtAccount());
        this.setCurrency(creditEntry.getCurrency());
        this.setFinantialDocument((CreditNote) creditEntry.getFinantialDocument());
        this.setDescription(creditEntry.getDescription());
        this.setAmount(creditEntry.getAmount());
        this.setQuantity(creditEntry.getQuantity());
        this.setDescription(creditEntry.getDescription());
        this.setAmount(creditEntry.getAmount());
        this.setQuantity(creditEntry.getQuantity());
    }

    public Tariff getTariff() {
        return tariff;
    }

    public void setTariff(Tariff tariff) {
        this.tariff = tariff;
    }
}
