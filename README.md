[![qubIT](http://www.qub-it.com/cms/images/qubIT_logo_transparent_medium.png)](http://www.qub-it.com)

# fenixedu-treasury

FenixEDU Treasury is a comprehensive module for managing financial operations within the FenixEdu educational platform. It handles **Payments**, **Debt Accounts**, **Financial Institutions**, **Invoice Generation**, and **ERP Integration** for universities and educational institutions.

## Purpose

This module enables any FenixEDU module to:

- Create and manage **debits** (DebitEntry) and **credits** (CreditEntry) for customers
- Generate **financial documents** (DebitNote, CreditNote, SettlementNote, Invoice)
- Process **online payments** via SIBS, MB Way, and other payment gateways
- Integrate with **ERP systems** via SAFT-PT XML format
- Manage **payment plans** with installment-based payments
- Handle **VAT/tax calculations** and fiscal compliance

---

## YuML Domain Model

The following YuML format can be parsed programmatically:

```
[FinantialInstitution]1--*>[FinantialEntity|institution]
[FinantialInstitution]1--*>[DebtAccount|institution]
[FinantialInstitution]1--*>[Currency|currency]
[FinantialInstitution]1--*>[FiscalCountryRegion]
[FinantialInstitution]1--*>[Vat|vats]
[FinantialInstitution]1--*>[Series|series]
[FinantialInstitution]1--*>[Product|products]
[FinantialInstitution]1--*>[ProductGroup]
[FinantialEntity]1--*>[Tariff|tariffs]
[FinantialEntity]1--*>[FixedTariff]
[FinantialEntity]1--*>[Series|entitySeries]
[FinantialEntity]1--*>[TreasuryDocumentTemplate]
[Customer]1--*>[DebtAccount|accounts]
[Customer]0..1-->[CustomerType|type]
[DebtAccount]1--*>[FinantialDocument|documents]
[DebtAccount]1--*>[PaymentPlan|paymentPlans]
[DebtAccount]1--*>[TreasuryEvent|events]
[FinantialDocument]1--*>[FinantialDocumentEntry|entries]
[FinantialDocument]0..1-->[Series|series]
[FinantialDocument]0..1-->[FinantialEntity|entity]
[FinantialDocument]1-->[FinantialDocumentType|type]
[FinantialDocumentEntry]1--0..1>[Vat|vat]
[FinantialDocumentEntry]1--0..1>[Product|product]
[InvoiceEntry]->[DebitEntry|extends]
[InvoiceEntry]->[CreditEntry|extends]
[InvoiceEntry]|parent|-[FinantialDocumentEntry]
[DebitNote]^-[Invoice]
[CreditNote]^-[Invoice]
[Invoice]^-[FinantialDocument]
[SettlementNote]^-[FinantialDocument]
[AdvancedPaymentCreditNote]^-[CreditNote]
[PaymentEntry]1--*>[SettlementNote|settlements]
[PaymentEntry]1-->[PaymentMethod|paymentMethod]
[SettlementEntry]^-[FinantialDocumentEntry]
[ReimbursementEntry]1-->[SettlementNote]
[ReimbursementEntry]1-->[PaymentMethod]
[Tariff]1--*>[InterestRate|interestRates]
[Tariff]1-->[InterestRateType|interestRateType]
[Tariff]1-->[DueDateCalculationType]
[Tariff]<-[FixedTariff]
[InterestRate]1-->[InterestRateType]
[InterestRateType]^-[GlobalInterestRateType]
[InterestRateType]^-[FixedAmountInterestRateType]
[Vat]1-->[VatType|type]
[Vat]0..1-->[VatExemptionReason]
[Vat]0..1-->[FiscalYear]
[VatExemptionReason]*--[VatType]
[PaymentPlan]1--*>[Installment|installments]
[Installment]1--*>[InstallmentEntry]
[Installment]1--*>[InstallmentSettlementEntry]
[PaymentRequest]1--*>[PaymentInvoiceEntriesGroup|groups]
[PaymentRequest]1--*>[PaymentTransaction|transactions]
[PaymentRequest]1--*>[PaymentRequestLog|logs]
[PaymentCodeTarget]1--*>[SibsPaymentRequest]
[PaymentCodeTarget]1--*>[PaymentReferenceCode]
[TreasuryEvent]1--*>[TreasuryExemption|exemptions]
[TreasuryExemption]1-->[TreasuryExemptionType|type]
[TreasuryExemption]1--0..1>[Product]
[TreasuryExemption]1-->[DebitEntry|debitEntry]
[TreasuryExemption]1--0..1>[CreditEntry]
[CreditTreasuryExemption]^-[TreasuryExemption]
[Customer]^-[AdhocCustomer]
[FinantialInstitution]1--*>[ERPConfiguration]
[ERPConfiguration]1--*>[ERPExportOperation]
[ERPConfiguration]1--*>[ERPImportOperation]
[FinantialDocument]1--*>[ERPExportOperation|exports]
```

---

## Core Entities

### Financial Structure

| Entity                   | Description                               | Key Properties                                                                           |
| ------------------------ | ----------------------------------------- | ---------------------------------------------------------------------------------------- |
| **FinantialInstitution** | University/organization managing finances | code, fiscalNumber, companyId, name, address, currency, country, invoiceRegistrationMode |
| **FinantialEntity**      | Department/unit within institution        | code, name                                                                               |

### Customers & Accounts

| Entity                  | Description                              | Key Properties                                                                            |
| ----------------------- | ---------------------------------------- | ----------------------------------------------------------------------------------------- |
| **Customer** (abstract) | Base for all customers                   | code, name, fiscalNumber, addressCountryCode, businessIdentification, email, customerType |
| **AdhocCustomer**       | Non-person customer (e.g., organization) | fiscalNumber, identificationNumber, name                                                  |
| **DebtAccount**         | Account linking customer to institution  | closed, legacyCode                                                                        |
| **CustomerType**        | Customer category                        | code, name                                                                                |

### Documents

| Entity                           | Description               | Key Properties                                                |
| -------------------------------- | ------------------------- | ------------------------------------------------------------- |
| **FinantialDocument** (abstract) | Base financial document   | documentNumber, documentDate, documentDueDate, state, address |
| **Invoice**                      | Full invoice              | Inherits FinantialDocument                                    |
| **DebitNote**                    | Debt request document     | Inherits Invoice                                              |
| **CreditNote**                   | Credit/refund document    | reason, certificationOriginDocumentReference                  |
| **AdvancedPaymentCreditNote**    | Credit for excess payment | Inherits CreditNote                                           |
| **SettlementNote**               | Payment settlement        | paymentDate, usedInBalanceTransfer                            |

### Document Entries

| Entity                                | Description                | Key Properties                                         |
| ------------------------------------- | -------------------------- | ------------------------------------------------------ |
| **FinantialDocumentEntry** (abstract) | Base document line         | code, finantialEntryType, description, amount          |
| **InvoiceEntry**                      | Invoice line               | quantity, vatAmount, vatRate, netAmount, amountWithVat |
| **DebitEntry**                        | Debt line                  | dueDate, eventAnnuled, blockAcademicActsOnDebt         |
| **CreditEntry**                       | Credit line                | fromExemption                                          |
| **SettlementEntry**                   | Settlement line            | closeDate                                              |
| **PaymentEntry**                      | Payment against settlement | payedAmount, paymentMethodId                           |
| **ReimbursementEntry**                | Reimbursement line         | reimbursedAmount, reimbursementMethodId                |

### Payments

| Entity                 | Description              | Key Properties                                         |
| ---------------------- | ------------------------ | ------------------------------------------------------ |
| **PaymentMethod**      | Payment type             | code, name, availableForPaymentInApplication, saftCode |
| **PaymentRequest**     | Payment attempt          | amount, state                                          |
| **PaymentTransaction** | Completed payment        | transactionId, amount, date                            |
| **PaymentCodeTarget**  | Target for payment codes | -                                                      |
| **SibsPaymentRequest** | SIBS payment             | entityCode, reference, amount                          |

### Payment Plans

| Entity                         | Description        | Key Properties                    |
| ------------------------------ | ------------------ | --------------------------------- |
| **PaymentPlan**                | Installment plan   | totalAmount, numberOfInstallments |
| **Installment**                | Individual payment | amount, dueDate, state            |
| **InstallmentEntry**           | Pending entry      | -                                 |
| **InstallmentSettlementEntry** | Settled entry      | -                                 |

### Tariffs & Interest

| Entity                          | Description               | Key Properties                                                           |
| ------------------------------- | ------------------------- | ------------------------------------------------------------------------ |
| **Tariff** (abstract)           | Pricing rule              | beginDate, endDate, dueDateCalculationType, fixedDueDate, applyInterests |
| **FixedTariff**                 | Flat fee                  | amount                                                                   |
| **InterestRateType**            | Interest category         | code, description, allowsPeriodicInterestDebitEntryCreation              |
| **InterestRate**                | Interest calculation      | numberOfDaysAfterDueDate, rate, interestFixedAmount                      |
| **GlobalInterestRateType**      | System-wide interest type | Inherits InterestRateType                                                |
| **FixedAmountInterestRateType** | Fixed penalty type        | Inherits InterestRateType                                                |
| **InterestRateEntry**           | Interest rate record      | startDate, rate, penaltyFixedAmount                                      |

### Fiscal

| Entity                  | Description   | Key Properties                         |
| ----------------------- | ------------- | -------------------------------------- |
| **Vat**                 | VAT rate      | taxRate, beginDate, endDate            |
| **VatType**             | VAT category  | code, name, requiresVatExemptionReason |
| **VatExemptionReason**  | VAT exemption | code, name, legalArticle, active       |
| **FiscalYear**          | Fiscal year   | year, beginDate                        |
| **FiscalCountryRegion** | Fiscal region | fiscalCode, name                       |
| **Currency**            | Currency      | code, name, isoCode, symbol            |

### Products

| Entity           | Description      | Key Properties                                      |
| ---------------- | ---------------- | --------------------------------------------------- |
| **Product**      | Service/article  | code, name, active, legacy, applyInterestsByDefault |
| **ProductGroup** | Product category | code, name                                          |

### Events

| Entity            | Description     | Key Properties                       |
| ----------------- | --------------- | ------------------------------------ |
| **TreasuryEvent** | Financial event | code, description, propertiesJsonMap |

### Exemptions

| Entity                      | Description        | Key Properties                         |
| --------------------------- | ------------------ | -------------------------------------- |
| **TreasuryExemptionType**   | Exemption category | code, name, defaultExemptionPercentage |
| **TreasuryExemption**       | Exemption granted  | reason, valueToExempt                  |
| **CreditTreasuryExemption** | Credit exemption   | creditedNetExemptedAmount              |

### Access Control

| Entity                              | Description        | Key Properties |
| ----------------------------------- | ------------------ | -------------- |
| **TreasuryAccessControl**           | Access config      | -              |
| **PersistentTreasuryFrontOffice**   | Front office users | -              |
| **PersistentTreasuryBackOffice**    | Back office users  | -              |
| **PersistentTreasuryManagersGroup** | Managers           | -              |

### Integration

| Entity                 | Description   | Key Properties                                |
| ---------------------- | ------------- | --------------------------------------------- |
| **ERPConfiguration**   | ERP settings  | active, code, externalURL, username, password |
| **ERPExportOperation** | Export record | -                                             |
| **ERPImportOperation** | Import record | -                                             |

### Templates

| Entity                           | Description       | Key Properties               |
| -------------------------------- | ----------------- | ---------------------------- |
| **TreasuryDocumentTemplate**     | Document template | -                            |
| **TreasuryDocumentTemplateFile** | Template file     | fileId, active, creationDate |

---

## Enumerations

| Enum                              | Values                                                             |
| --------------------------------- | ------------------------------------------------------------------ |
| **FinantialDocumentTypeEnum**     | DEBIT_NOTE, CREDIT_NOTE, SETTLEMENT_NOTE, etc.                     |
| **FinantialEntryType**            | DEBIT, CREDIT, SETTLEMENT, PAYMENT, REIMBURSEMENT                  |
| **FinantialDocumentStateType**    | PREPARATION, CLOSED, ANNULLED                                      |
| **DueDateCalculationType**        | FIXED_DAY_OF_MONTH, DAYS_AFTER_CREATION, DAYS_AFTER_DUE_DATE, etc. |
| **InterestType**                  | MONTHLY, DAILY                                                     |
| **PaymentReferenceCodeStateType** | PENDING, PAID, CANCELLED, REJECTED                                 |
| **ForwardPaymentStateType**       | PENDING, SUCCESS, FAILURE                                          |
| **PaymentPlanStateType**          | ACTIVE, SETTLED, CANCELLED                                         |
| **InvoiceRegistrationMode**       | ERP_INTEGRATION, TREASURY_CERTIFICATION                            |
| **InvoiceSourceBillingType**      | STANDARD, TUITION_FEE, etc.                                        |
| **PaymentSourcePaymentType**      | CASH, BANK_TRANSFER, CARD, MULTIBANCO, etc.                        |

---

## Package Structure

```
org.fenixedu.treasury/
├── domain/
│   ├── debt/                    # DebtAccount, balance transfer
│   ├── document/                # FinantialDocument, entries
│   │   └── log/                 # Document logs
│   ├── event/                   # TreasuryEvent
│   ├── exemption/               # TreasuryExemption
│   ├── integration/             # ERP integration
│   ├── paymentPlan/             # PaymentPlan, Installment
│   │   └── paymentPlanValidator/
│   ├── payments/                # Payment requests
│   │   └── integration/
│   ├── paymentcodes/            # Payment references
│   ├── sibsonlinepaymentsgateway/
│   ├── sibspay/                 # MB Way
│   ├── sibspaymentsgateway/
│   ├── tariff/                  # Tariffs, Interest
│   └── forwardpayments/
├── dto/                        # Data transfer objects
│   ├── forwardpayments/
│   ├── meowallet/
│   └── PaymentPlans/
├── services/
│   ├── accesscontrol/
│   ├── groups/
│   └── integration/
└── util/

org.fenixedu.onlinepaymentsgateway/
├── api/
└── sibs/
    └── sdk/
```

---

## Integration Modules

- **ERP Integration** via SAFT-PT XML
- **SIBS Payments** integration via CheckDigit and CustomersFile
- **MB Way** payments
- **Meo Wallet** payments

---

## Usage Examples

### Creating a Debt (DebitEntry)

```java
FinantialInstitution institution = ...;
Customer customer = ...;
DebtAccount debtAccount = customer.getDebtAccountFor(institution);
FinantialEntity finantialEntity = ...;
Product product = ...;

DebitEntry.create(debtAccount, finantialEntity, product, 
    new BigDecimal("100.00"), "Tuition Fee");
```

### Recording a Payment

```java
SettlementNote settlement = SettlementNote.create(...);
settlement.addSettlementEntry(debitEntry, new BigDecimal("50.00"));
```

### Creating a Payment Plan

```java
PaymentPlan plan = PaymentPlan.create(debtAccount, entries, 
    PaymentPlanCalculateInstallmentsInterestsConfigurator.NULL_CONFIGURATOR);
```

### Applying an Exemption

```java
TreasuryExemptionType exemptionType = ...;
TreasuryExemption.create(debitEntry, exemptionType, 
    new BigDecimal("25.00"), "Discount description");
```

---

## Key Concepts (from Portuguese Documentation)

- **Financial Institution**: The university/school that issues invoices
- **Customer**: Customers (students, candidates, or organizations)
- **Debt Account**: Links customer to institution; holds all financial documents
- **Debit Note**: Debt document requesting payment
- **Settlement Note**: Payment settlement document
- **Tuition Payment Plan**: Tuition payment plan with installments
- **Interests**: Late payment interest/penalties
- **SAFT-PT**: Portuguese tax authority XML format for invoice reporting