package org.fenixedu.treasury.services.reports.dataproviders;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;

import com.qubit.terra.docs.util.IDocumentFieldsData;
import com.qubit.terra.docs.util.IReportDataProvider;

public class DebtAccountDataProvider extends AbstractDataProvider implements IReportDataProvider {

    protected static final String DEBT_ACCOUNT_KEY = "debtAccount";
    protected static final String PAYMENT_LINES_KEY = "paymentLines";

    private DebtAccount debtAccount;

    public DebtAccountDataProvider(final DebtAccount debtAccount) {
        this(debtAccount, null);
    }

    public DebtAccountDataProvider(final DebtAccount debtAccount, final List<DebitNote> debitNotesForPaymentLines) {
        this.debtAccount = debtAccount;
        registerKey(DEBT_ACCOUNT_KEY, DebtAccountDataProvider::handleDebtAccountKey);
        registerKey(PAYMENT_LINES_KEY, DebtAccountDataProvider::handlePaymentsLinesKey);
    }

    private static Object handlePaymentsLinesKey(IReportDataProvider provider) {

        DebtAccountDataProvider debtProvider = (DebtAccountDataProvider) provider;

        List<PaymentReferenceCodeDataProvider> codesProviders = new ArrayList<PaymentReferenceCodeDataProvider>();
        for (SibsPaymentRequest code : debtProvider.debtAccount.getActiveSibsPaymentRequestsOfPendingDebitEntries()) {
            codesProviders.add(new PaymentReferenceCodeDataProvider(code));
        }
        
        return codesProviders.stream().sorted((x, y) -> x.getDueDate().compareTo(y.getDueDate())).collect(Collectors.toList());
    }

    private static Object handleDebtAccountKey(IReportDataProvider provider) {
        DebtAccountDataProvider invoiceProvider = (DebtAccountDataProvider) provider;
        return invoiceProvider.debtAccount;
    }

    @Override
    public void registerFieldsAndImages(IDocumentFieldsData arg0) {
        arg0.registerCollectionAsField(PAYMENT_LINES_KEY);

    }
}
