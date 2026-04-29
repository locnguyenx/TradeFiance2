# **SWIFT MESSAGE SPECIAL TAGS**

## **The SWIFT Multiple Format Options**

In SWIFT architecture, when a field is designated with a lowercase letter (e.g., `5Xa` or `4Xa`), it means the network allows the sender to choose the format based on the information available. 

From the **SWIFT Standards Release Guide (SRG)**, these fields belong to the **40-series** (Trade Finance specific) and **50-series** (Universal Financial Institution / Party) tags. 

Here is the master key for what those option letters actually mean across the SWIFT network, followed by the exhaustive list of every party field that uses them.

### The SWIFT Format Options (The "Letter" Key)
Before listing the fields, you need to know what the letters dictate to the database:
* **Option A:** Identifier Code (A mandatory SWIFT BIC). *Highly preferred for STP.*
* **Option B:** Location (A specific physical branch or city name, without full address).
* **Option C:** Clearing Code (A national routing code, like a US ABA number or UK Sort Code).
* **Option D:** Name and Address (Unformatted, free text, max 4 lines of 35 chars).
* **Option F:** Formatted Name and Address (Highly structured with specific sub-tags for City, Country, Zip. Used to satisfy strict FATF Anti-Money Laundering rules).
* **Option K:** Name and Address (Specific only to Tag 50, functions similarly to D).

---

### Exhaustive List: Trade Finance Specific Party Fields (40-Series)
These fields are used almost exclusively in Category 4 (Collections) and Category 7 (Letters of Credit and Guarantees).

| SWIFT Tag | Description / Usage | Available Formatting Options |
| :--- | :--- | :--- |
| **41a** | **Available With... By...**<br>Specifies the bank authorized to negotiate or pay the LC. | **41A:** SWIFT BIC.<br>**41D:** Name and Address. |
| **42a** | **Drawee**<br>The party on whom the drafts (Bills of Exchange) must be drawn. | **42A:** SWIFT BIC.<br>**42C:** Clearing / Routing Code.<br>**42D:** Name and Address. |

*(Note: Tags 42M and 42P exist, but they are used for mixed payment instructions, not party identification).*

---

### Exhaustive List: Universal Financial Institution & Party Fields (50-Series)
These are the backbone of the SWIFT network. They are used in Trade Finance (MT 700), Payments (MT 103, 202), and Treasury (MT 300).

| SWIFT Tag | Description / Usage | Available Formatting Options |
| :--- | :--- | :--- |
| **50a** | **Ordering Customer**<br>The entity instructing the payment or transaction (The Applicant). | **50A:** SWIFT BIC.<br>**50C:** Clearing Code.<br>**50F:** Formatted Name & Address (Strict AML format).<br>**50G, 50H:** Specialized ISO identifiers.<br>**50K:** Name & Address. |
| **51a** | **Applicant Bank**<br>The bank initiating the LC on behalf of the Applicant. | **51A:** SWIFT BIC.<br>**51C:** Clearing Code.<br>**51D:** Name and Address. |
| **52a** | **Ordering Institution / Drawer Bank**<br>The bank ordering a payment on behalf of a client. | **52A:** SWIFT BIC.<br>**52B:** Location.<br>**52C:** Clearing Code.<br>**52D:** Name and Address. |
| **53a** | **Sender's Correspondent / Reimbursing Bank**<br>The Nostro account holding bank used by the sender. | **53A:** SWIFT BIC.<br>**53B:** Location.<br>**53C:** Clearing Code.<br>**53D:** Name and Address. |
| **54a** | **Receiver's Correspondent**<br>The account holding bank used by the receiver. | **54A:** SWIFT BIC.<br>**54B:** Location.<br>**54D:** Name and Address. |
| **55a** | **Third Reimbursement Institution**<br>A tertiary bank used to settle claims. | **55A:** SWIFT BIC.<br>**55B:** Location.<br>**55D:** Name and Address. |
| **56a** | **Intermediary Institution**<br>A bank through which funds must pass between correspondent banks. | **56A:** SWIFT BIC.<br>**56C:** Clearing Code.<br>**56D:** Name and Address. |
| **57a** | **Account With Institution / 'Advise Through' Bank**<br>The bank where the final Beneficiary maintains their account. | **57A:** SWIFT BIC.<br>**57B:** Location.<br>**57C:** Clearing Code.<br>**57D:** Name and Address. |
| **58a** | **Beneficiary Institution / Negotiating Bank**<br>The ultimate bank receiving the funds or the LC. | **58A:** SWIFT BIC.<br>**58B:** Location.<br>**58C:** Clearing Code.<br>**58D:** Name and Address. |
| **59a** | **Beneficiary**<br>The ultimate corporate entity or person receiving the funds/LC. | **59:** Name and Address (Unlike others, it drops the 'a' for Option D).<br>**59A:** SWIFT BIC.<br>**59F:** Formatted Name & Address (Strict AML format). |

