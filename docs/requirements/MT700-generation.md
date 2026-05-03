# MT700 generation
Generating a SWIFT MT700 (Issue of a Documentary Credit) requires transforming the structured, relational data from your internal database (e.g., Moqui entities) into the rigid, flat, text-based format required by the SWIFT network. 

SWIFT has strict constraints regarding character limits, specific allowed character sets (the SWIFT "X" character set), and line breaks. 

Here is the step-by-step technical and data-mapping process to gather data from the system entities and generate a compliant MT700 message.

---

## 1. The Generation Process Overview

The generation of an MT700 typically happens during the execution of the `Issue LC` service, immediately after the Checker authorizes the transaction.

1. **Data Retrieval:** The service queries the `ImportLc`, `TradeInstrument`, `TradeParty` (Mantle), and related configuration entities.
2. **Data Cleansing & Validation:** The system runs a pre-check to ensure all text fields comply with the **SWIFT X Character Set** (A-Z, a-z, 0-9, and specific punctuation like `- / ? : ( ) . , ' +`). Any invalid characters (like `_`, `@`, or `&`) must be stripped or converted (e.g., converting `&` to `AND`) before assembly.
3. **Tag Mapping & Assembly:** The system utilizes a SWIFT library (in the Java/Moqui ecosystem, libraries like **Prowide Core** are industry standard) to assemble the specific MT700 tags.
4. **Message Dispatch:** The assembled text block is wrapped in SWIFT Basic Header blocks and sent to the SWIFT Gateway/Alliance Access.

---

## 2. Entity-to-SWIFT Tag Data Mapping (Mandatory fields)

Below is the mapping detailing exactly how fields from the UI/Database map to the standard MT700 tags. 

*Note: In SWIFT terminology, "M" means Mandatory, and "O" means Optional.*

#### Block A: Header & General Info

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **27** | Sequence of Total | M | Hardcoded to `1/1`. (An MT700 is almost always a single message, unless the 45A/46A text exceeds 10,000 chars, requiring an MT701 continuation). |
| **40A** | Form of Doc Credit | M | `ImportLc.lcTypeEnumId`. Maps internal enums to SWIFT codes: `IRREVOCABLE`, `IRREVOCABLE TRANSFERABLE`, etc. |
| **20** | Documentary Credit No. | M | `TradeInstrument.transactionRef`. The system-generated unique ID (Max 16 chars). |
| **31C** | Date of Issue | M | `TradeInstrument.issueDate`. Formatted strictly as `YYMMDD`. |
| **31D** | Date and Place of Expiry | M | Concatenation: `TradeInstrument.expiryDate` (YYMMDD) + `ImportLc.expiryPlace` (Max 29 chars). |

#### Block B: The Parties

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **50** | Applicant | M | Query `TradeParty` where `role = Applicant`. Map `Party.legalName` and `PostalAddress`. Must truncate and wrap to SWIFT limit: Max 4 lines of 35 characters (`4x35`). |
| **59** | Beneficiary | M | Query `TradeParty` where `role = Beneficiary`. Format as `4x35`. If they have an account number, prepend it with `/`. |
| **51A** | Applicant Bank | O | (Used if issuing on behalf of another bank). Query `TradeParty` where `role = ApplicantBank`. Use `Party.swiftBic`. |

#### Block C: Financials & Terms

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **32B** | Currency Code, Amount | M | Concatenation: `TradeInstrument.currencyUomId` (3 chars) + `TradeInstrument.transactionAmount`. Amount must use a comma `,` as the decimal separator, not a period. |
| **39A** | Percentage Tolerance | O | `ImportLc.tolerancePositive` + `/` + `ImportLc.toleranceNegative`. E.g., `10/10`. |
| **41a** | Available With ... By ... | M | Logic: Determine the "Available With" bank (Often `ANY BANK` or a specific Advising Bank BIC). Append the `ImportLc.tenorTypeEnumId` mapped to SWIFT codes: `BY PAYMENT`, `BY ACCEPTANCE`, `BY NEGOTIATION`, or `BY DEF PAYMENT`. |
| **42C** | Drafts at (Tenor) | O | `ImportLc.usanceDays` + " DAYS AFTER " + `ImportLc.usanceBaseDate`. (Required if tenor is not sight). |
| **42A** | Drawee | O | Typically the Issuing Bank. Extracted from the bank's own system profile. |

