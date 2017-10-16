package org.fenixedu.treasury.services.integration.erp.dto;


import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.Constants;

public class DocumentStatusWS {
    private String documentNumber;
    private String certifiedDocumentURL;
    private String sapDocumentNumber;
    private StatusType integrationStatus;
    private String errorDescription;

    public enum StatusType {
        PENDING, ERROR, SUCCESS;

        public LocalizedString getDescriptionI18N() {
            return Constants.bundleI18N(getClass().getSimpleName() + "." + name());
        }
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
    
    public String getCertifiedDocumentURL() {
        return certifiedDocumentURL;
    }
    
    public void setCertifiedDocumentURL(String certifiedDocumentURL) {
        this.certifiedDocumentURL = certifiedDocumentURL;
    }
    
    public String getSapDocumentNumber() {
        return sapDocumentNumber;
    }
    
    public void setSapDocumentNumber(String sapDocumentNumber) {
        this.sapDocumentNumber = sapDocumentNumber;
    }
    
    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public StatusType getIntegrationStatus() {
        return integrationStatus;
    }

    public void setIntegrationStatus(StatusType integrationStatus) {
        this.integrationStatus = integrationStatus;
    }

    public boolean isIntegratedWithSuccess() {
        return this.getIntegrationStatus() != null && this.getIntegrationStatus().equals(StatusType.SUCCESS);
    }
}