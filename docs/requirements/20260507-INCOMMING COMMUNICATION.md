# **INCOMMING COMMUNICATION**

## **Inbound Message Correlation Engine**

This is the most challenging architectural hurdle in building a Trade Finance system. Unlike modern REST APIs where you send a request and immediately get a JSON response (`200 OK`), the SWIFT network is entirely **asynchronous**. 

You might send an MT 707 Amendment on a Tuesday, and the Beneficiary’s bank might not respond until Friday. During those three days, your Moqui system must safely suspend that transaction in a pending state.

To handle responses from other banks, your system must implement an **Inbound Message Correlation Engine**. Here is the exact architectural blueprint for how a core banking system listens to the network, matches messages, and triggers state changes for Amendments and Discrepancies.

---

### The Core Mechanism: The Correlation Engine & Tag 21

Banks do not query each other's databases. They communicate by dropping messages into each other's SWIFT queues. Your system must have a background service (often using JMS, Apache Camel, Kafka,... or dedicated service in Moqui framework) constantly listening to your bank's inbound SWIFT Alliance Access (SAA) queue, or a dir in secured server that store incomming SWIFT message text file.

The "magic key" that connects an inbound message from a bank in London to a specific LC record in your Moqui database is **Tag 21 (Related Reference)**.

**The Golden Rule of SWIFT Routing:**
When your system sends a message, it puts its Moqui LC Number in **Tag 20 (Sender's Reference)**. When the foreign bank replies, they are universally mandated to bounce that exact number back to you in **Tag 21**. 

Here is how the Moqui Correlation Engine processes an inbound message:
1.  **Consume & Parse:** Moqui reads the raw SWIFT string from the queue and parses it (using a library like Prowide Core).
2.  **Extract Key:** The system extracts the value in Tag 21.
3.  **Database Lookup:** Moqui queries the database: `SELECT id FROM ImportLC WHERE lcNumber = [Tag 21 Value]`.
4.  **Route to Workflow:** Once the parent LC is found, the system looks at the Message Type (e.g., MT 730, MT 799, MT 750) and triggers the appropriate Moqui State Machine event.

---
### High level Business Processing for incomming messages

#### Business Process A: Tracking Amendment Acceptance/Rejection

Under UCP 600, you send the MT 707, and the amendment sits in a `Pending Consent` state. Here is how the system handles the response.

**Path 1: The Explicit Response (Inbound MT 730 or MT 799)**
The Beneficiary explicitly tells their bank to accept or reject the amendment. Their bank sends you a message.
*   **The Trigger:** Your system receives an inbound **MT 730 (Acknowledgment)** or an **MT 799 (Free Format Message)** with your MT 707 number in Tag 21.
*   **System Action:** Because MT 799s are free-text, it is extremely dangerous to let the system auto-approve an amendment based on text parsing (e.g., the text might say "Beneficiary does *not* accept").
*   **The Moqui Workflow:** 
    1. The system links the inbound message to the pending Amendment Record.
    2. It drops the message into an operational UI queue called the **"Trade Inbox."**
    3. An operations user reads the MT 799 on screen, sees "We accept the extension," and clicks the **"Log Acceptance"** button in Moqui. 
    4. The system executes the SECA to merge the delta, release the limits, and change the state to `Accepted`.

**Path 2: Implied Consent (The UCP 600 Reality)**
In reality, 80% of Beneficiaries never send an explicit acceptance message. They simply ship the goods and present documents that match the new terms. Under UCP 600, this constitutes legally binding "Implied Consent."
*   **The System Action:** Your Moqui UI must have a manual override button on the Pending Amendment record labeled **"Force Accept (Implied Consent)."** 
*   When documents arrive, the Checker verifies they match the amended terms, clicks this button, and the system instantly merges the amendment into the Master LC record so the document examination module can validate against the new values.

---

#### Business Process B: Handling Discrepancies

When the Beneficiary ships the goods, their bank (the Negotiating Bank) sends the documents to you (the Issuing Bank) requesting payment. 

**Step 1: The Inbound Claim (MT 750 or Courier)**
*   **The Trigger:** Your system receives an inbound **MT 750 (Advice of Discrepancy)**. 
*   **System Action:** Moqui parses Tag 21 to find your LC. It automatically generates a "Presentation Record" as a child of the LC. It sets the state to `Pending Examination`.

**Step 2: Issuing Bank Rejection (Outbound MT 734)**
If your document examiners find discrepancies and the Applicant refuses to waive them, your system must generate an outbound message.
*   **System Action:** The Maker selects "Refuse Documents" in the Moqui UI. The system generates an **MT 734 (Notice of Refusal)**. It populates Tag 20 with the Presentation ID, Tag 21 with the Negotiating Bank's reference, and Tag 77J with the exact discrepancies. 
*   **State Change:** The Presentation Record shifts to `Refused - Awaiting Instructions`.

**Step 3: Applicant Waiver & Payment (Outbound MT 202)**
If the Applicant says, "I don't care that the shipment was late, pay them anyway" (Waiving the Discrepancy).
*   **System Action:** Operations clicks "Waive & Pay" in Moqui. The system generates an **MT 202 (General Financial Institution Transfer)** to wire the funds to the Negotiating Bank, effectively closing the discrepancy loop.

---

### The UI Requirement: The "Trade Inbox"

Because inbound SWIFT messages like the MT 799 (Free Format) or MT 999 cannot be perfectly evaluated by machine logic, you cannot build a completely "hands-free" state machine. 

To handle asynchronous banking communication successfully, your Moqui frontend must include a **Trade Inbox UI**. This acts like an email inbox for your operations team, where every inbound message is already pre-linked (via Tag 21) to its parent LC. The user clicks the message, reads the narrative, and manually clicks the "Accept," "Reject," or "Acknowledge" action to progress the Moqui state machine.

---

## **Detailed Data Mapping & Business Processing**

After we have successfully parsed the incomming SWIFT message (i.e as `.txt` file), found the matching LC (via Tag 21), and dropped the message into the Moqui **Trade Inbox**. 

Now, we must define the exact data mapping and the state machine logic that executes when a human operator clicks a button. 

Here is the exhaustive blueprint for how the three primary inbound messages (MT 730, MT 799, MT 750) map to your Moqui database, and exactly what happens during the "Accept," "Reject," or "Acknowledge" UI actions.

---

### The Foundation: The Trade Inbox Entity
Before we look at specific messages, every parsed inbound message is first saved to a universal `TradeInboxItem` entity.

*   `inboxId` (Primary Key)
*   `lcNumber` (Mapped from Tag 21)
*   `messageType` (e.g., '730', '799')
*   `senderBic` (Parsed from SWIFT Basic Header Block 1)
*   `receivedDate` (Timestamp of the cron job ingestion)
*   `status` (Enum: `UNREAD`, `PROCESSED`, `ORPHANED`)

When an operator clicks a row in the Trade Inbox, the UI reads the `messageType` and dynamically renders the appropriate action screen.

---

### Scenario 1: Inbound MT 730 (Acknowledgment)
The Advising Bank sends this to confirm they received your MT 700 (Issuance) or MT 707 (Amendment) and have delivered it to the Beneficiary. 

#### A. Data Mapping (MT 730 $\rightarrow$ Moqui UI/DB)
| SWIFT Tag | Description | Moqui UI / Entity Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Ref | Saved as `AdvisingBankReference` on the LC Record. |
| **21** | Related Ref | Used to find the LC. |
| **30** | Date of Msg Ack'd| Used by the UI to show *which* specific outbound message (Issuance vs. Amendment #2) is being acknowledged. |
| **71D / 72Z**| Charges / Notes | Displayed as read-only text on the Inbox screen. |

#### B. Operator Action: "Acknowledge Receipt"
Because an MT 730 is purely administrative, the operator only has one action button.
*   **Business Impact:** The bank now has legal proof of delivery.
*   **Moqui State Change:**
    *   If acknowledging the MT 700 Issuance: Updates the Master LC status from `Issued - Pending Advise` to `Issued - Advised`.
    *   If acknowledging an MT 707: Updates the Pending Amendment record status to `Advised to Beneficiary`. *(Note: This is NOT acceptance of the amendment, just proof they received it).*
*   **System Action:** Closes the `TradeInboxItem` (marks as `PROCESSED`). If Tag 71D contains advising fees owed by your bank, it automatically queues a payable invoice in the Moqui Accounting module.

---

### Scenario 2: Inbound MT 799 (Free Format - Amendment Consent)
The Beneficiary's bank sends this free-text message to explicitly accept or reject your pending MT 707 amendment.

#### A. Data Mapping (MT 799 $\rightarrow$ Moqui UI/DB)
| SWIFT Tag | Description | Moqui UI / Entity Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Ref | Logged for audit trails. |
| **21** | Related Ref | Used to find the LC. |
| **79** | Narrative | **Critical:** Displayed prominently in the UI. This text contains the actual consent (e.g., "Beneficiary accepts amendment 1" or "Amendment 1 rejected"). |

#### B. Operator Actions & State Machine
The operator reads the text in Tag 79 and makes a human decision to click one of two buttons.

**Action 1: Click "Accept Amendment"**
*   **Business Impact:** The amendment is legally binding. The new terms are the master contract.
*   **Moqui State Change:** The `LcAmendment` record changes from `Pending Consent` to `Accepted`. 
*   **System Actions:**
    *   **The Merge:** The system overwrites the `ImportLC` master fields with the amendment delta.
    *   **Liability Release:** If the amendment was for a *decrease* in the LC amount, the Moqui Core Banking API is triggered to release the excess facility limit back to the Applicant.
    *   Marks the `TradeInboxItem` as `PROCESSED`.

**Action 2: Click "Reject Amendment"**
*   **Business Impact:** The amendment is dead. The original LC remains in force.
*   **Moqui State Change:** The `LcAmendment` record changes from `Pending Consent` to `Rejected`.
*   **System Actions:**
    *   **The Rollback:** The Master LC is untouched. 
    *   **Liability Release:** If the rejected amendment was for an *increase*, the system immediately releases the extra limit that was earmarked when the MT 707 was dispatched.
    *   Marks the `TradeInboxItem` as `PROCESSED`.

---

### Scenario 3: Inbound MT 750 (Advice of Discrepancy)
The Negotiating Bank sends this when the shipping documents do not match the LC terms. They are asking if your Applicant will waive the errors and pay anyway.

#### A. Data Mapping (MT 750 $\rightarrow$ Moqui UI/DB)
When the system correlates an MT 750, it automatically spawns a new child entity linked to the LC: `LcPresentation`.

| SWIFT Tag | Description | Moqui UI / Entity Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Ref | Saved as `NegotiatingBankReference` on the Presentation. |
| **32B** | Principal Claimed | Mapped to `LcPresentation.claimAmount`. |
| **33a / 73**| Additional Fees | Mapped to `LcPresentation.additionalCharges`. |
| **77J** | Discrepancies | Mapped to `LcPresentation.discrepancyList`. |

#### B. Operator Actions & State Machine
The operator contacts the corporate Applicant outside the system (via phone or corporate portal) to show them the Tag 77J discrepancies. Based on the Applicant's decision, the operator clicks one of two buttons.

**Action 1: Click "Waive Discrepancies & Accept"**
*   **Business Impact:** The Applicant agrees to buy the goods despite the errors. The bank is now legally obligated to pay the Negotiating Bank.
*   **Moqui State Change:** The `LcPresentation` record moves to `Waived - Pending Settlement`.
*   **System Actions:**
    *   **Limit Deduction:** The system formally deducts the `claimAmount` from the LC's unutilized balance.
    *   **Payment Generation:** The system automatically queues an outbound **MT 202 (Bank-to-Bank Transfer)** to settle the funds on the requested value date. 

**Action 2: Click "Refuse Documents"**
*   **Business Impact:** The Applicant refuses the goods. The bank declines the claim.
*   **Moqui State Change:** The `LcPresentation` record moves to `Refused`.
*   **System Actions:**
    *   **Message Generation:** The system automatically generates an outbound **MT 734 (Notice of Refusal)**. It pulls the Tag 20 from the inbound MT 750 and places it in the outbound Tag 21. It pulls the discrepancy list from the database and injects it into the MT 734 Tag 77J. 
    *   **Limits:** The LC balance remains unchanged (funds are not deducted).

***

Because the decision to Waive or Refuse an MT 750 requires the corporate Applicant's input, we can plan to build an external "Corporate Portal" where the Applicant can log in and click "Waive" themselves, or internal bank operators manually record the Applicant's decision received via email/phone into the Moqui UI.

---

### The Moqui "Discrepancy Resolution" UI Flow

Because the operator is translating an offline instruction (an email or phone call) into a system action that moves real money, you must architect a strict **Audit and Dual-Control (Maker/Checker) Pipeline** around this manual step.

Here is the precise Moqui UI workflow and database mapping for a Bank Operator handling an MT 750 Discrepancy.

When the operator opens the `LcPresentation` record (which was spawned by the inbound MT 750), the system must enforce the following steps.

#### Step 1: The Decision Capture (Maker)
The Maker receives an email from the Corporate Applicant stating, "We accept the late shipment, please pay." The Maker goes into Moqui and clicks **"Resolve Discrepancies."**

The UI presents two distinct paths, both of which require mandatory audit data.

**Path A: Log Applicant Waiver (Pay)**
*   **Action Selector:** Maker selects `Waive Discrepancies`.
*   **Mandatory Upload:** The system **blocks submission** unless the Maker uploads a file (e.g., PDF of the Applicant's signed waiver or a screenshot of the authorized email). This is mapped to Moqui's `Content` entity and linked to the `LcPresentation`.
*   **Value Date Input:** Maker inputs the date the Applicant wants the funds debited (cannot be in the past).
*   **Submission:** Maker clicks Submit.
*   **State Change:** `LcPresentation` moves from `Pending Examination` to `Pending Waiver Authorization`.

**Path B: Log Applicant Refusal (Reject)**
*   **Action Selector:** Maker selects `Refuse Documents`.
*   **Mandatory Upload:** System requires upload of the Applicant's refusal instruction.
*   **Return Instructions:** Maker selects from a SWIFT-compliant dropdown what to do with the physical documents (e.g., *Hold at your disposal*, *Return to presenter*). This populates Tag 77B for the outbound MT 734.
*   **Submission:** Maker clicks Submit.
*   **State Change:** `LcPresentation` moves to `Pending Refusal Authorization`.

#### Step 2: Dual Control (Checker)
A single operator cannot be allowed to trigger an MT 202 payment based on an offline email. A Checker must authorize it.

The Checker logs into Moqui, opens their queue, and reviews the Maker's work.
*   **The Audit Check:** The Checker physically opens the PDF/Email uploaded by the Maker in Step 1 and verifies that the authorized signer from the corporate Applicant actually agreed to the exact discrepancies listed in Tag 77J.
*   **Checker Action:** Clicks **"Authorize."**

#### Step 3: Automated Execution (The System Payload)
The moment the Checker clicks Authorize, the Moqui SECA engine takes over and fires the appropriate payload based on the Maker's path.

**If Authorized for Waiver (Path A):**
1.  **Accounting:** Moqui calls the Core Banking API to debit the Applicant's account and deduct the amount from the LC's unutilized balance.
2.  **SWIFT Outbound:** Moqui generates the **MT 202 (Bank-to-Bank Transfer)** to settle the funds with the Negotiating Bank.
3.  **Final State:** `LcPresentation` moves to `Settled`.

**If Authorized for Refusal (Path B):**
1.  **Accounting:** No funds are moved. The LC balance remains unchanged.
2.  **SWIFT Outbound:** Moqui generates the **MT 734 (Notice of Refusal)**. The system pulls the original discrepancies (from the inbound MT 750) and the Maker's return instructions, mapping them into the payload.
3.  **Final State:** `LcPresentation` moves to `Refused - Closed`.

---

By forcing the Maker to upload the physical proof of the Applicant's decision *before* the Checker can authorize the SWIFT message, your Moqui system covers all regulatory audit requirements while keeping the architecture simple.

---

## **Data Mapping & Business Processing - Happy Paths**

Those three scenarios above are just the most operationally complex because they require human decision-making and exception handling. 

In fact, most of the time we have "Happy Path" scenarios.

The **MT 750 (Advice of Discrepancy)** only arrives when something goes wrong. If the Beneficiary ships the goods perfectly according to the LC terms, the Negotiating Bank will *never* send an MT 750. 

Here are the other critical inbound SWIFT messages your Moqui system must be architected to catch and process during an Import LC lifecycle.

---

### 1. The "Happy Path" Payload: MT 754 (Advice of Payment/Acceptance/Negotiation)
When the Beneficiary presents perfect, clean shipping documents to their local bank, that bank negotiates the documents and sends you an MT 754. It means: *"The documents were perfect, we took them, we are forwarding them to you via courier, and you owe us the money."*

*   **Business Impact:** The bank is now legally bound to pay. There are no discrepancies to waive.
*   **Data Mapping (Tag 21):** The system uses Tag 21 to find the `ImportLC`.
*   **Moqui State Machine:** 
    *   Just like the MT 750, the system automatically spawns a child `LcPresentation` record.
    *   However, the state skips the examination queue entirely and lands directly in `Clean - Pending Settlement`.
*   **Operator Action:** The operator does not need to contact the Applicant for a waiver. The Maker simply verifies the claim amount (Tag 32A/B), inputs the Value Date, and the Checker authorizes the outbound **MT 202** payment.

### 2. The Direct Claim Payload: MT 742 (Reimbursement Claim)
This message bypasses the Trade Finance operations department entirely and hits your bank's Settlement/Reimbursement department. 

If your Import LC authorized another bank to claim funds (via an outbound MT 740 Authorization to Reimburse), the claiming bank will send an MT 742 to get their money.

*   **Business Impact:** This is a pure money-movement message governed by URR 725, not UCP 600.
*   **Data Mapping (Tag 21):** The system searches for the active `ReimbursementAuthorization` linked to the LC.
*   **Moqui State Machine:**
    *   **Auto-Match Logic:** Your system must check if the MT 742 claim amount (Tag 32B) is $\le$ the authorized amount. 
    *   If it matches, the system can automatically generate the **MT 202** payment (Straight-Through Processing) without human intervention.
    *   If the claim exceeds the authorization, the system suspends it in a `Pending Exception` queue for an operator to investigate.

### 3. The Unauthenticated Administrative Payload: MT 999 (Free Format Message)
This is exactly the same as the MT 799 (Free Format), but with one massive systemic difference: **It lacks SWIFT cryptographic authentication.** 

If a foreign bank needs to talk to your bank about an LC, but you do not have an active RMA (Relationship Management Application) key exchanged with them, the SWIFT network will block an MT 799. The bank will be forced to send an MT 999 instead.

*   **Business Impact:** High risk of fraud. Anyone on the network can send an MT 999 claiming to be someone else.
*   **Moqui State Machine:** 
    *   When the system correlates an MT 999 to the `TradeInbox`, it must flag it with a severe **SECURITY WARNING** in the UI.
    *   **Strict Rule:** Operators must never execute a financial amendment or process a cancellation based on an MT 999. It is strictly for non-financial queries (e.g., "What is the courier tracking number for the documents?").

### 4. The Transfer / Assignment Notification: MT 720 (Transfer of a Documentary Credit)
If you issued an `IRREVOCABLE TRANSFERABLE` LC, the First Beneficiary can ask their advising bank to transfer the LC to a Second Beneficiary (the actual manufacturer).

While your bank (the Issuing Bank) does not *execute* the transfer, the Transferring Bank might send you an MT 720 (or an MT 799) as a courtesy copy to inform you that the legal parties have changed.
*   **Moqui Action:** This drops into the `TradeInbox`. The operator reads it and uses the **Internal Bank Amendment** UI to update the Beneficiary details in the Master LC record for compliance/sanctions tracking, without generating an outbound SWIFT message.

***

To summarize the inbound architecture for Phase 1: Your Moqui correlation engine needs to listen primarily for **730** (Ack), **799** (Admin/Consent), **750** (Discrepancies), and **754** (Clean Docs). 

If your `.txt` ingestion pipeline drops any of these into the Trade Inbox, your operators will have 99% of the Import LC lifecycle covered.

---
## Import LC business processes mapping

This is the perfect way to ground the technical architecture back into the business reality. You are completely right to force this alignment; if the SWIFT correlation engine doesn't directly map to the chapters in your BRD, the developers won't know *where* to route the data.

Let’s bridge that gap right now. Here is the exact mapping of every inbound SWIFT message we discussed directly into your 6 specific Core Business Processes. 

---

### 3.1. Process: Import LC Issuance
*Your system just generated an MT 700 to issue the LC. The liability is booked.*

*   **Inbound Message:** **MT 730 (Acknowledgment)**
*   **Business Impact:** This acts as the "Delivery Receipt." The Advising Bank confirms they received your MT 700 and handed it to the Beneficiary.
*   **Moqui State Impact:** The operator clicks "Acknowledge" in the Trade Inbox. The Master LC status moves from `Issued - Pending Advise` to `Issued - Advised`. (This is crucial for the Applicant, who often calls asking, "Did my supplier get the LC yet?")

### 3.2. Process: Import LC Amendments
*Your system generated an MT 707 to change the terms, but it is waiting in a `Pending Consent` state.*

*   **Inbound Message A:** **MT 730 (Acknowledgment)**
    *   **Business Impact:** Delivery receipt for the amendment.
    *   **Moqui State Impact:** Updates the Pending Amendment record to `Advised to Beneficiary`.
*   **Inbound Message B:** **MT 799 (Free Format - Consent)**
    *   **Business Impact:** The Beneficiary explicitly says "Yes, I accept" or "No, I reject."
    *   **Moqui State Impact:** The operator reads the text and clicks "Accept" or "Reject." This triggers the Moqui SECA to either merge the delta into the Master LC and release limits (if accepted), or kill the amendment (if rejected).

### 3.3. Process: Document Presentation & Examination
*The supplier shipped the goods. This process handles the arrival of the claim against your bank.*

*   **Inbound Message A:** **MT 750 (Advice of Discrepancy)**
    *   **Business Impact:** The documents have errors. The foreign bank is asking, "Will the Applicant waive these discrepancies?"
    *   **Moqui State Impact:** The system automatically spawns a `LcPresentation` record and places it in the `Pending Examination/Waiver` queue. The operator must execute the Maker/Checker waiver flow we discussed earlier.
*   **Inbound Message B:** **MT 754 (Advice of Clean Presentation)**
    *   **Business Impact:** The documents are perfect. You legally owe the money. No examination is required.
    *   **Moqui State Impact:** The system spawns a `LcPresentation` record but completely skips the Examination queue, landing directly in the `Ready for Settlement` queue.

### 3.4. Process: Settlement & Payment
*The documents are accepted (or discrepancies waived). It is time to move the actual money.*

*   **Inbound Message A:** **MT 742 (Reimbursement Claim)**
    *   **Business Impact:** The foreign bank is claiming the funds from your Nostro account based on a prior authorization. 
    *   **Moqui State Impact:** Bypasses the Trade operators and goes to the Settlement queue. If the amount matches the LC, Moqui automatically generates the **MT 202** payment to settle the debt.
*   **Inbound Message B:** **MT 999 / MT 799 (Chaser / Tracing)**
    *   **Business Impact:** A "Where is my money?" message from the foreign bank.
    *   **Moqui State Impact:** Drops into the Trade Inbox. The operator opens the LC, checks the payment status, and manually replies to the foreign bank.

### 3.5. Process: Shipping Guarantees (SG) & Air Waybill Endorsements
*The goods arrived at the port *before* the physical documents arrived at your bank. The Applicant needs to get the cargo off the ship to avoid demurrage fees.*

*   **Inbound SWIFT Impact:** **Almost ZERO.** 
    *   **Why?** This is a purely physical and local legal process. The Applicant walks into your branch with a copy of the Bill of Lading. You issue a paper Shipping Guarantee to the local shipping line (e.g., Maersk, Evergreen). 
    *   *(Rare Exception)* You might receive an **MT 999** from the foreign bank giving you the DHL/FedEx tracking number so you know the real documents are currently on an airplane heading to your branch. 

### 3.6. Process: Cancellations, Expiry, and Limit Reversal
*The LC is no longer needed, or it has expired. The bank needs to close it out and give the credit facility limits back to the Applicant.*

*   **Inbound Message:** **MT 799 (Consent to Cancel)**
    *   **Business Impact:** If an Applicant wants to cancel an LC *before* the Expiry Date, UCP 600 dictates you cannot just close it. The Beneficiary must agree to give up their right to draw. 
    *   **Moqui State Impact:** The system receives the MT 799 stating "Beneficiary agrees to cancellation." The operator clicks "Process Cancellation." The SECA engine immediately changes the LC status to `Cancelled - Closed` and releases 100% of the remaining earmarked limits back to the Core Banking system.

---

By mapping it this way, we can see exactly which SWIFT triggers activate the state machine for each specific chapter of the BRD. 

Since Process 3.5 (Shipping Guarantees) bypasses the SWIFT network entirely and relies heavily on physical paper and local shipping line systems, we can planning to track the SG inventory and expiry dates natively within a new Moqui data entity, or will rely on an external collateral management system.

---

# **TECHNICAL SOLUTION TO HANDLE SWIFT MESSAGE**

## **Manual upload approach**

That is a very pragmatic approach for Phase 1. Integrating directly with a SWIFT Alliance Access (SAA) gateway via MQ/JMS takes months of network security clearance and infrastructure setup. 

Starting with manual `.txt` file uploads allows you to build, test, and launch the core Moqui business logic immediately. 

However, moving from an automated API to manual `.txt` files changes your system architecture. Raw text files introduce a massive security risk (they can be edited in Notepad before upload) and require a specific handling pipeline.

Here is the architectural blueprint for building a secure, automated `.txt` file import pipeline within your Moqui component.

---

### Phase 1: The Upload & Security Gate
Because text files are inherently untrusted, you cannot simply let a user upload a file and have the system blindly execute a payment or accept an amendment. 

1.  **The Upload UI:** You will build a Moqui screen (e.g., `SwiftImport.xml`) with a bulk file upload tool. 
2.  **Audit Stamping:** The moment the file hits the Moqui server, the system must hash the file (e.g., SHA-256) and save it to an `InboundSwiftRaw` database entity. This proves exactly what the user uploaded, protecting the bank in case of internal fraud.
3.  **The "No-Touch" Rule:** Your system must read the file, extract the data, and immediately discard the file from the active UI. Operations users must interact *only* with the parsed Moqui database records, never the raw text file itself.

### Phase 2: The Parsing Engine (Prowide Core)
Do not attempt to write a custom regex parser for SWIFT `.txt` files. SWIFT blocks, X-characters, and trailer logic are incredibly complex.

Instead, add **Prowide Core** (the open-source Java standard for SWIFT) to your Moqui component's `build.gradle`:
`implementation 'com.prowidesoftware:pw-swift-core:SRU2023-10.1.1'`

Your Moqui Groovy upload service will execute logic that looks like this:
1.  Read the `.txt` file BLOB.
2.  Pass it to Prowide: `SwiftMessage sm = SwiftMessage.parse(fileContent);`
3.  Extract the message type: `String mtType = sm.getType();` (e.g., "730", "799").
4.  Extract the keys: `String tag20 = sm.getBlock4().getTagValue("20");` and `String tag21 = sm.getBlock4().getTagValue("21");`.

### Phase 3: The Routing & Correlation Logic
Once Prowide extracts Tag 21, the system must figure out what to do with the message. 

*   **The Happy Path (Match Found):**
    The system queries your `ImportLC` entity. If it finds an LC where `lcNumber == Tag 21`, it generates a new record in the `TradeInboxItem` entity, linking the parsed SWIFT message directly to that specific LC. 
*   **The Unmatched Queue (Crucial Requirement):**
    What happens if the foreign bank made a typo in Tag 21, or if they left it blank (which happens frequently in MT 799s)? Your Moqui service will fail to find the LC. 
    *   *Rule:* The system must **never** reject the upload. Instead, it routes the message to an "Orphan Queue" UI. An operations manager must open the Orphan Queue, read the narrative of the MT 799, manually search the Moqui database for the correct LC, and click "Link to LC".

### Phase 4: The "Trade Inbox" Action Center
Now the `.txt` file has been converted into secure, relational Moqui data and linked to the correct LC. 

When the Maker opens the LC in Moqui, they will see an alert: *"1 New Inbound Message."*

1.  **For an MT 730 / 799 (Amendment Consent):** The user clicks the message. The system displays the parsed narrative. The user reads it, verifies the Beneficiary agreed, and clicks a button labeled **"Apply Amendment Consent."** This triggers the SECA we discussed earlier, merging the pending amendment into the Master LC.
2.  **For an MT 750 (Discrepancies):** The user clicks the message. Because it is an MT 750, Moqui *knows* this is a claim. The system automatically launches a "Process Discrepant Presentation" UI, pre-filling the claim amount (Tag 32B) and the discrepancy list (Tag 77J) directly from the parsed file.

***

### The Future-Proofing Benefit
The beauty of this architecture is that when you eventually get IT budget to connect directly to the SWIFT Alliance Access (SAA) gateway via an MQ queue in Phase 2, **you don't have to rewrite any of your business logic.** 

You simply replace the "Phase 1 UI Upload" with a "Phase 2 JMS Listener." The listener will consume the message from the queue and hand it directly to your Phase 2 Parsing Engine. The Routing, the Orphan Queue, and the Trade Inbox will remain exactly the same.

---

## **Automated processing for SWIFT message txt files**

This changes the architecture entirely for the better. Having a cron job automatically drop `.txt` files into a secure network directory is a massive leap forward in automation and security. It completely removes the risk of human error or malicious editing at the user-upload level.

Instead of building a manual UI upload screen, you will now build a **Scheduled Service (Batch Processor)**.

Here is the revised, fully automated architectural blueprint for your `.txt` file ingestion pipeline.

---

### Phase 1: The Polling Service (Ingestion)
Since the cron job drops the files, System needs to be configured to "wake up" and check that directory. 

1.  **Define a Moqui Service Job:** You will create an XML service (e.g., `trade.swift.IngestionServices.xml`) with a verb like `poll` and noun `SwiftDirectory`.
2.  **Configure the Scheduler:** In your Moqui database (or seed data), you configure a `ServiceJob` record to run this service on a cron schedule (e.g., every 5 minutes).
3.  **The Directory Handshake:** When the service wakes up, it scans the designated `/inbound_swift/` directory for any files ending in `.txt`.

### Phase 2: The "Read & Move" Protocol (Critical for Batch)
When dealing with directory polling, the biggest system risk is **Double Processing** (reading the same file twice) or **File Locking** (reading a file while the cron job is only halfway done writing it).

Your Moqui service must execute this strict loop for every file it finds:
1.  **Check Lock:** Verify the file size hasn't changed in the last 2 seconds (ensuring the cron job has finished writing).
2.  **Read to Memory:** Load the `.txt` file content into memory.
3.  **Move to Archive:** *Immediately* move the physical `.txt` file out of the `/inbound_swift/` directory and into an `/archive_swift/processed/` directory. If the file fails to parse later, move it to `/archive_swift/error/`. **Never leave files in the hot directory.**
4.  **Audit Log:** Insert a record into your `InboundSwiftRaw` database entity containing the original file name, timestamp, and a hash of the content.

### Phase 3: The Parsing & Correlation Engine (Prowide Core)
Now that the file is safely in memory and archived, the logic flows exactly as we discussed before, but entirely headless.

1.  **Parse:** Pass the text string to Prowide Core (`SwiftMessage.parse(content)`).
2.  **Extract:** Grab Tag `21` (Related Reference) and Tag `20` (Sender's Reference).
3.  **Correlate:** The system queries your `ImportLC` entity looking for the LC number in Tag 21.

### Phase 4: Staging for Operations (The UI Handoff)
Because the system is running headless in the background, it needs a way to hand the data back to your human operations team.

*   **The Happy Path (Match Found):**
    If Tag 21 matches an active LC, the service creates a record in your `TradeInboxItem` entity, linking the parsed data directly to the LC. The next time the Maker logs into Moqui, they will see an alert on their dashboard: *"3 New Inbound Messages Ready for Review."*
*   **The Orphan Path (No Match):**
    If Tag 21 is missing or contains a typo, the Moqui service does not crash. It creates a record in the `TradeOrphanQueue` entity. 
*   **System Alerts:** If a file fails to parse entirely (e.g., corrupt text formatting), the Moqui service should trigger an email or system notification to the IT Admin, referencing the file in the `/archive_swift/error/` directory.

---

### A Note on File Concatenation (Batch Splitting)
Often, when a bank gateway outputs SWIFT messages to a `.txt` file via cron job, it doesn't output one message per file. It outputs a "batch" file containing dozens of SWIFT messages concatenated together, separated by a specific delimiter (usually a `$` or `{1:` block start).

**If your files are batched:**
Prowide Core has a built-in utility specifically for this. Before you call `SwiftMessage.parse()`, you must use Prowide's `SwiftParser` iterator to break the massive `.txt` string into individual message blocks, and then loop through them.

By utilizing Moqui's native Service Jobs and moving the physical files instantly upon reading, your system will be highly resilient, secure, and ready to plug directly into an MQ API queue later without rewriting the core business logic.