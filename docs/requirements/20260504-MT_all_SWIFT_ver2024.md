## Revised MT tags - 2024 version

To give you some historical context: the massive, structural changes you are seeing did not *all* happen in 2024. The SWIFT network underwent a monumental, multi-year overhaul of Category 7 (Trade Finance) messages starting with the **SRG 2018 Release** (which completely rebuilt the MT 700/707) and concluding with the **SRG 2021 Release** (which rebuilt Guarantees/Standbys). 

The 2024 standard you are looking at is the settled, modern culmination of those overhauls. 

Because an Import LC lifecycle involves several distinct SWIFT messages, your Moqui system must map entirely different data dictionaries depending on the business event. Here are the exhaustive tag lists for the four most critical MT messages in the Import LC lifecycle (excluding the MT 707 we already mapped).

---

### 1. The Issuance Payload: MT 700 (Issue of a Documentary Credit)
This is the foundational contract. Every other message in the lifecycle references the data established here. 

**Header & Control (Mandatory)**
| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **27** | Sequence of Total | **M** | Used for pagination (e.g., `1/1`, `1/2`). |
| **40A** | Form of Doc. Credit | **M** | E.g., `IRREVOCABLE`, `IRREVOCABLE TRANSFERABLE`. |
| **20** | Documentary Credit Number | **M** | Your Moqui system's unique LC ID. |
| **31D** | Date and Place of Expiry | **M** | E.g., `260531VIETNAM`. |

**Parties & Financials**
| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **50** | Applicant | **M** | The corporate buyer. |
| **59** | Beneficiary | **M** | The corporate seller. |
| **32B** | Currency Code, Amount | **M** | E.g., `USD100000,00`. |
| **51a** | Applicant Bank | O | Used if the applicant is a bank acting on behalf of a client. |
| **52a** | Issuing Bank | O | Used if routing via a third party. |
| **39A** | Percentage Credit Tolerance | O | Format: `[Increase]/[Decrease]` (e.g., `10/10`). |
| **39B** | Maximum Credit Amount | O | Mutually exclusive with 39A. |
| **39C** | Additional Amounts Covered | O | Free text for insurance/freight. |

**Payment & Settlement Terms**
| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **41a** | Available With... By... | **M** | Defines the Negotiating Bank and payment method (Sight, Def Payment, etc.). |
| **42C** | Drafts at... | O | The Tenor (e.g., `AT SIGHT`). |
| **42a** | Drawee | O | The BIC of the bank the draft is drawn upon. |
| **42M** | Mixed Payment Details | O | Used if 41a is set to `MIXED PAYMENT`. |
| **42P** | Deferred Payment Details | O | Used if 41a is set to `DEFERRED PAYMENT`. |

**Logistics & Narrative**
| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **43P** | Partial Shipments | O | `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **43T** | Transhipment | O | `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **44A** | Place of Taking in Charge | O | Pre-carriage origin. |
| **44E** | Port of Loading | O | Origin port. |
| **44F** | Port of Discharge | O | Destination port. |
| **44B** | Place of Final Destination | O | Post-carriage destination. |
| **44C** | Latest Date of Shipment | O | Mutually exclusive with 44D. |
| **44D** | Shipment Period | O | Mutually exclusive with 44C. |
| **45A** | Description of Goods | O | Max 100 lines of 65 chars. |
| **46A** | Documents Required | O | Max 100 lines of 65 chars. |
| **47A** | Additional Conditions | O | Max 100 lines of 65 chars. |
| **48** | Period for Presentation | O | Days allowed to present docs after shipment. |

**Routing & Bank Instructions**
| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **71D** | Charges | O | Defines who pays which bank fees. |
| **49** | Confirmation Instructions | O | `CONFIRM`, `MAY ADD`, `WITHOUT`. |
| **58a** | Requested Confirming Bank | O | Must be present if 49 is `CONFIRM`. |
| **53a** | Reimbursing Bank | O | Settles the funds. |
| **78** | Instructions to Paying Bank | O | How the negotiating bank should claim funds. |
| **57a** | "Advise Through" Bank | O | The Second Advising Bank (as discussed previously). |
| **72Z** | Sender to Receiver Info | O | Private bank-to-bank instructions. |

---

### 2. The Claim Routing Payload: MT 740 (Authorization to Reimburse)
Sent to your Reimbursing Bank (usually where your Nostro account is held) to authorize them to pay the Negotiating Bank when a claim arrives.

| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **20** | Documentary Credit Number | **M** | The parent MT 700 LC Number. |
| **21** | Receiver's Reference | O | The Reimbursing Bank's ref (if known). |
| **25** | Account Identification | O | Your Nostro Account number to be debited. |
| **31D** | Date and Place of Expiry | **M** | Expiry of the *Reimbursement Auth*, not the LC. |
| **58a** | Negotiating Bank | **M** | The specific bank authorized to claim the funds. (Or `ANY BANK` if freely negotiable). |
| **59** | Beneficiary | **M** | Matches the MT 700. |
| **32B** | Credit Amount | **M** | The max amount authorized for reimbursement. |
| **39A** | Percentage Credit Tolerance | O | Matches the MT 700 tolerance. |
| **39B** | Maximum Credit Amount | O | Mutually exclusive with 39A. |
| **40F** | Applicable Rules | O | Usually `URR LATEST VERSION`. |
| **71D** | Charges | O | Defines who pays the Reimbursing Bank's fees. |
| **72Z** | Sender to Receiver Info | O | Private bank notes. |

---

### 3. The Discrepancy Payload: MT 750 (Advice of Discrepancy)
Sent by the Negotiating Bank to your Issuing Bank when the documents presented by the Beneficiary contain errors. It is a formal request for the Applicant to waive the discrepancies.

| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **20** | Sender's Reference | **M** | The Negotiating Bank's presentation reference. |
| **21** | Related Reference | **M** | Your Moqui MT 700 LC Number. |
| **32B** | Principal Amount Claimed | **M** | The exact value of the invoice/draft. |
| **33a** | Additional Amount | O | Extra fees or interest claimed. |
| **71B** | Charges | O | Discrepancy fees deducted by the Sender. |
| **73** | Charges Added | O | Discrepancy fees added to the claim. |
| **34a** | Total Amount Claimed | O | Calculated: 32B + 33a - 71B + 73. |
| **77J** | Discrepancies | **M** | **Critical Limit:** Max 70 lines of 50 chars. Moqui must forcefully restrict the UI input here. |
| **72Z** | Sender to Receiver Info | O | E.g., "Documents held at your disposal pending waiver." |

---

### 4. The Administrative Payload: MT 730 (Acknowledgment)
A simple, administrative message used to acknowledge receipt of an MT 700 or MT 707, or to formally advise that an LC has been delivered to the Beneficiary.

| Tag | Field Name | Status | SRG 2024 Rules & Notes |
| :--- | :--- | :--- | :--- |
| **20** | Sender's Reference | **M** | The Advising Bank's reference. |
| **21** | Receiver's Reference | **M** | Your Moqui MT 700/707 Number. |
| **25** | Account Identification | O | Nostro account for fee settlement. |
| **30** | Date of Message Being Ack'd | **M** | The Date of Issue (31C) of the message being acknowledged. |
| **57a** | Account With Bank | O | Bank where fees should be remitted. |
| **71D** | Charges | O | Advising fees owed. |
| **72Z** | Sender to Receiver Info | O | E.g., "LC safely advised to beneficiary." |

### System Pagination Note (MT 701 & MT 708)
You will see references to MT 701 and MT 708 in the SWIFT book. These do not have their own unique fields. They are strictly overflow messages. If the text in your Moqui database for an MT 700 exceeds the SWIFT network size limit, your generation logic must put the core financials in the MT 700, and push the remaining Goods (45A), Documents (46A), and Conditions (47A) into a generated MT 701.

## **GAP analysis**

This is exactly the kind of forensic analysis a banking system architect needs to do. However, before diving into the tables, I need to offer a crucial piece of chronological candor regarding the SWIFT network that will save your development team a lot of confusion.

### The Architectural Reality: The "2018 Revolution" vs. "2024 Stability"

The massive, structural overhaul of SWIFT Category 7 (Trade Finance) messages actually **occurred IN the SRG 2018 release**. 

Between 2018 and the current SRG 2024, the tag structures for Import LCs (MT 700, 707, 740, 750) have remained almost entirely stable. SWIFT made all the "breaking changes" in 2018 to prepare the network for eventual digitization and to force banks to use structured data instead of free text. 

Therefore, the actual "System Gap" you need to build against is **Legacy (Pre-2018) vs. Modern (SRG 2018–2024)**. If you are migrating an older system or dealing with legacy data mapped to old tags, this is the exact delta your database and SWIFT payload generator must account for.

Here is the exhaustive gap analysis of exactly what changed when SWIFT moved to the modern standard.

---

### 1. Gap Analysis: MT 700 / MT 701 (Issuance)

The goal of the overhaul here was to eliminate ambiguity. SWIFT took fields where banks historically typed whatever they wanted (free text) and locked them down into strict, machine-readable codes.

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **43P** (Partial Shipments) | Free Text. Banks would type "Allowed", "Permitted", "Yes", etc. | **Restricted Codes.** | Moqui UI must convert to a rigid dropdown: `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **43T** (Transhipment) | Free Text. | **Restricted Codes.** | Moqui UI must convert to a rigid dropdown: `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **49G** (Payment Cond. Bene) | Did not exist. Banks crammed this into Tag 47A. | **ADDED** | System must provide a dedicated text block for Beneficiary-specific payment instructions. |
| **49H** (Payment Cond. Bank) | Did not exist. Banks crammed this into Tag 47A or 78. | **ADDED** | System must provide a dedicated text block for Receiving Bank-specific payment instructions. |
| **58a** (Confirming Bank) | Used broadly for various confirmation requests. | **Strict Dependency.** | Moqui must enforce that 58a is *only* populated if Tag 49 is set to `CONFIRM`. |

