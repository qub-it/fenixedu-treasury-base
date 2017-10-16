package org.fenixedu.treasury.services.payments.sibs;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.SibsReportFile;
import org.fenixedu.treasury.domain.paymentcodes.SibsTransactionDetail;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.services.payments.sibs.SIBSPaymentsImporter.ProcessResult;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileFooter;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileHeader;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;

public class SibsPaymentsBrokerService {

    //@formatter:off
/*
 * 
E034        - A036 (Código da Mensagem)
01      - A037 (Id. Sistema)
01      - A038 (Versão da mensagem)
0254        - A008 (Identificação Log SIBS)
00169930    - A014 (Nr. Log SIBS)
20647       - A033 (Nr. Entidade)
264775209   - A034 (Referência do Pagamento)
0000025000  - A011 (Montante Pago)
978     - A006 (Código de moeda)
201604050827    - A035 (Data /home transacção cliente)
01      - A015 (Tipo de terminal)
0010003504  - A016 (Identificação do terminal)
04117       - A017 (Identificação da transacção local)
Cartaxo         - A030 (Localidade do terminal)
000000000   - Número de contribuinte
00000000    - Número da factura
\ 

 */
    //@formatter:on

    public static final String ERROR_SIBS_PAYMENTS_BROKER_SERVICE_NO_PAYMENTS_TO_IMPORT = "error.SibsPaymentsBrokerService.no.payments.to.import";
    private static final String PAY_PREAMBLE = "E034";

    public static String getPaymentsFromBroker(final FinantialInstitution finantialInstitution, final LocalDate fromDate,
            final LocalDate toDate, final boolean removeInexistentReferenceCodes, final boolean removeAlreadyProcessedCodes) {
        if (!isSibsPaymentsBrokerActive(finantialInstitution)) {
            throw new TreasuryDomainException("error.SibsPaymentsBrokerService.not.active");
        }

        final String url = finantialInstitution.getSibsConfiguration().getSibsPaymentsBrokerUrl();
        final String key = finantialInstitution.getSibsConfiguration().getSibsPaymentsBrokerSharedKey();
        final String dtInicio = fromDate.toString("yyyy-MM-dd");
        final String dtFim = toDate.toString("yyyy-MM-dd");

        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(url);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("secret", key);
        formData.add("dt_inicio", dtInicio);
        formData.add("dt_fim", dtFim);
        formData.add("v", "2");
        Response response = target.request().post(Entity.form(formData));

        String strResult = response.readEntity(String.class);
        return strResult;
    }

    public static SibsIncommingPaymentFile readPaymentsFromBroker(final FinantialInstitution finantialInstitution,
            final LocalDate fromDate, final LocalDate toDate, final boolean removeInexistentReferenceCodes,
            final boolean removeAlreadyProcessedCodes) {
        if (!isSibsPaymentsBrokerActive(finantialInstitution)) {
            throw new TreasuryDomainException("error.SibsPaymentsBrokerService.not.active");
        }

        String strResult = getPaymentsFromBroker(finantialInstitution, fromDate, toDate, removeInexistentReferenceCodes,
                removeAlreadyProcessedCodes);

        final SibsPayments sibsPayments = new GsonBuilder().create().fromJson(strResult, SibsPayments.class);

        if (sibsPayments == null) {
            throw new TreasuryDomainException("error.SibsPaymentsBrokerService.unable.parse.information");
        }

        if (!Strings.isNullOrEmpty(sibsPayments.error)) {
            throw new TreasuryDomainException("error.SibsPaymentsBrokerService.error.returned.by.broker", sibsPayments.error);
        }

        return parsePayments(finantialInstitution, sibsPayments, removeInexistentReferenceCodes, removeAlreadyProcessedCodes);
    }

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmm";
    private static final int[] FIELD_SIZES = new int[] { 4, 2, 2, 4, 8, 5, 9, 10, 3, 12, 2, 10, 5, 15, 9, 8 };
    private static final Integer DEFAULT_SIBS_VERSION = 1;

