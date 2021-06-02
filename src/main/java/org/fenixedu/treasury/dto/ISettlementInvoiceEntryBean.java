package org.fenixedu.treasury.dto;

import java.math.BigDecimal;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.joda.time.LocalDate;

public interface ISettlementInvoiceEntryBean {

    public InvoiceEntry getInvoiceEntry();

    public String getDescription();

    public LocalDate getDueDate();

    /**
     * Amount divida
     * Open amount divida
     *
     * paymentAmount
     *
     * @return
     */

    public BigDecimal getEntryAmount();

    public BigDecimal getEntryOpenAmount();

    public BigDecimal getSettledAmount();

    public void setSettledAmount(BigDecimal debtAmount);

    public Vat getVat();

    public BigDecimal getVatRate();

    public boolean isIncluded();

    public void setIncluded(boolean isIncluded);

    public boolean isNotValid();

    public void setNotValid(boolean notValid);

    public FinantialDocument getFinantialDocument();

    // TODO: Rename method to getPaymentCustomers or getPaymentCustomerSet
    public Set<Customer> getPaymentCustomer();

    /*
     * Methods to support jsp, overriden in subclasses
     */

    boolean isForDebitEntry();

    boolean isForInstallment();

    boolean isForCreditEntry();

    boolean isForPendingInterest();

    boolean isForPaymentPenalty();

    boolean isForPendingDebitEntry();
    /**
     * Descrição
     *
     * DueDate
     *
     * DebitAmount
     *
     * OpenAmount
     *
     * Iva
     *
     * SettlementAmount
     *
     * private boolean isIncluded;
     * private boolean isNotValid;
     *
     */

}