---

### Exhaustive List: Treasury & Securities Party Fields (80 & 90-Series)
While these are outside the direct scope of the Import LC module, for absolute SRG completeness, SWIFT uses the `8Xa` and `9Xa` series for complex Treasury, FX, and Securities messaging (Categories 3, 5, and 6). 

* **81a, 82a, 83a, 84a, 85a, 86a, 87a, 88a:** These represent generic "Party A", "Party B", "Deliverer", "Receiver", etc., in Treasury trades. They utilize **Options A, B, D, and J** (where J includes an internal Party ID alongside the Name/Address).
* **95a (Party):** Used heavily in ISO 15022 Securities messages (like MT 541). It utilizes completely different formatting letters: **Options P, Q, R, and S** to designate specific institutional investors and clearers.

### Implementation Takeaway
When building out the database schema (`TradeParty`), you do not need to create separate columns for "Option A" and "Option D". 

You simply need a generic `TradeParty` entity that stores the `SWIFT_BIC`, `CLEARING_CODE`, and `POSTAL_ADDRESS`. System generation service (e.g., `SwiftServices.xml`) will evaluate the data present in that entity at runtime. 
* If `SWIFT_BIC` is not null $\rightarrow$ Generate Tag **57A**.
* If `SWIFT_BIC` is null but `CLEARING_CODE` exists $\rightarrow$ Generate Tag **57C**.
* If both are null $\rightarrow$ Generate Tag **57D** using the `POSTAL_ADDRESS`.

---

## **Display rule in SWIFT MT messages**

In the SWIFT MT messaging standard, the different formatting options for a specific field are **mutually exclusive**. You can only choose exactly *one* option for a given data element within a message block.

Here is the breakdown of how this rule works and why it is enforced.

### 1. The "Either/Or" Rule
When the SWIFT Standards Release Guide (SRG) defines a field like **50a** (Ordering Customer), the lowercase "a" indicates that you must make a choice based on the data you have. 

You must select **either** 50A, **or** 50C, **or** 50F, **or** 50K. 

If system generates a message containing both `:50A:` and `:50K:`, the SWIFT network will immediately reject the message with a fatal **NACK** (Negative Acknowledgement), usually citing a format or sequence error.

### 2. Why SWIFT Enforces This
The SWIFT network is designed for Straight-Through Processing (STP) by automated core banking systems across the globe. 

If a receiving bank's system parsed a message and found both a `:50A:` (which contains a BIC) and a `:50K:` (which contains a free-text name and address), the automated system wouldn't know which one to trust if they contradicted each other. To prevent legal ambiguity and routing failures, SWIFT forces the sender to commit to a single identifier.

### 3. How to Handle "I have both" Scenarios
A common dilemma for operations teams is: *"I have the recipient's SWIFT BIC, but my compliance team also wants me to include their full physical address in the message. Can I use A and D?"*

Because you cannot use both, system must apply a strict hierarchy (which we mapped out earlier):

