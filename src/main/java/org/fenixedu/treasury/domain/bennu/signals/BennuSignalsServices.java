package org.fenixedu.treasury.domain.bennu.signals;

import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

public class BennuSignalsServices {
    
    public static final String SETTLEMENT_EVENT = "SETTLEMENT_EVENT";
    
    public static void emitSignalForSettlement(final SettlementNote settlementNote) {
        TreasuryPlataformDependentServicesFactory.implementation().signalsEmitForObject(SETTLEMENT_EVENT, settlementNote);
    }
    
    public synchronized static void registerSettlementEventHandler(final Object handler) {
        TreasuryPlataformDependentServicesFactory.implementation().signalsRegisterHandlerForKey(SETTLEMENT_EVENT, handler);
    }
    
    public synchronized static void unregisterSettlementEventHandler(final Object handler) {
        TreasuryPlataformDependentServicesFactory.implementation().signalsUnregisterHandlerForKey(SETTLEMENT_EVENT, handler);
    }
    
}
