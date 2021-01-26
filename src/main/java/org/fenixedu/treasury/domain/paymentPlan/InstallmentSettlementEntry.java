package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.FenixFramework;

public class InstallmentSettlementEntry extends InstallmentSettlementEntry_Base {

	public InstallmentSettlementEntry() {
		super();
		setDomainRoot(FenixFramework.getDomainRoot());
	}

	private InstallmentSettlementEntry(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
			BigDecimal debtAmount) {
		this();
		setSettlementEntry(settlementEntry);
		setInstallmentEntry(installmentEntry);
		setAmount(debtAmount);
		checkRules();
	}

	private void checkRules() {
		if (getSettlementEntry() == null) {
			throw new TreasuryDomainException("error.InstallmentSettlementEntry.settlementEntry.required");
		}
		if (getAmount() == null || !TreasuryConstants.isPositive(getAmount())) {
			throw new TreasuryDomainException("error.InstallmentSettlementEntry.amount.must.be.positive");
		}
		if (getInstallmentEntry() == null) {
			throw new TreasuryDomainException("error.InstallmentSettlementEntry.installmentEntry.required");
		}
	}

	public static InstallmentSettlementEntry create(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
			BigDecimal debtAmount) {
		return new InstallmentSettlementEntry(installmentEntry, settlementEntry, debtAmount);
	}
}