* **The Golden Rule:** If you have a valid SWIFT BIC, you **always** use Option A. You do not need to send the physical address because the receiving bank will automatically look up the physical address associated with that BIC in their internal SWIFT Directory (the BIC Plus database).
* **The Fallback:** If you absolutely must send the full text name and address because the party does not have a BIC (or the specific local regulator demands it printed on the message), you must drop Option A entirely and exclusively use Option D (or K/F depending on the specific tag). 

### System Implementation Takeaway
In the Moqui integration layer, the SWIFT builder logic must use `if/else` statements, never sequential `add` statements for the same party role. 

**Correct Logic (Pseudo-code):**
```java
if (party.swiftBic != null) {
    message.addField(new Field50A(party.account, party.swiftBic));
} else if (party.clearingCode != null) {
    message.addField(new Field50C(party.account, party.clearingCode));
} else {
    message.addField(new Field50K(party.account, party.nameAndAddress));
}
```
---

### What tags belong in scope by this rule?
This is one of the most confusing quirks of the SWIFT messaging standard: **the reuse of numbers.**

The "mutually exclusive" rule applies **only to fields designated with a lowercase "a" in the SWIFT rulebook.** Here is exactly how SWIFT differentiates between "Format Options" (mutually exclusive) and "Distinct Fields" (which just happen to share the same number).

#### 1. The Lowercase "a" = Format Options (Mutually Exclusive)
When the SWIFT Standards Release Guide (SRG) writes a field with a lowercase letter—such as **`50a`**, **`53a`**, or **`41a`**—it means *"This is a single piece of business data, but you must choose ONE format option."*

For these tags, the letter represents the **format** (A = BIC, D = Name/Address). 
* You **must** pick exactly one (e.g., 53A). 
* You **cannot** use 53A and 53D in the same message. 
* This is where the strict mutually exclusive rule applies.

#### 2. The Uppercase Letter = Distinct Fields (Not Mutually Exclusive)
When SWIFT defines a field with a hardcoded uppercase letter—such as **`42M`**, **`42P`**, or **`39C`**—that letter does **not** stand for a formatting option. 

Instead, SWIFT ran out of numbers (from 1 to 99) to describe all the complexities of global trade, so they started attaching uppercase letters to numbers to create entirely **new, distinct fields**. 

Let’s look at the **"42 Family"** in an MT 700 as the perfect example:

* **`42C` (Drafts at):** This is a distinct field used to state the Tenor (e.g., "90 DAYS AFTER SIGHT"). 
* **`42a` (Drawee):** This is a Party field. Because of the lowercase "a", it has mutually exclusive format options. You must choose exactly one: **42A** (BIC) or **42D** (Name & Address).
* **`42M` (Mixed Payment Details):** This is a completely separate field used only if the LC is paid in mixed tranches (e.g., "50% at sight, 50% at 90 days").
* **`42P` (Deferred Payment Details):** Another completely separate field used for specific maturity date calculations.

In a single MT 700 message, it is perfectly legal to have `:42C:`, `:42A:`, and `:42M:` all present at the same time. They are not conflicting options; they are three completely different pieces of data that just happen to share the number 42. 

#### 3. Summary Rule for the System Architecture
When building a service to generate SWIFT messages, use this exact rule of thumb:

* **Rule 1:** If the SWIFT spec says **`Tag + lowercase 'a'`** (like 50a, 57a, 59a), treat it as an **XOR (Exclusive OR)** condition in the code. The system must select one and only one letter variant.
* **Rule 2:** If the SWIFT spec says **`Tag + UPPERCASE LETTER`** (like 45A, 46A, 71D, 42M), treat it as a **unique, standalone database column**. The letter is just part of its permanent name, and it does not conflict with other tags that share the same number.

---

# **TRADEPARTY ENTITY IN DETAILS**

## **Roles used in MT messages**

To properly structure the `TradeParty` entity in the system, we must map exactly which roles participate in which SWIFT messages, and enforce strict validation rules on the data fields (BIC vs. Name/Address) based on the SWIFT Standard Release Guide (SRG). 

Here is the comprehensive summary of `TradeParty` role utilization across the Trade Finance MT messages, followed by the strict field-level constraints.

### 1. `TradeParty` Roles Utilized per MT Message Type

