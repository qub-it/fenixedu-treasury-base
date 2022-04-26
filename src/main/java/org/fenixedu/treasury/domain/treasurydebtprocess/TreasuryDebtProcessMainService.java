package org.fenixedu.treasury.domain.treasurydebtprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;

public class TreasuryDebtProcessMainService {

    private static final List<ITreasuryDebtProcessService> services = new ArrayList<>();

    public synchronized static void registerService(ITreasuryDebtProcessService service) {
        if (!services.stream().anyMatch(s -> s.getClass().equals(service.getClass()))) {
            services.add(service);
        }
    }

    public synchronized static void removeService(Class<ITreasuryDebtProcessService> clazz) {
        Optional<ITreasuryDebtProcessService> optional = services.stream().filter(s -> s.getClass().equals(clazz)).findAny();

        if (optional.isPresent()) {
            services.remove(optional.get());
        }
    }

    static Set<ITreasuryDebtProcess> getDebtProcesses(InvoiceEntry invoiceEntry) {
        return Collections.emptySet();
    }

    static boolean isBlockingPaymentInFrontend(InvoiceEntry invoiceEntry) {
        for (ITreasuryDebtProcessService service : services) {
            if (service.isBlockingPaymentInFrontend(invoiceEntry)) {
                return true;
            }
        }

        return false;
    }

    static boolean isBlockingPaymentInBackoffice(InvoiceEntry invoiceEntry) {
        for (ITreasuryDebtProcessService service : services) {
            if (service.isBlockingPaymentInBackoffice(invoiceEntry)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockingPayment(InvoiceEntry invoiceEntry, InvoiceEntryBlockingPaymentContext context) {
        if (context == InvoiceEntryBlockingPaymentContext.FRONTEND) {
            return isBlockingPaymentInFrontend(invoiceEntry);
        } else {
            return isBlockingPaymentInBackoffice(invoiceEntry);
        }
    }

    public static boolean isBlockingPayment(Installment installment, InvoiceEntryBlockingPaymentContext context) {
        return installment.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry()).filter(d -> d.isInDebt())
                .anyMatch(d -> isBlockingPayment(d, context));
    }

    static List<LocalizedString> getBlockingPaymentReasonsForFrontend(InvoiceEntry invoiceEntry) {
        List<LocalizedString> result = new ArrayList<>();

        for (ITreasuryDebtProcessService service : services) {
            if (service.getBlockingPaymentReasonForFrontend(invoiceEntry) != null) {
                result.add(service.getBlockingPaymentReasonForFrontend(invoiceEntry));
            }
        }

        return result;
    }

    static List<LocalizedString> getBlockingPaymentReasonsForBackoffice(InvoiceEntry invoiceEntry) {
        List<LocalizedString> result = new ArrayList<>();

        for (ITreasuryDebtProcessService service : services) {
            if (service.getBlockingPaymentReasonForBackoffice(invoiceEntry) != null) {
                result.add(service.getBlockingPaymentReasonForBackoffice(invoiceEntry));
            }
        }

        return result;
    }

}