    private static SibsIncommingPaymentFile parsePayments(final FinantialInstitution finantialInstitution,
            final SibsPayments sibsPayments, boolean removeInexistentReferenceCodes, boolean removeAlreadyProcessedCodes) {

        final List<SibsIncommingPaymentFileDetailLine> detailLines = Lists.newArrayList();
        BigDecimal transactionsTotalAmount = BigDecimal.ZERO;
        for (final SibsPaymentEntry entry : sibsPayments.data) {

            final String referenceCode = getCodeFrom(entry);
            final String entityReferenceCode = getEntityCodeFrom(entry);
            final DateTime whenOccuredTransactionFrom = getWhenOccuredTransactionFrom(entry);

            if (!finantialInstitution.getSibsConfiguration().getEntityReferenceCode().equals(entityReferenceCode)) {
                continue;
            }

            if (removeInexistentReferenceCodes
                    && PaymentReferenceCode.findByReferenceCode(referenceCode, finantialInstitution).count() == 0) {
                continue;
            }

            SibsIncommingPaymentFileDetailLine line = new SibsIncommingPaymentFileDetailLine(whenOccuredTransactionFrom,
                    getAmountFrom(entry), getSibsTransactionIdFrom(entry), referenceCode);

            if (removeAlreadyProcessedCodes && SibsTransactionDetail.isReferenceProcessingDuplicate(referenceCode,
                    entityReferenceCode, whenOccuredTransactionFrom)) {
                continue;
            }

            detailLines.add(line);

            transactionsTotalAmount = transactionsTotalAmount.add(line.getAmount());
        }

        final SibsIncommingPaymentFileHeader header = new SibsIncommingPaymentFileHeader(new YearMonthDay(), DEFAULT_SIBS_VERSION,
                finantialInstitution.getSibsConfiguration().getEntityReferenceCode());

        final SibsIncommingPaymentFileFooter footer =
                new SibsIncommingPaymentFileFooter(transactionsTotalAmount, BigDecimal.ZERO);

        if (detailLines.isEmpty()) {
            throw new TreasuryDomainException(ERROR_SIBS_PAYMENTS_BROKER_SERVICE_NO_PAYMENTS_TO_IMPORT);
        }

        return new SibsIncommingPaymentFile(String.format("SIBS_%s.inp", new DateTime().toString("yyyyMMddHHmmss")), header,
                footer, detailLines);
    }

    public static String getEntityCodeFrom(final SibsPaymentEntry entry) {
        return entry.entityReferenceCode;
    }

    private static String getCodeFrom(final SibsPaymentEntry entry) {
        return entry.referenceCode;
    }

    private static String getSibsTransactionIdFrom(final SibsPaymentEntry entry) {
        return entry.sibsTransactionId;
    }

    private static BigDecimal getAmountFrom(final SibsPaymentEntry entry) {
        return BigDecimal.valueOf(Double.parseDouble(entry.amount.substring(0, 8) + "." + entry.amount.substring(8)));
    }

    private static DateTime getWhenOccuredTransactionFrom(final SibsPaymentEntry entry) {
        try {
            return new DateTime(new SimpleDateFormat(DATE_TIME_FORMAT).parse(entry.whenOccuredTransaction));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final static String[] splitLine(final String line) {
        int lastIndex = 0;
        final String[] result = new String[FIELD_SIZES.length];
        for (int i = 0; i < FIELD_SIZES.length; i++) {
            result[i] = line.substring(lastIndex, lastIndex + FIELD_SIZES[i]);
            lastIndex += FIELD_SIZES[i];
        }
        return result;
    }

    public static boolean isSibsPaymentsBrokerActive(final FinantialInstitution finantialInstitution) {
        return !Strings.isNullOrEmpty(finantialInstitution.getSibsConfiguration().getSibsPaymentsBrokerUrl());
    }

    private static class SibsPaymentEntry {
        private String sibsTransactionId;
        private String entityReferenceCode;
        private String referenceCode;
        private String amount;
        private String whenOccuredTransaction;
    }

    private static class SibsPayments {
        private String error;
        private List<SibsPaymentEntry> data;
    }

    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    
    public void ProcessSibsPaymentsFromBroker(final PrintWriter writer) throws IOException {
        
        for(final FinantialInstitution finantialInstitution : FinantialInstitution.findAll().collect(Collectors.toSet())) {
            
            if(!finantialInstitution.getSibsConfiguration().isPaymentsBrokerActive()) {
                continue;
            }
            
            for(final PaymentCodePool paymentCodePool : finantialInstitution.getPaymentCodePoolsSet()) {
                try {
                    if(paymentCodePool.getActive() == null || !paymentCodePool.getActive()) {
                        continue;
                    }
                    
                    LocalDate now = new LocalDate();
                    
                    final SibsIncommingPaymentFile sibsFile =
                            SibsPaymentsBrokerService.readPaymentsFromBroker(paymentCodePool.getFinantialInstitution(), now.minusDays(1), now,
                                    true, true);
                    
                    if(sibsFile.getDetailLines().isEmpty()) {
                        continue;
                    }
                    
                    SIBSPaymentsImporter importer = new SIBSPaymentsImporter();
                    SibsReportFile reportFile = null;
                    
                    final ProcessResult result = importer.processSIBSPaymentFiles(sibsFile, paymentCodePool.getFinantialInstitution());
                    reportFile = result.getReportFile();
                    if (result.getReportFile() != null) {
                        reportFile.updateLogMessages(result);
                    }
                } catch(final TreasuryDomainException e) {
                    if(SibsPaymentsBrokerService.ERROR_SIBS_PAYMENTS_BROKER_SERVICE_NO_PAYMENTS_TO_IMPORT.equals(e.getMessage())) {
                        writer.format("No payments to register");
                        continue;
                    }
                    
                    throw new RuntimeException(e);
                }
            }
        }
    	
    }
    // @formatter:on
    
}