Not all messages require all parties. The system must know which `TradeParty` records to extract from the `TradeInstrument` container when assembling a specific message.

| MT Message Type | Commercial Parties Extracted | Banking & Routing Parties Extracted |
| :--- | :--- | :--- |
| **MT 700 (Issue LC)** | Applicant, Beneficiary | Applicant Bank, Advise Through Bank, Confirming Bank, Reimbursing Bank |
| **MT 707 (Amendment)** | Beneficiary *(Only if name/address is amended)* | Applicant Bank, Advise Through Bank, Confirming Bank, Reimbursing Bank |
| **MT 740 (Auth to Reimburse)** | Beneficiary | Reimbursing Bank, Negotiating Bank |
| **MT 747 (Amend Reimburse)** | None | Reimbursing Bank |
| **MT 750 / 734 / 752 / 732** | None *(Handled via references)* | Presenting Bank |
| **MT 202 (Settlement Payment)**| None | Ordering Institution, Sender's Correspondent, Receiver's Correspondent, Intermediary, Beneficiary Institution |


This table maps exactly which role populates which specific Tag in the SWIFT message block.

| `TradeParty` Role | MT Message Type | Specific SWIFT Tag Populated |
| :--- | :--- | :--- |
| **Applicant** | MT 700 (Issue LC) | **50** (Applicant) |
| **Beneficiary** | MT 700 (Issue LC) <br> MT 707 (Amendment) <br> MT 740 (Auth to Reimburse) | **59** (Beneficiary) <br> **59** (Beneficiary) - *Only if amending name/address* <br> **59 / 59A** (Beneficiary) |
| **Applicant Bank** | MT 700 (Issue LC) | **51a** (Applicant Bank) |
| **Advising Bank** | MT 700, 707, 740 <br> MT 750, 734, 752 | **No Tag (Basic Header Block 2)** - They are the destination address of the message. <br> **Tag 21** (Related Reference) often contains their reference number. |
| **Advise Through Bank** | MT 700 (Issue LC) | **57a** ('Advise Through' Bank) |
| **Confirming Bank** | MT 700 (Issue LC) | **58a** (Requested Confirming Bank) |
| **Reimbursing Bank** | MT 700, 707 <br> MT 740, 747 | **53a** (Reimbursing Bank) <br> **No Tag (Basic Header Block 2)** - They are the destination of the 740/747. |
| **Negotiating / Presenting Bank** | MT 700 (Issue LC) <br> MT 740 (Auth to Reimburse) <br> MT 202 (Settlement) | **41a** (Available With... By...) <br> **58a** (Negotiating Bank) <br> **58a** (Beneficiary Institution) |
| **Intermediary Bank** | MT 202 (Settlement) | **56a** (Intermediary Institution) |
| **Sender's Correspondent** | MT 202 (Settlement) | **53a** (Sender's Correspondent) |
| **Receiver's Correspondent**| MT 202 (Settlement) | **54a** (Receiver's Correspondent) |

**Notes:** The SWIFT Standards Release Guide (SRG) has a notorious quirk: it often does not have a dedicated data tag for the "Advising Bank," because the Advising Bank is usually the *Receiver* of the message itself. Furthermore, the Intermediary Bank plays a massive role in actual money movement (Category 2 messages) but is often overlooked in the pure Letter of Credit (Category 7) documentation.

---

### 2. Mandatory, Optional, and Not Allowed Fields by Role

The `TradeParty` database entity contains fields for `swiftBic`, `legalName`, `postalAddress`, and `accountNumber`. 

However, SWIFT strictly dictates which of these fields are allowed to be populated in the final message block based on the role and the "Option A vs. Option D" rule. The system must enforce these rules at the service layer during message generation.

#### Category A: Commercial Parties (The "Fixed" Tags)
**IMPORTANT:** These roles are only applicable for Commercial Parties

In an MT 700, the Applicant and Beneficiary do not use the "a" format options. They rely entirely on physical names and addresses.

