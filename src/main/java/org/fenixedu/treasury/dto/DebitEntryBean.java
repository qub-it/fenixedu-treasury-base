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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class DebitEntryBean implements ITreasuryBean {

    private DebitEntry debitEntry;
    private TreasuryEvent treasuryEvent;
    private List<TreasuryTupleDataSourceBean> treasuryEventDataSource;
    private Vat vat;
    private List<TreasuryTupleDataSourceBean> vatDataSource;
    private Product product;
    private List<TreasuryTupleDataSourceBean> productDataSource;
    private DebtAccount debtAccount;
    private List<TreasuryTupleDataSourceBean> debtAccountDataSource;
    private Currency currency;
    private List<TreasuryTupleDataSourceBean> currencyDataSource;
    private DebitNote finantialDocument;
    private boolean eventAnnuled;
    private DateTime entryDate;
    private LocalDate dueDate;
    private String description;
    private BigDecimal amount;
    private BigDecimal quantity;
    private boolean applyInterests;
    private FixedTariffInterestRateBean interestRate;

    private boolean academicalActBlockingSuspension;
    boolean blockAcademicActsOnDebt;

    private boolean showLegacyProducts = false;

    public Boolean isAmountValuesEditable() {
        return false;
    }

    public TreasuryEvent getTreasuryEvent() {
        return treasuryEvent;
    }

    public void setTreasuryEvent(TreasuryEvent value) {
        treasuryEvent = value;
    }

    public List<TreasuryTupleDataSourceBean> getTreasuryEventDataSource() {
        return treasuryEventDataSource;
    }

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
        final String inactiveDescription = " " + Constants.bundle("label.DebitEntryBean.inactive.description");

        this.productDataSource =
                value.stream().sorted((x, y) -> x.getName().getContent().compareToIgnoreCase(y.getName().getContent())).map(x -> {
                    TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
                    tuple.setId(x.getExternalId());
                    tuple.setText(String.format("%s", x.getName().getContent().replace("\"", "").replace("'", ""))
                            + (!x.isActive() ? inactiveDescription : ""));
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

    public DebitNote getFinantialDocument() {
        return finantialDocument;
    }

    public void setFinantialDocument(DebitNote value) {
        finantialDocument = value;
    }

    public boolean getEventAnnuled() {
        return eventAnnuled;
    }

    public void setEventAnnuled(boolean value) {
        eventAnnuled = value;
    }

    public org.joda.time.LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(org.joda.time.LocalDate value) {
        dueDate = value;
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

    public DebitEntryBean() {
        this.setEntryDate(new DateTime());
        this.setDueDate(new LocalDate());
        this.setQuantity(BigDecimal.ONE);
        this.setAmount(BigDecimal.ZERO);
        this.setInterestRate(new FixedTariffInterestRateBean());
        this.setAcademicalActBlockingSuspension(false);
        this.setBlockAcademicActsOnDebt(false);
    }

    public DebitEntryBean(DebitEntry debitEntry) {
        this();
        this.setDebitEntry(debitEntry);
        this.setTreasuryEvent(debitEntry.getTreasuryEvent());
        this.setVat(debitEntry.getVat());
        this.setProduct(debitEntry.getProduct());
        this.setDebtAccount(debitEntry.getDebtAccount());
        this.setCurrency(debitEntry.getCurrency());
        this.setFinantialDocument((DebitNote) debitEntry.getFinantialDocument());
        this.setEventAnnuled(debitEntry.getEventAnnuled());
        this.setDueDate(debitEntry.getDueDate());
        this.setDescription(debitEntry.getDescription());
        this.setEntryDate(debitEntry.getEntryDateTime());
        this.setDueDate(debitEntry.getDueDate());
        this.setDescription(debitEntry.getDescription());
        this.setAmount(debitEntry.getAmount());
        this.setQuantity(debitEntry.getQuantity());
        if (debitEntry.getInterestRate() == null) {
            this.applyInterests = false;
            this.setInterestRate(new FixedTariffInterestRateBean());
        } else {
            this.applyInterests = true;
            this.setInterestRate(new FixedTariffInterestRateBean(debitEntry.getInterestRate()));
        }
        this.setTreasuryEventDataSource(
                TreasuryEvent.find(debitEntry.getDebtAccount().getCustomer()).collect(Collectors.<TreasuryEvent> toList()));

        this.setAcademicalActBlockingSuspension(!debitEntry.isAcademicalActBlockingSuspension());
        this.setBlockAcademicActsOnDebt(debitEntry.isBlockAcademicActsOnDebt());
    }

    public boolean isApplyInterests() {
        return applyInterests;
    }

    public void setApplyInterests(boolean applyInterests) {
        this.applyInterests = applyInterests;
    }

    public FixedTariffInterestRateBean getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(FixedTariffInterestRateBean interestRate) {
        this.interestRate = interestRate;
    }

    public DateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(DateTime entryDate) {
        this.entryDate = entryDate;
    }

    public DebitEntry getDebitEntry() {
        return debitEntry;
    }

    public void setDebitEntry(DebitEntry debitEntry) {
        this.debitEntry = debitEntry;
    }

    public boolean isAcademicalActBlockingSuspension() {
        return academicalActBlockingSuspension;
    }

    public void setAcademicalActBlockingSuspension(boolean academicalActBlockingSuspension) {
        this.academicalActBlockingSuspension = academicalActBlockingSuspension;
    }

    public boolean isBlockAcademicActsOnDebt() {
        return blockAcademicActsOnDebt;
    }

    public void setBlockAcademicActsOnDebt(boolean blockAcademicActsOnDebt) {
        this.blockAcademicActsOnDebt = blockAcademicActsOnDebt;
    }

    public boolean isShowLegacyProducts() {
        return showLegacyProducts;
    }

    public void setShowLegacyProducts(boolean showLegacyProducts) {
        this.showLegacyProducts = showLegacyProducts;
    }

    public void setTreasuryEventDataSource(List<TreasuryEvent> treasuryEventDataSource) {
        this.treasuryEventDataSource = treasuryEventDataSource.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(x.getExternalId());
            String text = "";
            if (x.getTreasuryEventDate() != null) {
                text += ("[" + x.getTreasuryEventDate().toString("YYYY-MM-dd") + "] ");
            } else {
                text += "[YYYY-MM-dd] ";
            }
            if (x.getDescription() != null) {
                text += x.getDescription().getContent();
            } else {
                text += "---";
            }
            tuple.setText(text);
            return tuple;
        }).collect(Collectors.toList());
    }

    public void refreshProductsDataSource() {
        setProductDataSource(isShowLegacyProducts() ? Product.findAllLegacy().collect(Collectors.toList()) : Product
                .findAllActive().collect(Collectors.toList()));
    }
}
