package org.fenixedu.treasury.services.integration;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.bennu.scheduler.TaskRunner;
import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.TreasuryFile;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineWebServiceResponse;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.IERPExporter;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.fenixedu.treasury.services.integration.erp.tasks.ERPExportSingleDocumentsTask;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Address;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Buyer;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Details;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.DoWebPaymentRequest;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.DoWebPaymentResponse;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.GetWebPaymentDetailsRequest;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.GetWebPaymentDetailsResponse;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Order;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.OrderDetail;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Payment;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Strings;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceAuthenticationLevel;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceClientConfiguration;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceConfiguration;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainObject;

public class FenixEDUTreasuryPlatformDependentServices implements ITreasuryPlatformDependentServices {

    private static final int WAIT_TRANSACTION_TO_FINISH_MS = 500;

    @Override
    public IERPExternalService getERPExternalServiceImplementation(final ERPConfiguration erpConfiguration) {
        final String className = erpConfiguration.getImplementationClassName();

        try {

            //force the "invocation" of class name
            Class cl = Class.forName(className);
            WebServiceClientConfiguration clientConfiguration = WebServiceConfiguration.readByImplementationClass(className);

            IERPExternalService client = clientConfiguration.getClient();

            return client;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TreasuryDomainException("error.ERPConfiguration.invalid.external.service");
        }
    }

    /* File */