| TradeParty Role | `accountNumber` | `legalName` & `postalAddress` | `swiftBic` |
| :--- | :--- | :--- | :--- |
| **Applicant** *(Tag 50)* | **Optional** | **Mandatory** *(Max 4x35 chars)* | **Not Allowed** *(MT 700 strictly forbids a BIC for the Applicant)* |
| **Beneficiary** *(Tag 59)* | **Optional** | **Mandatory** *(Max 4x35 chars)* | **Not Allowed** *(Except in rare MT 740 scenarios using 59A)* |

#### Category B: Banking & Routing Parties (The "a" Option Tags)
**IMPORTANT:** These roles are only applicable for Bank Parties

This category covers all banks: **Applicant Bank, Advise Through Bank, Confirming Bank, Reimbursing Bank, Negotiating Bank, Correspondents, and Intermediaries.**

Because these are financial institutions, they fall under the strict "Either/Or" formatting rule (Option A vs. Option D). The system logic must branch based on the presence of the `swiftBic`.

**Scenario 1: The Bank has a SWIFT BIC (Option A)**
*Applies to: Applicant Bank (51A), Advise Through Bank (57A), Confirming Bank (58A), Reimbursing Bank (53A), Negotiating Bank (41A/58A), Intermediary Bank (56A), Correspondents (53A/54A)*

| Format Option | `swiftBic` | `legalName` & `postalAddress` | `accountNumber` |
| :--- | :--- | :--- | :--- |
| **Option A (Identifier Code)**| **Mandatory** (8 or 11 chars) | **Not Allowed** (Must be strictly Null in system) | Optional |

If the `TradeParty` record contains a `swiftBic`, the system must suppress the physical address(If you send Option A, including the address will cause a SWIFT NACK).

**Scenario 2: The Bank does NOT have a SWIFT BIC (Option D)**
*Applies to: Applicant Bank (51D), Advise Through Bank (57D), Confirming Bank (58D), Reimbursing Bank (53D), Negotiating Bank (41D/58D), Intermediary Bank (56D), Correspondents (53D/54D)*

If the `TradeParty` record does not have a `swiftBic` (or the bank routes via a domestic clearing code), the system falls back to the physical address.

| TradeParty Role | `accountNumber` | `legalName` & `postalAddress` | `swiftBic` |
| :--- | :--- | :--- | :--- |
| **All Banking Roles** | **Optional** | **Mandatory** *(Max 4x35 chars)* | **Not Allowed** *(System must be completely null for this field)* |

### System Implementation Rule for system
To manage this complexity without creating a convoluted database schema, the `TradeParty` entity should allow all fields to be `nullable="true"` at the database level. 

The validation must occur dynamically inside the `Swift Services` generation logic. When extracting a Banking Party, the system evaluates:
1.  Is `swiftBic` populated? If Yes $\rightarrow$ Extract `swiftBic` + `accountNumber`. Generate Option A. Ignore name/address.
2.  Is `swiftBic` null? If Yes $\rightarrow$ Validate that `legalName` + `postalAddress` are not null. Extract them + `accountNumber`. Generate Option D.

---

## Party - Instrument relationship

It is actually the standard operating procedure in global trade that 1 party can play as different role in Trade Instrument.

In real-world Trade Finance, legal entities (companies and banks) are highly dynamic. Their "role" is not a permanent identity; it is simply the hat they are wearing for one specific transaction.

Here is how these scenarios play out in reality and how the system architecture must be designed to handle them.

### Scenario 1: A Company Changing Roles Across Different LCs
A standard trading company will constantly flip between being an Applicant and a Beneficiary, depending on which side of the supply chain they are operating on.

* **Standard Import/Export:** "Vietnam Textiles JSC" is the **Applicant** on an Import LC to buy raw cotton from India. Next week, they act as the **Beneficiary** on an Export LC to sell finished shirts to a buyer in Europe.
* **Back-to-Back LCs:** This happens simultaneously. A middleman broker receives a master Export LC (acting as **Beneficiary**). They immediately take that LC to our bank and ask us to issue a baby Import LC to their actual supplier, using the master LC as collateral. In the baby LC, the middleman is now the **Applicant**.