#### Block D: Shipping & Goods

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **43P** | Partial Shipments | O | `ImportLc.partialShipmentEnumId`. Map to `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **43T** | Transhipment | O | `ImportLc.transhipmentEnumId`. Map to `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **44A** | Place of Taking in Charge | O | `ImportLc.receiptPlace`. (Max 65 chars). |
| **44E** | Port of Loading | O | `ImportLc.portOfLoading`. (Max 65 chars). |
| **44F** | Port of Discharge | O | `ImportLc.portOfDischarge`. (Max 65 chars). |
| **44C** | Latest Date of Shipment | O | `ImportLc.latestShipmentDate`. Formatted as `YYMMDD`. |

#### Block E: The Narratives (The heavy text)
*Note: These fields support up to 65,000 characters (using 100-line blocks of 65 chars each).*

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **45A** | Description of Goods | M | `ImportLc.goodsDescription`. System must automatically word-wrap text at 65 characters per line (`\r\n`). |
| **46A** | Documents Required | M | `ImportLc.documentsRequired`. System must format/bullet the list and apply the 65-char line wrap. |
| **47A** | Additional Conditions | O | `ImportLc.additionalConditions`. Applies standard 65-char line wrapping. |
| **71D** | Charges | O | `ImportLc.chargeAllocation`. Free text detailing who pays what (e.g., "ALL CHARGES OUTSIDE VIETNAM ARE FOR BENEFICIARY'S ACCOUNT"). |
| **48** | Period for Presentation | O | Typically calculated: "DOCUMENTS MUST BE PRESENTED WITHIN [X] DAYS AFTER THE DATE OF SHIPMENT BUT WITHIN THE VALIDITY OF THE CREDIT." |
| **49** | Confirmation Instructions| M | `ImportLc.confirmationEnumId`. Maps to `CONFIRM`, `MAY ADD`, or `WITHOUT`. |

---
## 3. Optional fields

**The previous list did not cover absolutely every field.** The previous mapping covered the foundational "Happy Path" fields—the core tags that are used in 95% of standard commercial Import LCs and that map directly to the UI wireframes we designed. 

However, the SWIFT MT700 is an incredibly comprehensive standard designed to handle highly complex, edge-case trade scenarios. To make your Business Requirements Document (BRD) 100% complete for the technical team, we must document the remaining **Optional (O)** fields. 

These remaining fields generally fall into three categories: Advanced Financials, Complex Bank Routing, and Specialized Transport.

Here is the supplementary data mapping for the remaining MT700 fields.

### 1. Advanced Financial & Payment Terms
These fields handle complex tolerances, specialized payment structures, and additional coverage beyond the pure goods value.

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **39B** | Maximum Credit Amount | O | `ImportLc.maxCreditAmountFlag`. Used instead of 39A (Tolerance) if the LC strictly states "NOT EXCEEDING". Mutually exclusive with 39A. |
| **39C** | Additional Amounts Covered | O | `ImportLc.additionalAmountsText`. Free text used if the LC covers specific additional costs like freight or insurance up to a certain limit. |
| **42M** | Mixed Payment Details | O | `ImportLc.mixedPaymentDetails`. Free text (Max 4x35 chars). Used only if Tag 41a is set to `BY MIXED PYMT`. Details how portions of the LC are paid (e.g., "30% AT SIGHT, 70% AT 90 DAYS"). |
| **42P** | Negotiation/Deferred Payment Details | O | `ImportLc.deferredPaymentDetails`. Used if Tag 41a is `BY DEF PAYMENT` or `BY NEGOTIATION` to specify the exact maturity calculation if Tag 42C is insufficient. |