    @Override
    public byte[] getFileContent(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFileSize(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getSize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime getFileCreationDate(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getCreationDate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilename(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getFilename();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFileStream(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileContentType(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getContentType();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createFile(final IGenericFile genericFile, final String fileName, final String contentType,
            final byte[] content) {
        try {
            GenericFile file = TreasuryFile.create(fileName, contentType, content);

            PropertyUtils.setProperty(genericFile, "treasuryFile", file);
            genericFile.setFileId(file.getExternalId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void deleteFile(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            file.delete();
            PropertyUtils.setProperty(genericFile, "treasuryFile", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /* User */

    @Override
    public String getLoggedUsername() {
        if (Authenticate.getUser() == null) {
            return null;
        }

        return Authenticate.getUser().getUsername();
    }

    @Override
    public String getCustomerEmail(Customer customer) {
        return null;
    }

    @Override
    public void setCurrentApplicationUser(String username) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void removeCurrentApplicationUser() {
        throw new RuntimeException("not supported");
    }

    /* Locales */

    @Override
    public Locale defaultLocale() {
        return Locale.forLanguageTag(CoreConfiguration.getConfiguration().defaultLocale());
    }

    @Override
    public Locale currentLocale() {
        return I18N.getLocale();
    }

    @Override
    public Set<Locale> availableLocales() {
        return CoreConfiguration.supportedLocales();
    }

    /* Bundles */

    @Override
    public String bundle(final String bundleName, final String key, final String... args) {
        return BundleUtil.getString(bundleName, key, args);
    }

    @Override
    public String bundle(final Locale locale, final String bundleName, final String key, final String... args) {
        return BundleUtil.getString(bundleName, locale, key, args);
    }

    @Override
    public LocalizedString bundleI18N(final String bundleName, final String key, final String... args) {
        return BundleUtil.getLocalizedString(bundleName, key, args);
    }

    @Override
    public <T> String versioningCreatorUsername(T obj) {
        return readVersioningCreatorUsername(obj);
    }

    @Override
    public <T> DateTime versioningCreationDate(T obj) {
        return readVersioningCreationDate(obj);
    }

    @Override
    public <T> String versioningUpdatorUsername(T obj) {
        return readVersioningUpdatorUsername(obj);
    }

    @Override
    public <T> DateTime versioningUpdateDate(T obj) {
        return readVersioningUpdateDate(obj);
    }

    public static <T> String readVersioningCreatorUsername(T obj) {
        try {
            String username = (String) PropertyUtils.getProperty(obj, "versioningCreator");

            return username;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> DateTime readVersioningCreationDate(T obj) {
        try {
            DateTime creationDate = (DateTime) PropertyUtils.getProperty(obj, "versioningCreationDate");

            return creationDate;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String readVersioningUpdatorUsername(T obj) {
        try {
            Object versioningUpdatedBy = PropertyUtils.getProperty(obj, "versioningUpdatedBy");

            if (versioningUpdatedBy == null) {
                return null;
            }

            return (String) PropertyUtils.getProperty(versioningUpdatedBy, "username");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> DateTime readVersioningUpdateDate(T obj) {
        try {
            Object versioningUpdateDate = PropertyUtils.getProperty(obj, "versioningUpdateDate");

            if (versioningUpdateDate == null) {
                return null;
            }

            return (DateTime) PropertyUtils.getProperty(versioningUpdateDate, "date");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public byte[] getFileContent(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileContent(String): not supported");
    }

    @Override
    public long getFileSize(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileSize(String): not supported");
    }

    @Override
    public String getFilename(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFilename(String): not supported");
    }

    @Override
    public InputStream getFileStream(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileStream(String): not supported");
    }

    @Override
    public DateTime getFileCreationDate(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileCreationDate(String): not supported");
    }

    @Override
    public String getFileContentType(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileContentType(String): not supported");
    }

    @Override
    public String createFile(String fileName, String contentType, byte[] content) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.createFile(): not supported");
    }

    @Override
    public void deleteFile(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.deleteFile(String): not supported");
    }

    /* ERP Integration */
    // TODO: Check the difference between this method and scheduleSingleDocument
    @Override
    public void scheduleDocumentForExportation(final FinantialDocument finantialDocument) {
        FinantialInstitution finantialInstitution = finantialDocument.getDebtAccount().getFinantialInstitution();

        if (finantialInstitution.getErpIntegrationConfiguration() == null) {
            return;
        }

        if (StringUtils.isEmpty(finantialInstitution.getErpIntegrationConfiguration().getImplementationClassName())) {
            return;
        }

        IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        if (erpExporter == null) {
            return;
        }

        final List<FinantialDocument> documentsToExport =
                erpExporter.filterDocumentsToExport(Collections.singletonList(finantialDocument).stream());

        if (documentsToExport.isEmpty()) {
            return;
        }

        final String externalId = documentsToExport.iterator().next().getExternalId();

        new Thread() {

            @Override
            @Atomic
            public void run() {
                try {
                    Thread.sleep(WAIT_TRANSACTION_TO_FINISH_MS);
                } catch (InterruptedException e) {
                }

                SchedulerSystem.queue(new TaskRunner(new ERPExportSingleDocumentsTask(externalId)));
            };

        }.start();
    }

    @Override
    public PaylineWebServiceResponse paylineGetWebPaymentDetails(ForwardPaymentRequest forwardPaymentRequest) {
        final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

        GetWebPaymentDetailsRequest request = new GetWebPaymentDetailsRequest();
        request.setToken(forwardPaymentRequest.getCheckoutId());

        GetWebPaymentDetailsResponse response = new PaylineWebServiceClient().getClient().getWebPaymentDetails(request);

        PaylineWebServiceResponse result = new PaylineWebServiceResponse();

        if (response.getResult() != null) {
            result.setResultCode(response.getResult().getCode());
            result.setResultLongMessage(response.getResult().getLongMessage());
        }

        if (response.getAuthorization() != null) {
            result.setAuthorizationNumber(response.getAuthorization().getNumber());
        }

        if (response.getAuthorization() != null && !Strings.isNullOrEmpty(response.getAuthorization().getDate())) {
            result.setAuthorizationDate(DATE_TIME_PATTERN.parseDateTime(response.getAuthorization().getDate()));
        }

        if (response.getPayment() != null && !Strings.isNullOrEmpty(response.getPayment().getAmount())) {
            result.setPaymentAmount(new BigDecimal(response.getPayment().getAmount()).divide(new BigDecimal("100")));
        }

        if (response.getTransaction() != null) {
            result.setTransactionId(response.getTransaction().getId());
        }

        if (response.getTransaction() != null && !Strings.isNullOrEmpty(response.getTransaction().getDate())) {
            result.setTransactionDate(DATE_TIME_PATTERN.parseDateTime(response.getTransaction().getDate()));
        }

        result.setJsonRequest(TreasuryConstants.json(request));
        result.setJsonResponse(TreasuryConstants.json(response));

        return result;
    }

    @Override
    public PaylineWebServiceResponse paylineDoWebPayment(ForwardPaymentRequest forwardPaymentRequest, String returnUrl,
            String cancelUrl) {
        final int PAYLINE_MAX_PHONE_SIZE = 14;

        final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

        final String SECURITY_MODE = "SSL";
        final String PT = "PT";
        final String EURO_CURRENCY = "978";
        final String ACTION_AUTHORIZATION_AND_VALIDATION = "101";
        final String MODE_CPT = "CPT";
        final String TRANSACTION_APPROVED_CODE = "00000";
        final String TRANSACTION_PENDING_FORM_FILL = "02306";

        final String ACTION_RETURN_URL = "return";
        final String ACTION_CANCEL_URL = "cancel";
        final String LANG_PT = "pt";
        final String LANG_EN = "en";

        String formattedAmount = forwardPaymentRequest.getPayableAmount().multiply(new BigDecimal(100))
                .setScale(0, RoundingMode.HALF_EVEN).toString();

        final Payment paymentDetails = new Payment();
        paymentDetails.setAmount(formattedAmount);
        paymentDetails.setCurrency(EURO_CURRENCY);
        paymentDetails.setAction(ACTION_AUTHORIZATION_AND_VALIDATION);
        paymentDetails.setMode(MODE_CPT);

        paymentDetails.setContractNumber(
                ((PaylineConfiguration) forwardPaymentRequest.getDigitalPaymentPlatform()).getPaylineContractNumber());

        final Order order = new Order();
        order.setRef(String.valueOf(forwardPaymentRequest.getOrderNumber()));
        order.setAmount(formattedAmount);
        order.setCurrency(EURO_CURRENCY);
        order.setDate(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(forwardPaymentRequest)
                .toString("dd/MM/yyyy HH:mm"));
        order.setCountry(PT);

        // fillOrderDetails(forwardPayment, order);

        final Customer customer = forwardPaymentRequest.getDebtAccount().getCustomer();

        final Buyer buyerDetails = new Buyer();
        buyerDetails.setFirstName(customer.getFirstNames());
        buyerDetails.setLastName(customer.getLastNames());

        if (!Strings.isNullOrEmpty(customer.getEmail()) && EmailValidator.getInstance().isValid(customer.getEmail())) {
            buyerDetails.setEmail(customer.getEmail());
        }

        if (!Strings.isNullOrEmpty(customer.getPhoneNumber())) {
            String phone = customer.getPhoneNumber().replaceAll("[^\\d]", "");

            if (phone.length() > PAYLINE_MAX_PHONE_SIZE) {
                phone = phone.substring(0, PAYLINE_MAX_PHONE_SIZE);
            }

            buyerDetails.setMobilePhone(phone);

        }

        // fillAddress(customer, buyerDetails);

        final DoWebPaymentRequest request = new DoWebPaymentRequest();

        request.setPayment(paymentDetails);
        request.setOrder(order);
        request.setReturnURL(returnUrl);
        request.setCancelURL(cancelUrl);

        final String languageToUse = "en".equals(I18N.getLocale().getLanguage()) ? LANG_EN : LANG_PT;
        request.setLanguageCode(languageToUse);

        request.setBuyer(buyerDetails);
        request.setSecurityMode(SECURITY_MODE);

        final DoWebPaymentResponse response = new PaylineWebServiceClient().getClient().doWebPayment(request);

        PaylineWebServiceResponse result = new PaylineWebServiceResponse();

        if (response.getResult() != null) {
            result.setResultCode(response.getResult().getCode());
            result.setResultLongMessage(response.getResult().getLongMessage());
        }

        result.setToken(response.getToken());
        result.setRedirectURL(response.getRedirectURL());

        result.setJsonRequest(TreasuryConstants.json(request));
        result.setJsonResponse(TreasuryConstants.json(response));

        return result;
    }

    private void fillAddress(final Customer customer, final Buyer buyerDetails) {
        final Address address = new Address();
        address.setStreet1(customer.getAddress());
        address.setZipCode(customer.getZipCode());
        address.setCountry(customer.getAddressCountryCode());
        buyerDetails.setShippingAdress(address);
    }

    private void fillOrderDetails(final ForwardPaymentRequest forwardPayment, final Order order) {
        final Currency currency = forwardPayment.getDebtAccount().getFinantialInstitution().getCurrency();
        final Details details = new Details();
        for (final DebitEntry debitEntry : forwardPayment.getDebitEntriesSet()) {
            final OrderDetail orderDetail = new OrderDetail();
            orderDetail.setRef(debitEntry.getExternalId());
            orderDetail.setPrice(Currency.getValueWithScale(debitEntry.getOpenAmount()).multiply(new BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_EVEN).toString());
            orderDetail.setQuantity("1");
            orderDetail.setComment(debitEntry.getDescription());
            details.getDetails().add(orderDetail);
        }

        order.setDetails(details);
    }

    @Override
    public void paylineConfigureWebservice(PaylineConfiguration paylineConfiguration) {
        if (WebServiceConfiguration.readByImplementationClass(
                "org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient") == null) {
            new WebServiceClientConfiguration(
                    "org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient");
        }

        WebServiceClientConfiguration configuration = WebServiceConfiguration.readByImplementationClass(
                "org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient");

        configuration.setAuthenticationLevel(WebServiceAuthenticationLevel.BASIC_AUTH);
        configuration.setUrl(paylineConfiguration.getPaymentURL());
        configuration.setClientUsername(paylineConfiguration.getPaylineMerchantId());
        configuration.setClientPassword(paylineConfiguration.getPaylineMerchantAccessKey());
    }

    /* Web */

    @Override
    public String calculateURLChecksum(String urlToChecksum, HttpSession session) {
        return GenericChecksumRewriter.calculateChecksum(urlToChecksum, session);
    }

    @Override
    public String getForwardPaymentURL(String contextPath, Class screenClass, boolean isSuccess, String forwardPaymentId,
            boolean isException) {
        throw new RuntimeException("not supported");
    }

    /* Domain entities events */

    @Override
    public void signalsRegisterHandlerForKey(String signalKey, Object handler) {
        Signal.register(signalKey, handler);
    }

    @Override
    public void signalsUnregisterHandlerForKey(String signalKey, Object handler) {
        Signal.unregister(signalKey, handler);
    }

    @Override
    public void signalsEmitForObject(String signalKey, DomainObject obj) {
        Signal.emit(signalKey, new DomainObjectEvent<DomainObject>(obj));
    }

    @Override
    public InputStream exportDocuments(String templateCode, FinantialInstitution finantialInstitution,
            FinantialEntity finantialEntity, LocalDate documentDateFrom, LocalDate documentDateTo, String username) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String exportDocumentFileExtension() {
        throw new RuntimeException("not supported");
    }

    @Override
    public InputStream exportPaymentReceipt(String templateCode, SettlementNote settlementNote) {
        throw new RuntimeException("not supported");
    }

    @Override
    public ISaftExporterConfiguration getSaftExporterConfiguration(ERPConfiguration configuration) {
        return null;
    }

    @Override
    public Set<Partial> getHolidays() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void certifyDocument(FinantialDocument finantialDocument) {
        // Do nothing
    }

    @Override
    public void updateCertifiedDocument(FinantialDocument finantialDocument) {
        // Do nothing
    }

    @Override
    public void annulCertifiedDocument(FinantialDocument finantialDocument) {
        // Do nothing
    }

    @Override
    public boolean hasCertifiedDocument(FinantialDocument finantialDocument) {
        return false;
    }

    @Override
    public boolean isProductCertified(Product product) {
        return false;
    }

    @Override
    public String getCertifiedDocumentNumber(FinantialDocument finantialDocument) {
        return null;
    }

    @Override
    public LocalDate getCertifiedDocumentDate(FinantialDocument finantialDocument) {
        return null;
    }

    /* Development or quality mode */

    @Override
    public boolean isQualityOrDevelopmentMode() {
        return CoreConfiguration.getConfiguration().developmentMode() || isQualityMode();
    }

    private boolean isQualityMode() {
        try {
            final Properties properties = new Properties();
            properties.load(this.getClass().getResourceAsStream("/configuration.properties"));
            return Boolean.valueOf(properties.getProperty("quality.mode", "true"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