### Scenario 2: A Bank Playing Multiple Roles in the SAME LC
To save on SWIFT fees and reduce operational friction, banks frequently consolidate roles. It is extremely common for one physical bank to execute three or four different roles on a single LC.

* **Advising Bank = Negotiating Bank:** The Issuing Bank sends the LC to Citibank London (Advising Bank). The LC states that it is restricted for negotiation specifically at Citibank London. Thus, they wear both hats.
* **Advising Bank = Confirming Bank = Negotiating Bank:** A Vietnamese bank issues an LC to a US exporter. The US exporter wants a local guarantee, so the LC asks JP Morgan NY to add its confirmation. JP Morgan advises the LC to the beneficiary, adds their legal guarantee, and eventually examines the documents and pays the client. They are acting as Advising, Confirming, and Negotiating bank simultaneously.
* **Confirming Bank = Reimbursing Bank:** If JP Morgan confirms the LC (taking on the risk), the Vietnamese Issuing Bank will almost always designate JP Morgan as the Reimbursing Bank as well, allowing JPM to directly debit the Vietnamese bank's USD Nostro account the moment they pay the Beneficiary.

---

### The System Implementation: The "Party-Role Pattern"
Because entities change hats constantly, you must **never** create duplicate records in the database (e.g., do not create one record for "Citibank - Advising" and a separate record for "Citibank - Reimbursing"). 

**Note:** In the Moqui Framework, this is solved using the **Party-Role Pattern** (specifically utilizing `mantle.party.Party` and `mantle.party.PartyRole`).

**1. The Master Data (The Entity)**
You maintain a single, golden record in the central Party directory.
* `Party ID: 1001` | `Legal Name: JP Morgan Chase NY` | `SWIFT: CHASUS33`
* `Party ID: 2002` | `Legal Name: Vietnam Textiles JSC` | `Address: District 1, HCMC`

**2. The Transactional Data (The Hat)**
The `ImportLc` or `TradeInstrument` entity does not store the bank's name. It stores foreign keys linking back to the master Party ID, alongside the specific role they are playing for *that specific LC*.
Example: a TradeInstrument (instrumentId="TF-2026-001")
- Applicant partyId="2002"
- Applicant Bank partyId="1002"
- Advising Bank partyId="1001"
- Confirming Bank partyId="1001" 
- Reimbursing Bank partyId="1001" 

**System UI Rule for Consolidation:**
When the Operations Maker is filling out the LC Issuance screen, if they select "JP Morgan" as the Advising Bank, the system UI should offer a simple checkbox: `[x] Make Advising Bank the Negotiating Bank`. 

If checked, the system automatically links that same Master Party ID to both roles in the database. When generating the SWIFT MT 700, the system will populate Tag 57a (Advise Through) and Tag 41a (Available With) using the exact same SWIFT BIC, perfectly reflecting real-world banking practices.

---

## **Requirements for banks to participate LC roles**

In international trade finance, banks cannot simply route legally binding documents and millions of dollars to any random institution. The global correspondent banking network relies on a strict web of established relationships, technical handshakes, and regulatory approvals.

Whether our bank needs a direct relationship with Bank X—and what is required of Bank X—depends entirely on the **role** Bank X is being asked to play. 

Here is how international practice and regulation dictate these relationships.

### Part 1: Does Our Bank Need a Specific Relationship with Bank X?

The short answer is: **It depends on the role, but at a minimum, you need a routing path.**

#### 1. The Messaging Relationship (SWIFT RMA)
To send a secure, authenticated Trade Finance message (like an MT 700 or MT 799) directly to Bank X, our bank and Bank X must have an active **SWIFT RMA (Relationship Management Application)** in place. An RMA is a digital handshake where both banks authorize each other to exchange specific message types over the SWIFT network.
* **If you have an RMA:** You can designate Bank X as the Advising Bank (Tag 57a) and send the MT 700 directly to them.
* **If you do NOT have an RMA:** You cannot send messages directly to Bank X. You must find a mutual third-party bank that *both* you and Bank X have an RMA with. This third-party bank becomes the **"Advise Through Bank"** (Tag 57a), and Bank X is relegated to the final **"Advising Bank"** role.

