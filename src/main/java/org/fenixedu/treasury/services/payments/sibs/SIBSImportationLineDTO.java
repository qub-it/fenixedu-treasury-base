package org.fenixedu.treasury.services.payments.sibs;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.joda.time.DateTime;

public class SIBSImportationLineDTO {

    protected SibsIncommingPaymentFileDetailLine line;
    protected SibsReferenceCode paymentCode;
    private SIBSImportationFileDTO sibsImportationFileDTO;

    public SIBSImportationLineDTO(final SIBSImportationFileDTO sibsImportationFileDTO,
            final SibsIncommingPaymentFileDetailLine line) {
        this.line = line;
        this.paymentCode = SIBSPaymentsImporter.getPaymentCode(sibsImportationFileDTO.getSibsEntityCode(), line.getCode());

        setSibsImportationFileDTO(sibsImportationFileDTO);
    }

    public DateTime getWhenProcessedBySibs() {
        return getSibsImportationFileDTO().getWhenProcessedBySibs();
    }

    public String getFilename() {
        return getSibsImportationFileDTO().getFilename();
    }

    public BigDecimal getTransactionsTotalAmount() {
        return getSibsImportationFileDTO().getTransactionsTotalAmount();
    }

    public BigDecimal getTotalCost() {
        return getSibsImportationFileDTO().getTotalCost();
    }

    public Integer getFileVersion() {
        return getSibsImportationFileDTO().getFileVersion();
    }

    public String getSibsTransactionId() {
        return line.getSibsTransactionId();
    }

    public BigDecimal getTransactionTotalAmount() {
        return line.getAmount();
    }

    public DateTime getTransactionWhenRegistered() {
        return line.getWhenOccuredTransaction();
    }

    public String getCode() {
        return line.getCode();
    }

    public String getPersonName() {
        if (this.paymentCode == null || this.paymentCode.getSibsPaymentRequest() == null) {
            return null;
        }

        return this.paymentCode.getSibsPaymentRequest().getDebtAccount().getCustomer().getName();
    }

    public String getStudentNumber() {
        if (this.paymentCode == null || this.paymentCode.getSibsPaymentRequest() == null) {
            return null;
        }

        return this.paymentCode.getSibsPaymentRequest().getDebtAccount().getCustomer().getBusinessIdentification();
    }

    public String getDescription() {
        if (this.paymentCode == null || this.paymentCode.getSibsPaymentRequest() == null) {
            return null;
        }

        return this.paymentCode.getSibsPaymentRequest().getDescription();
    }

    public SIBSImportationFileDTO getSibsImportationFileDTO() {
        return sibsImportationFileDTO;
    }

    public void setSibsImportationFileDTO(SIBSImportationFileDTO sibsImportationFileDTO) {
        this.sibsImportationFileDTO = sibsImportationFileDTO;
    }

}
