package org.fenixedu.treasury.domain.paymentPlan;

import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

public class ConfiguracaoToDelete {
    public static boolean isToCalculateInterest = false;
    public static Product emulomentProduct = Product.findByCode("AVERBAMENTO").iterator().next();
    public static Product interestProduct = TreasurySettings.getInstance().getInterestProduct();

    public static boolean newItensInSameDebitNote = false;

    private static String installmentDescriptionFormat = "%dº prestação de %s";

    public static String getInstallmentDescription(int i, String description) {
        return String.format(installmentDescriptionFormat, i, description);
    }
}
