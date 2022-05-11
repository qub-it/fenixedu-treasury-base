package org.fenixedu.treasury.domain.treasurydebtprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

    public static Set<? extends ITreasuryDebtProcess> getDebtProcesses(InvoiceEntry invoiceEntry) {
        Set<ITreasuryDebtProcess> result = new HashSet<>();

        services.stream().forEach(service -> result.addAll(service.getDebtProcesses(invoiceEntry)));

        return result;
    }

    public static boolean isBlockingPaymentInFrontend(InvoiceEntry invoiceEntry) {
        for (ITreasuryDebtProcessService service : services) {
            if (service.isBlockingPaymentInFrontend(invoiceEntry)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockingPaymentInBackoffice(InvoiceEntry invoiceEntry) {
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

    public static List<LocalizedString> getBlockingPaymentReasonsForFrontend(InvoiceEntry invoiceEntry) {
        List<LocalizedString> result = new ArrayList<>();

        for (ITreasuryDebtProcessService service : services) {
            if (service.getBlockingPaymentReasonForFrontend(invoiceEntry) != null) {
                result.add(service.getBlockingPaymentReasonForFrontend(invoiceEntry));
            }
        }

        return result;
    }

    public static List<LocalizedString> getBlockingPaymentReasonsForBackoffice(InvoiceEntry invoiceEntry) {
        List<LocalizedString> result = new ArrayList<>();

        for (ITreasuryDebtProcessService service : services) {
            if (service.getBlockingPaymentReasonForBackoffice(invoiceEntry) != null) {
                result.add(service.getBlockingPaymentReasonForBackoffice(invoiceEntry));
            }
        }

        return result;
    }

    public static boolean isInterestCreationWhenTotalSettledPrevented(InvoiceEntry invoiceEntry) {
        for (ITreasuryDebtProcessService service : services) {
            if (service.isInterestCreationWhenTotalSettledPrevented(invoiceEntry)) {
                return true;
            }
        }

        return false;
    }
    
}