#### 2. The Financial Relationship (Nostro/Vostro Accounts)
If Bank X is going to handle the actual movement of money, an account relationship is usually required.
* **Role - Reimbursing Bank:** If you designate Bank X as the Reimbursing Bank (Tag 53a), our bank **must** hold a funded foreign currency account (a Nostro account) with Bank X. You cannot authorize them to reimburse claims if you do not have money sitting in their vault.
* **Role - Advising / Negotiating Bank:** You do not need a direct account relationship with Bank X to advise or negotiate an LC. If they pay the Beneficiary, they will simply claim the funds from our designated Reimbursing Bank. 

#### 3. The Risk Relationship (Bank-to-Bank Credit Limits)
Trade finance involves massive credit exposure. Banks do not guarantee each other's LCs without pre-approved credit facilities, known as **FI (Financial Institution) Limits**.
* **Role - Confirming Bank:** If you ask Bank X to add their confirmation (guarantee) to our LC, Bank X takes on our bank's default risk. Therefore, Bank X must have a pre-approved, active credit limit established *for our bank*. If they don't trust our bank's creditworthiness, they will refuse to add confirmation.
* **Role - Issuing Bank (Export LC side):** If Bank X issues an LC and asks our bank to confirm it, our bank must have an approved credit limit for Bank X. 

---

### Part 2: Special Requirements for Bank X

Under international banking regulations (such as those guided by the FATF and the Wolfsberg Group), Bank X cannot participate in the LC lifecycle unless they meet strict institutional criteria.

#### 1. Strict KYC and AML Clearance
Before our bank can establish an RMA, open a Nostro account, or extend a credit limit to Bank X, Bank X must pass our bank's internal Financial Institution KYC (Know Your Customer) process. 
* The compliance team must verify Bank X's ownership structure, their anti-money laundering (AML) controls, and their regulatory standing.
* **Shell Bank Prohibition:** Under international law, our bank is strictly prohibited from dealing with "shell banks" (banks with no physical presence or unaffiliated with a regulated financial group).

#### 2. Sanctions Screening
Bank X must continuously clear international sanctions lists (OFAC, UN, EU). 
* If Bank X is a sanctioned entity, or operates in a comprehensively sanctioned jurisdiction, our bank cannot route an LC to them, even if it is just to advise a document. Doing so would result in severe regulatory fines.

#### 3. SWIFT Membership & BIC
To formally execute the automated roles defined in UCP 600 and URR 725, Bank X must be a registered member of the SWIFT network with a valid 8 or 11-character **Business Identifier Code (BIC)**. While it is technically possible to advise an LC via physical courier to a non-SWIFT bank, it is highly discouraged in modern Tier-1 practice due to fraud risk and operational friction.

#### 4. UCP 600 Recognition
While not a statutory law, Bank X must be a recognized financial institution capable of undertaking the obligations defined in the ICC's UCP 600. If an entity is not legally recognized as a bank or authorized financial institution in their home jurisdiction, they cannot legally issue, confirm, or negotiate a Letter of Credit.

***

### The Intermediary Bank (Requirements & Context)

To complete the matrix, we must define the **Intermediary Bank**. This bank rarely appears in the MT 700 (Letter of Credit issuance). Instead, it is highly critical during **Process 3.4 (Settlement)** when generating the MT 202 to actually move the money.

**The Business Scenario:**
our bank (the Issuing Bank) needs to pay Citibank London (the Presenting Bank) $500,000 USD. 
* Our USD Nostro account is at JP Morgan NY (Sender's Correspondent).
* Citibank London's USD account is at Bank of America NY (Receiver's Correspondent). 
* *However*, JP Morgan and Bank of America do not have a direct clearing arrangement for this specific type of trade transfer. They need a bridge. That bridge is the **Intermediary Bank** (e.g., The Federal Reserve Bank or another clearing bank like Standard Chartered).

