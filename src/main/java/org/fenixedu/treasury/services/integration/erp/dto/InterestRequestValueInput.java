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
package org.fenixedu.treasury.services.integration.erp.dto;

import java.math.BigDecimal;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class InterestRequestValueInput {
    private String finantialInstitutionFiscalNumber;
    private String customerCode;
    private String debitNoteNumber;
    private Integer lineNumber;
    private BigDecimal amount;
    private String paymentDate;
    private Boolean generateInterestDebitNote;

    public Boolean getGenerateInterestDebitNote() {
        return generateInterestDebitNote;
    }

    public void setGenerateInterestDebitNote(Boolean generateInterestDebitNote) {
        this.generateInterestDebitNote = generateInterestDebitNote;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDebitNoteNumber() {
        return debitNoteNumber;
    }

    public void setDebitNoteNumber(String debitNoteNumber) {
        this.debitNoteNumber = debitNoteNumber;
    }

    public String getFinantialInstitutionFiscalNumber() {
        return finantialInstitutionFiscalNumber;
    }

    public void setFinantialInstitutionFiscalNumber(String finantialInstitutionFiscalNumber) {
        this.finantialInstitutionFiscalNumber = finantialInstitutionFiscalNumber;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String value) {
        paymentDate = value;
    }

    public LocalDate convertPaymentDateToLocalDate() {
        try {
            DateTimeFormatter createDateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd");
            return LocalDate.parse(paymentDate, createDateTimeFormatter);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid date format. Date Format MUST be: YYYY-MM-dd");
        }
    }

    public void convertLocalDateToPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate.toString("YYYY-MM-dd");
    }
}
