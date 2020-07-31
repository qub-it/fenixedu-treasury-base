package org.fenixedu.treasury.services.payments.sibs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.joda.time.DateTime;

public class SIBSImportationFileDTO {

    protected DateTime whenProcessedBySibs;
    protected String filename;
    protected BigDecimal transactionsTotalAmount;
    protected BigDecimal totalCost;
    protected Integer fileVersion;
    private String sibsEntityCode;

    protected List<SIBSImportationLineDTO> lines;

    public SIBSImportationFileDTO(final SibsIncommingPaymentFile sibsIncomingPaymentFile) {
        setWhenProcessedBySibs(sibsIncomingPaymentFile.getHeader().getWhenProcessedBySibs().toDateTimeAtMidnight());
        setFilename(sibsIncomingPaymentFile.getFilename());
        setTransactionsTotalAmount(sibsIncomingPaymentFile.getFooter().getTransactionsTotalAmount());
        setTotalCost(sibsIncomingPaymentFile.getFooter().getTotalCost());
        setFileVersion(sibsIncomingPaymentFile.getHeader().getVersion());
        setSibsEntityCode(sibsIncomingPaymentFile.getHeader().getEntityCode());

        setLines(generateLines(sibsIncomingPaymentFile));
    }

    protected List<SIBSImportationLineDTO> generateLines(final SibsIncommingPaymentFile sibsIncomingPaymentFile) {

        ArrayList<SIBSImportationLineDTO> result = new ArrayList<SIBSImportationLineDTO>();
        for (SibsIncommingPaymentFileDetailLine dto : sibsIncomingPaymentFile.getDetailLines()) {
            result.add(new SIBSImportationLineDTO(SIBSImportationFileDTO.this, dto));
        }
        return result;
    }

    public List<SIBSImportationLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<SIBSImportationLineDTO> lines) {
        this.lines = lines;
    }

    public DateTime getWhenProcessedBySibs() {
        return whenProcessedBySibs;
    }

    public void setWhenProcessedBySibs(final DateTime whenProcessedBySibs) {
        this.whenProcessedBySibs = whenProcessedBySibs;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public BigDecimal getTransactionsTotalAmount() {
        return transactionsTotalAmount;
    }

    public void setTransactionsTotalAmount(BigDecimal transactionsTotalAmount) {
        this.transactionsTotalAmount = transactionsTotalAmount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(final Integer fileVersion) {
        this.fileVersion = fileVersion;
    }

    public String getSibsEntityCode() {
        return sibsEntityCode;
    }
    
    public void setSibsEntityCode(String sibsEntityId) {
        this.sibsEntityCode = sibsEntityId;
    }
}