---

### 2. Gap Analysis: MT 707 / MT 708 (Amendments)

This is where SWIFT completely broke legacy systems. They transformed the MT 707 from a "loose memo" into a strictly linked relational database record. 

#### A. The Header & Linkage Gap
Legacy systems allowed you to send an amendment with almost no context. Modern SWIFT forces your system to prove it knows the exact history of the LC.

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **22A** (Purpose) | Did not exist. | **ADDED (Mandatory)** | Moqui must automatically inject `ACNF`, `ADVI`, or `ISSU`. |
| **23** (Issuing Bank Ref) | Optional / Unused. | **Made Mandatory.** | Moqui must carry the Master LC number forward into this tag. |
| **26E** (Amendment No.) | Optional. | **Made Mandatory.** | Moqui must count previous accepted amendments and auto-increment. |
| **30** (Date of Amendment)| Optional. | **Made Mandatory.** | Moqui must stamp the current business execution date. |
| **31C** (Date of Issue) | Optional. | **Made Mandatory.** | Moqui must query the DB for the original MT 700 issue date. |
| **59** (Beneficiary) | Mandatory. | **Made Optional.** | Moqui should only map this if the Beneficiary details are actively changing. |

#### B. The Financials Gap
SWIFT realized that asking banks to send "New Totals" caused mathematical disputes if an earlier amendment was rejected. They removed the "Total" fields entirely.

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **31E** (New Expiry Date)| Used to change Expiry. | **DELETED** | Moqui must use Tag **31D** (Date/Place of Expiry) for amendments. |
| **34B** (New Total Amount)| Used to declare the new LC value. | **DELETED** | Moqui must *never* generate 34B. The network calculates the total via 32B (Increase) / 33B (Decrease). |

#### C. The Narrative Text Gap
In the old days, a user would type a massive paragraph in Tag 79 explaining every change. SWIFT deleted Tag 79 and forced line-item amendments using "B" tags.

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **79** (Narrative) | Used for all text changes. | **DELETED** | Moqui must reject any attempt to output Tag 79. |
| **45B** (Goods) | Did not exist. | **ADDED** | Moqui must map Goods changes here using `/ADD/`, `/DELETE/`. |
| **46B** (Documents) | Did not exist. | **ADDED** | Moqui must map Docs changes here using `/ADD/`, `/DELETE/`. |
| **47B** (Conditions) | Did not exist. | **ADDED** | Moqui must map Conditions changes here using `/ADD/`, `/DELETE/`. |

---

### 3. Gap Analysis: MT 740 (Authorization to Reimburse)

The changes here were about aligning the message with the specific ICC rules for reimbursements (URR 725).

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **39A / 39B** (Tolerance)| Did not exist in 740. Reimbursing banks had to guess if the claim included tolerance. | **ADDED** | Moqui must carry the exact tolerance from the MT 700 into the MT 740 so the Reimbursing bank knows the maximum authorized limit. |
| **40F** (Applicable Rules)| Did not exist. | **ADDED** | Moqui should populate this with `URR LATEST VERSION` to legally bind the reimbursement. |
| **58a** (Negotiating Bank)| Used loosely. | **Tightened Rules.** | Must match exactly who is authorized to claim. |

---

### 4. Gap Analysis: MT 750 (Advice of Discrepancy)

The MT 750 remained largely unchanged structurally, but SWIFT enforced strict technical validation on the discrepancy list to prevent system crashes at the receiving bank.

| SWIFT Tag | Legacy (Pre-2018) | Modern (SRG 2018–2024) | System Impact / Gap Action |
| :--- | :--- | :--- | :--- |
| **77J** (Discrepancies) | Loosely enforced length. | **Strict Truncation.** | The SWIFT gateway will aggressively reject any payload where Tag 77J exceeds 70 lines of 50 characters. Moqui must enforce this at the UI input level. |

### The Future Gap (Post-2024)
While the 2018–2024 standards are the current law of the land, the next actual gap your architecture team needs to monitor is the **ISO 20022 Migration (CBPR+)**. While Payments (MT 103, 202) are actively migrating to XML (pacs/camt messages) right now, Trade Finance (Category 7) is on a delayed timeline. Eventually, the MT 700 will be replaced entirely by XML schemas, but building a robust relational database in Moqui today will make that future XML mapping trivial.