---

### Bank Relationship & Requirement Matrix

Here is the consolidated summary of relationship and regulatory requirements for each bank role in the Letter of Credit lifecycle. 

This matrix serves as the exact business logic the system's compliance and routing engine must evaluate before allowing a Maker to successfully submit a transaction.

| Bank Role | Primary Function in LC | Messaging (SWIFT RMA) | Financial (Accounts) | Risk (FI Credit Limits) |
| :--- | :--- | :--- | :--- | :--- |
| **Advising Bank** | Authenticates and forwards the LC to the Beneficiary. | **Mandatory.** Must have a direct RMA with the Issuing Bank (or receive it via an 'Advise Through' bank). | **None.** No Nostro/Vostro account is required. | **None.** They take no financial risk; they merely act as a secure messenger. |
| **'Advise Through' Bank** | An intermediary messenger used when the Issuing Bank has no RMA with the Advising Bank. | **Mandatory.** Must have an RMA with *both* the Issuing Bank and the Advising Bank. | **None.** | **None.** |
| **Confirming Bank** | Adds its own legal guarantee to pay the Beneficiary, assuming the Issuing Bank's default risk. | **Mandatory.** Must have a direct RMA with the Issuing Bank. | **Optional.** Usually settles via a designated Reimbursing Bank, but may hold a Vostro account. | **Mandatory.** They must have an approved, available credit limit *for the Issuing Bank* before adding confirmation. |
| **Reimbursing Bank** | Holds the Issuing Bank's funds and pays claims to the Negotiating Bank upon request. | **Mandatory.** Must receive the MT 740 Authorization to Reimburse. | **Mandatory.** The Issuing Bank must hold a funded foreign currency (Nostro) account with them. | **None.** Unless the Issuing Bank requires an overdraft facility to fund the payment. |
| **Negotiating / Presenting Bank** | Examines the physical documents on behalf of the Beneficiary and forwards them to the Issuing Bank. | **Mandatory.** Needed to send the MT 732/750/742 claim and discrepancy messages. | **Optional.** They will claim funds from the Reimbursing Bank or ask for direct T/T settlement. | **None.** |
| **Intermediary Bank (Tag 56a)** | Acts as a bridge to route funds between the Sender's Correspondent and the Receiver's Correspondent during final settlement. | **Not Required by Issuing Bank.** The RMA must exist between the *Sender's Correspondent* and the Intermediary. our bank just dictates the routing instructions. | **None.** You do not hold an account with the Intermediary. They hold accounts for the Correspondents. | **None.** They do not assume default risk for the LC. |

**System Implementation Note for Intermediaries:** Unlike the Advising or Reimbursing Banks, which are established during Process 3.1 (Issuance), the Intermediary Bank is usually dynamically captured during **Process 3.4 (Settlement)**. When the Maker is processing the final payment, the UI must allow them to input the Intermediary Bank (Tag 56a) based on the specific payment routing instructions provided by the Presenting Bank in their MT 742/750 claim message.

*(Note: **All** roles listed above universally require strict KYC/AML clearance, active SWIFT membership with a valid BIC, and continuous automated Sanctions screening.)*

---

### System Implementation: Master Data Flags
To enforce these requirements seamlessly without hardcoding logic, the Trade Party (which extends `mantle.party.Party` in Moqui) should utilize simple boolean flags or related entity checks for every registered financial institution:

1.  **`hasActiveRMA` (Boolean):** If `False`, the system UI must block the Maker from selecting this bank as the "Advising Bank" and force them to input an "Advise Through Bank."
2.  **`nostroAccountRef` (String/FK):** If the Maker selects a bank as the "Reimbursing Bank," the system checks if this field is populated. If null, it throws a validation error: *"Cannot designate as Reimbursing Bank: No active Nostro account found."*
3.  **`fiLimitAvailable` (Currency):** If our bank is acting as the Confirming Bank on an *Export LC*, the system must check this balance against the foreign Issuing Bank's profile before allowing authorization.

