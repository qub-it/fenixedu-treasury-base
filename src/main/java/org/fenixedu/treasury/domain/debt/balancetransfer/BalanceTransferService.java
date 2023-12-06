package org.fenixedu.treasury.domain.debt.balancetransfer;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;

public interface BalanceTransferService {

    public boolean isAutoTransferInSwitchDebtAccountsEnabled();

    public void transferBalance();

    public static BalanceTransferService getService(DebtAccount fromDebtAccount, DebtAccount destinyDebtAccount) {
        try {
            // Ensure both debts accounts are from the same finantial institution
            if (fromDebtAccount.getFinantialInstitution() != destinyDebtAccount.getFinantialInstitution()) {
                throw new IllegalArgumentException(
                        "error.BalanceTransferService.finantialInstitution.from.both.debtAccounts.differ");
            }

            FinantialInstitution finantialInstitution = fromDebtAccount.getFinantialInstitution();

            Class<? extends BalanceTransferService> clazz = (Class<? extends BalanceTransferService>) Class
                    .forName(finantialInstitution.getBalanceTransferServiceImplementationClass());

            return clazz.getConstructor(DebtAccount.class, DebtAccount.class).newInstance(fromDebtAccount, destinyDebtAccount);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
