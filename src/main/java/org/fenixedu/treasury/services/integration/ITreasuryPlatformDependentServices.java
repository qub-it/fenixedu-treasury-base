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
package org.fenixedu.treasury.services.integration;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineWebServiceResponse;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;

import pt.ist.fenixframework.DomainObject;

public interface ITreasuryPlatformDependentServices {

    /* ERP Integration */
    void scheduleDocumentForExportation(final FinantialDocument finantialDocument);

    IERPExternalService getERPExternalServiceImplementation(final ERPConfiguration erpConfiguration);

    /* File */

    byte[] getFileContent(IGenericFile genericFile);

    long getFileSize(IGenericFile genericFile);

    String getFilename(IGenericFile genericFile);

    InputStream getFileStream(IGenericFile genericFile);

    DateTime getFileCreationDate(IGenericFile genericFile);

    String getFileContentType(IGenericFile iGenericFile);

    void createFile(final IGenericFile genericFile, final String fileName, final String contentType, final byte[] content);

    void deleteFile(final IGenericFile genericFile);

    /* File */

    byte[] getFileContent(String fileId);

    long getFileSize(String fileId);

    String getFilename(String fileId);

    InputStream getFileStream(String fileId);

    DateTime getFileCreationDate(String fileId);

    String getFileContentType(String fileId);

    String createFile(String fileName, String contentType, byte[] content);

    void deleteFile(String fileId);

    /* User */

    String getLoggedUsername();

    String getCustomerEmail(Customer customer);

    /* Locales */

    // TODO: provide the default locale of the platform
    Locale defaultLocale();

    Locale currentLocale();

    Set<Locale> availableLocales();

    /* Bundles */

    String bundle(final String bundleName, final String key, final String... args);

    String bundle(final Locale locale, final String bundleName, final String key, final String... args);

    LocalizedString bundleI18N(final String bundleName, final String key, final String... args);

    /* Versioning Information */

    <T> String versioningCreatorUsername(final T obj);

    <T> DateTime versioningCreationDate(final T obj);

    <T> String versioningUpdatorUsername(final T obj);

    <T> DateTime versioningUpdateDate(final T obj);

    /* Web Services */

    PaylineWebServiceResponse paylineGetWebPaymentDetails(ForwardPaymentRequest forwardPaymentRequest);

    PaylineWebServiceResponse paylineDoWebPayment(ForwardPaymentRequest forwardPaymentRequest, String returnUrl, String cancelUrl);

    void paylineConfigureWebservice(PaylineConfiguration paylineConfiguration);

    /* Web */
    String calculateURLChecksum(String urlToChecksum, HttpSession session);

    /* Domain entities events */
    void signalsRegisterHandlerForKey(String signalKey, Object handler);

    void signalsUnregisterHandlerForKey(String signalKey, Object handler);

    void signalsEmitForObject(String signalKey, DomainObject obj);

    String getForwardPaymentURL(String contextPath, Class screenClass, boolean isSuccess, String forwardPaymentId,
            boolean isException);

    /* Web Docs */
    
    InputStream exportDocuments(String templateCode, FinantialInstitution finantialInstitution, LocalDate documentDateFrom,
            LocalDate documentDateTo);
    
    String exportDocumentFileExtension();

    InputStream exportPaymentReceipt(String templateCode, SettlementNote settlementNote);

    ISaftExporterConfiguration getSaftExporterConfiguration(ERPConfiguration erpConfiguration);

    /* Holidays */

    Set<Partial> getHolidays();

    /* Tax Authority Certification */

    void certifyDocument(FinantialDocument finantialDocument);

    void updateCertifiedDocument(FinantialDocument finantialDocument);
    
    void annulCertifiedDocument(FinantialDocument finantialDocument);
    
    boolean hasCertifiedDocument(FinantialDocument finantialDocument);
    
    boolean isProductCertified(Product product);

    /* Development or quality mode */
    
    boolean isQualityOrDevelopmentMode();
}
