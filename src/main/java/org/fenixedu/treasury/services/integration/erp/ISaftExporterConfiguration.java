package org.fenixedu.treasury.services.integration.erp;

import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.FinantialDocument;

public interface ISaftExporterConfiguration {

    byte[] generateSaftForFinantialDocuments(Set<FinantialDocument> finantialDocuments, boolean formatted);

    byte[] generateSaftForCustomers(Set<? extends Customer> customers, boolean formatted);

    byte[] generateSaftForProducts(Set<Product> products, boolean formatted);

    String getEncoding();

}