### 2. Complex Bank Routing & Reimbursement
In global trade, the Issuing Bank doesn't always have a direct relationship (Nostro/Vostro account) with the Advising Bank. These fields instruct the banks on how to route the actual money and messages.

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **53a** | Reimbursing Bank | O | `TradeInstrument.reimbursingBankBic`. Highly critical if the Issuing Bank dictates a third-party bank (like JP Morgan NY) to settle USD claims. Maps to option A (BIC) or D (Name/Address). |
| **57a** | "Advise Through" Bank | O | `TradeInstrument.adviseThroughBankBic`. Used if the Issuing Bank cannot send the MT700 directly to the Beneficiary's bank (no SWIFT RMA), requiring routing through an intermediary. |
| **58A** | "Confirm" Bank | O | Used if the Issuing Bank dictates a third-party bank (like JP Morgan NY) to settle USD claims. Maps to option A (BIC) or D (Name/Address). |

### 3. Specialized Transport Variations
Depending on the Incoterms (e.g., EXW, DDP) or the mode of transport (e.g., multimodal, inland freight), the standard Port of Loading/Discharge fields are not always accurate.

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **44B** | Place of Final Destination / For Transportation to... / Place of Delivery | O | `ImportLc.finalDeliveryPlace`. Used for inland destinations (e.g., unloading at a seaport, but final delivery is a warehouse in a landlocked country). |
| **44D** | Shipment Period | O | `ImportLc.shipmentPeriodText`. Used when a specific range or condition is required instead of a hard date. (e.g., "WITHIN 30 DAYS AFTER ISSUANCE"). *System Rule: Mutually exclusive with Tag 44C (Latest Date of Shipment).* |

### 4. Administrative & System Instructions
These fields govern bank-to-bank instructions that the Applicant and Beneficiary rarely see.

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **23** | Reference to Pre-Advice | O | `TradeInstrument.preAdviceRef`. Used only if the bank previously sent an MT705 (Pre-Advice) and is now issuing the full MT700. |
| **72Z** | Sender to Receiver Information | O | `TradeInstrument.senderToReceiverInfo`. Free text (Max 6x35 chars). Bank-to-bank technical or regulatory info (e.g., formatting codes like `/REJT/` for rejection handling). |
| **78** | Instructions to the Paying / Accepting / Negotiating Bank | O | `ImportLc.bankToBankInstructions`. Free text (Max 12x65 chars). Crucial field dictating how the Presenting Bank should claim funds (e.g., "UPON CLEAN PRESENTATION, PLEASE CLAIM T/T REIMBURSEMENT FROM OUR ACCOUNT AT JPM NY"). |

### Technical Implementation Note for the BRD: The "a" Designator
When designing your database schemas and UI, pay close attention to SWIFT tags that end with a lowercase letter (e.g., **53a**, **57a**, **51a**). 

The lowercase "a" means the tag has multiple formatting options:
* **Option A (e.g., 53A):** The system uses a verified SWIFT BIC code. (This is highly preferred for automation/STP).
* **Option D (e.g., 53D):** The system uses a free-text Name and Address (used if the bank does not have a SWIFT code).

Your system UI must provide a toggle (e.g., `[Use BIC] / [Use Name & Address]`) for these specific party fields so the backend Moqui service knows whether to generate a 53A or a 53D tag in the final text block.

---

## 4. Execution Logic (How to code it)

When writing the service to generate this, do not attempt to construct the raw text string manually via string concatenation. SWIFT line-wrapping rules are notoriously finicky. 

**Recommended Approach:**
1.  **Use an SDK:** Utilize a library like Prowide WIFE (open-source Java SWIFT parser).
2.  **Instantiate the Message:**
    ```java
    MT700 mt700 = new MT700();
    ```
3.  **Populate Fields:** Use the library's setters, which automatically handle SWIFT field formatting.
    ```java
    mt700.addField(new Field20(tradeInstrument.transactionRef));
    mt700.addField(new Field31C(formatToYYMMDD(tradeInstrument.issueDate)));
    mt700.addField(new Field32B(tradeInstrument.currencyUomId + formatSwiftAmount(tradeInstrument.amount)));
    ```
4.  **Handle Long Text:** For 45A, 46A, and 47A, pass your raw Moqui strings into a SWIFT text-wrapper utility that safely splits the paragraph into the `[n lines] x [65 chars]` arrays required by those tags.
5.  **Build and Export:**
    ```java
    String swiftMessageBlock = mt700.message();
    ```
    This `swiftMessageBlock` is what you save to your `MessageLog` database table and transmit to the SWIFT Alliance Gateway via MQ or API.