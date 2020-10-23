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
package org.fenixedu.treasury.domain.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public abstract class IntegrationOperation extends IntegrationOperation_Base {

    private static final String ERROR_LOG_TXT_FILENAME = "errorLog.txt";
    private static final String INTEGRATION_LOG_TXT_FILENAME = "integrationLog.txt";
    private static final String SOAP_OUTBOUND_MESSAGE_TXT_FILENAME = "soapOutboundMessage.txt";
    private static final String SOAP_INBOUND_MESSAGE_TXT_FILENAME = "soapInboundMessage.txt";

    protected IntegrationOperation() {
        super();
    }

    protected void init(final String erpOperationId, final DateTime executionDate, final boolean processed, final boolean success,
            final String errorLog) {
        setErpOperationId(erpOperationId);
        setExecutionDate(executionDate);
        setProcessed(processed);
        setSuccess(success);
        setErrorLog(errorLog);
        checkRules();
    }

    private void checkRules() {

        if (Strings.isNullOrEmpty(this.getErrorLog()) == false) {
            this.setSuccess(false);
        }
    }

    @Atomic
    public void appendLog(String errorLog, String integrationLog, String soapInboundMessage, String soapOutboundMessage) {

        if (errorLog == null) {
            errorLog = "";
        }

        if (integrationLog == null) {
            integrationLog = "";
        }

        if (soapInboundMessage == null) {
            soapInboundMessage = "";
        }

        if (soapOutboundMessage == null) {
            soapOutboundMessage = "";
        }

        if (!Strings.isNullOrEmpty(getErrorLog())) {
            errorLog = getErrorLog() + errorLog;
        }

        if (!Strings.isNullOrEmpty(getIntegrationLog())) {
            integrationLog = getIntegrationLog() + integrationLog;
        }

        if (!Strings.isNullOrEmpty(getSoapInboundMessage())) {
            soapInboundMessage = getSoapInboundMessage() + soapInboundMessage;
        }

        if (!Strings.isNullOrEmpty(getSoapOutboundMessage())) {
            soapOutboundMessage = getSoapOutboundMessage() + soapOutboundMessage;
        }

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            zos.putNextEntry(new ZipEntry(ERROR_LOG_TXT_FILENAME));
            zos.write(errorLog.getBytes("UTF-8"));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(INTEGRATION_LOG_TXT_FILENAME));
            zos.write(integrationLog.getBytes("UTF-8"));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(SOAP_INBOUND_MESSAGE_TXT_FILENAME));
            zos.write(soapInboundMessage.getBytes("UTF-8"));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(SOAP_OUTBOUND_MESSAGE_TXT_FILENAME));
            zos.write(soapOutboundMessage.getBytes("UTF-8"));
            zos.closeEntry();

            zos.close();
            baos.close();

            final byte[] contents = baos.toByteArray();

            if (getLogFile() != null) {
                getLogFile().delete();
            }

            OperationFile.createLog(String.format("integrationOperationLogs-%s.zip", getExternalId()), contents, this);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.IntegrationOperation.cannot.delete");
        }
        this.setFinantialInstitution(null);
        if (this.getFile() != null) {
            this.getFile().delete();
        }
        this.setFile(null);
        
        if(getLogFile() != null) {
            getLogFile().delete();
        }
        
        deleteDomainObject();
    }

    private String unzip(String possibleZippedString) {
        String value = possibleZippedString;
        if (value != null) {
            try {
                GZIPInputStream gzipInputStream =
                        new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(possibleZippedString)));
                value = IOUtils.toString(gzipInputStream);
                gzipInputStream.close();
            } catch (Throwable t) {
                // mostprobably is not a zipped string so we just return the value
                value = possibleZippedString;
            }
        }
        return value;
    }

    @Override
    public String getSoapInboundMessage() {
        final String soapInboundMessage = readLogZipFile(SOAP_INBOUND_MESSAGE_TXT_FILENAME);

        if (!Strings.isNullOrEmpty(soapInboundMessage)) {
            return soapInboundMessage;
        }

        return unzip(super.getSoapInboundMessage());
    }

    @Override
    public String getSoapOutboundMessage() {
        final String soapOutboundMessage = readLogZipFile(SOAP_OUTBOUND_MESSAGE_TXT_FILENAME);

        if (!Strings.isNullOrEmpty(soapOutboundMessage)) {
            return soapOutboundMessage;
        }

        return unzip(super.getSoapOutboundMessage());
    }

    @Override
    public String getIntegrationLog() {
        final String integrationLog = readLogZipFile(INTEGRATION_LOG_TXT_FILENAME);

        if (!Strings.isNullOrEmpty(integrationLog)) {
            return integrationLog;
        }

        return unzip(super.getIntegrationLog());
    }

    @Override
    public String getErrorLog() {
        final String errorLog = readLogZipFile(ERROR_LOG_TXT_FILENAME);

        if (!Strings.isNullOrEmpty(errorLog)) {
            return errorLog;
        }

        return unzip(super.getErrorLog());
    }

    private String readLogZipFile(final String zipFilename) {
        try {
            if (getLogFile() != null) {
                final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(getLogFile().getContent()));

                ZipEntry zipEntry = null;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (!zipFilename.equals(zipEntry.getName())) {
                        continue;
                    }

                    return new String(IOUtils.toByteArray(zis), "UTF-8");
                }
            }
        } catch (IOException e) {
        }

        return null;
    }

}
