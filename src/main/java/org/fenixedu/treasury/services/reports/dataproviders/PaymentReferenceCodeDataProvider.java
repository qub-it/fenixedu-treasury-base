package org.fenixedu.treasury.services.reports.dataproviders;


import java.math.BigDecimal;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentEntry;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.joda.time.LocalDate;

import com.qubit.terra.docs.util.IDocumentFieldsData;
import com.qubit.terra.docs.util.IReportDataProvider;

public class PaymentReferenceCodeDataProvider extends AbstractDataProvider implements IReportDataProvider {

    protected static final String PAYMENT_CODE_KEY = "paymentCode";

    private SibsPaymentRequest paymentCode;

    public PaymentReferenceCodeDataProvider(final SibsPaymentRequest paymentCode) {
        this.setPaymentCode(paymentCode);
        registerKey(PAYMENT_CODE_KEY, PaymentReferenceCodeDataProvider::handlePaymentCodeKey);
    }

    private static Object handlePaymentCodeKey(IReportDataProvider provider) {
        PaymentReferenceCodeDataProvider regisProvider = (PaymentReferenceCodeDataProvider) provider;
        return regisProvider.getPaymentCode();
    }

    @Override
    public void registerFieldsAndImages(IDocumentFieldsData arg0) {
    }

    public SibsPaymentRequest getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(SibsPaymentRequest paymentCode) {
        this.paymentCode = paymentCode;
    }

    public String getDescription() {
        final StringBuilder builder = new StringBuilder();

        for (FinantialDocumentEntry entry : this.paymentCode.getOrderedDebitEntries()) {
            builder.append(((DebitEntry) entry).getProduct().getName().getContent()).append("\n");
        }

        return builder.toString();
    }

    public BigDecimal getAmount() {
        return paymentCode.getPayableAmount();
    }

    public LocalDate getDueDate() {
        return this.paymentCode.getDueDate();
    }

}