package org.fenixedu.treasury.services.reports.dataproviders;

import org.fenixedu.treasury.domain.FinantialInstitution;

import com.qubit.terra.docs.util.IDocumentFieldsData;
import com.qubit.terra.docs.util.IReportDataProvider;

public class FinantialInstitutionDataProvider extends AbstractDataProvider implements IReportDataProvider {

    protected static final String FINANTIAL_INSTITUTION_KEY = "finantialInstitution";
    protected static final String FINANTIAL_INSTITUTION_LOGO_KEY = "finantialInstitutionLogo";

    private FinantialInstitution finantialInstitution;

    public FinantialInstitutionDataProvider(final FinantialInstitution finantialInstitution) {
        this.finantialInstitution = finantialInstitution;
        registerKey(FINANTIAL_INSTITUTION_KEY, FinantialInstitutionDataProvider::handleFinantialInstitutionKey);
    }

    private static Object handleFinantialInstitutionKey(IReportDataProvider provider) {
        FinantialInstitutionDataProvider invoiceProvider = (FinantialInstitutionDataProvider) provider;
        return invoiceProvider.finantialInstitution;
    }

    @Override
    public void registerFieldsAndImages(IDocumentFieldsData arg0) {
        // TODO
        // arg0.registerImage(FINANTIAL_INSTITUTION_LOGO_KEY, Bennu.getInstance().getConfiguration().getLogo());
    }

}
