# Trade Finance System: User Guide - LC Amendment (R8.5)

## 1. Introduction
This guide covers the **LC Amendment** functionality introduced in Phase 2 of the Trade Finance System. LC Amendments allow banks to modify terms of an already issued Letter of Credit, such as increasing the amount, extending the expiry date, or changing the beneficiary.

The system follows international standards:
- **UCP 600 Article 10**: Amendments to Documentary Credits
- **SWIFT MT707**: Amendment to a Documentary Credit

---

## 2. LC Amendment Overview

### 2.1 What Can Be Amended?
The following fields can be modified via an amendment:
- **Amount**: Increase or decrease the LC value
- **Expiry Date**: Extend or advance the expiration date
- **Beneficiary Details**: Name, address, party ID
- **Shipment Details**: Latest shipment date, loading/discharge ports
- **Documentary Requirements**: Description of goods, documents required
- **Additional Conditions**: Special conditions or amendments to existing terms

### 2.2 What CANNOT Be Amended?
The following fields are **immutable** and cannot be changed:
- **LC Number**: The original LC reference
- **Applicant**: The importer/buyer party
- **Currency**: The LC currency
- **Issuing Bank**: The bank that issued the LC
- **Advising Bank**: The bank that advises the LC to the beneficiary

---

## 3. Creating an LC Amendment

### 3.1 Prerequisites
To create an amendment, the LC must be in **Issued** status.

### 3.2 Steps to Create Amendment
1. Navigate to the **Import LC Dashboard**
2. Search for the LC using **Find Import LC**
3. Click on the LC Number to open **LC Detail** screen
4. Go to the **Amendments** tab
5. Click **"Request Amendment"** button
6. The system creates an **Amendment Draft** with a shadow copy of all current LC terms

### 3.3 Amendment Draft Details
When an amendment is created:
- A new **LcAmendment** record is generated
- All current LC fields are copied as "shadow" values
- An **Amendment Lock** is acquired to prevent concurrent amendments
- The amendment status is set to **Draft**
- A workflow **Request** is created for tracking

---

## 4. Editing Amendment Details

### 4.1 Modifying Shadow Fields
In the **Amendment Detail** screen:
1. Navigate to the field you want to change
2. Enter the new value in the amendment-specific field
3. The system tracks both old and new values
4. Add remarks explaining the reason for the amendment

### 4.2 Validation Rules
- Fields marked as **immutable** cannot be edited
- Amount increases may require additional provisions
- Expiry date extensions require justification

---

## 5. Amendment Approval Workflow

### 5.1 Workflow Stages
The amendment follows a **3-level approval** process:

```
CSR → Supervisor → IPC → Beneficiary Response → Confirm
```

### 5.2 Stage 1: CSR Submission
1. After editing the amendment details, click **"Submit for Review"**
2. System validates all required fields are complete
3. Amendment status changes to **Submitted**
4. Lock is released for other operations
5. Notification sent to Supervisor

### 5.3 Stage 2: Supervisor Review
1. Supervisor reviews the proposed changes
2. Supervisor can:
   - **Approve**: Forward to IPC for final approval
   - **Reject**: Return with rejection reason
3. Supervisor validates:
   - Credit limit compliance
   - Collateral impact
   - Risk assessment

### 5.4 Stage 3: IPC Approval
1. IPC (International Processing Center) Officer reviews
2. IPC can:
   - **Approve**: Generate SWIFT MT707 message
   - **Reject**: Return with rejection reason
3. On approval:
   - **MT707** SWIFT message is generated
   - Beneficiary is notified via Advising Bank
   - Amendment status changes to **IPC Approved**

---

## 6. Beneficiary Response

### 6.1 Acceptance Window
Once the MT707 is sent, the beneficiary has a window to respond (per UCP 600 Article 10).

### 6.2 Beneficiary Accepts
If the beneficiary accepts the amendment:
1. System records the acceptance response
2. Amendment confirmation status changes to **Accepted**
3. Shadow values are applied to the **Master LC**
4. LC status changes to **Amended**
5. Amendment number is incremented
6. Financials are updated (charges, provisions)

### 6.3 Beneficiary Rejects
If the beneficiary rejects the amendment:
1. System records the rejection
2. Original LC terms are **preserved**
3. Amendment confirmation status changes to **Rejected**
4. No changes are applied to the Master LC

### 6.4 Acceptance Window Expires
If no response is received within the window:
1. System treats lack of response as rejection
2. Amendment confirmation status changes to **Expired**
3. Original LC terms remain in effect

---

## 7. Confirming Amendment Application

### 7.1 Automatic vs Manual Confirmation
After beneficiary response (acceptance), the amendment can be confirmed:
- **Automatic**: System applies changes immediately
- **Manual**: IPC officer reviews and confirms

### 7.2 What Happens on Confirmation
1. All shadow fields are copied back to Master LC
2. Amendment number is incremented by 1
3. LC status transitions to **Amended**
4. Financial updates are processed:
   - Additional charges collected (if amount increased)
   - Provisions adjusted (if amount changed)
5. GL entries are posted for any financial changes
6. Amendment lock is automatically released

---

## 8. Cancelling an Amendment

### 8.1 When to Cancel
You can cancel an amendment in **Draft** status if:
- The amendment request was made in error
- The changes are no longer required
- Supporting documents are missing

### 8.2 How to Cancel
1. Navigate to **Find Amendments** screen
2. Locate the draft amendment in the list
3. Click **"Cancel"** button in the action column
4. Confirm the cancellation

### 8.3 What Happens on Cancellation
1. Amendment status changes to **Cancelled**
2. Amendment lock is automatically released
3. The LC returns to its original state
4. No changes are applied to the Master LC

---

## 9. Viewing Amendment History

### 9.1 Amendment History Tab
Navigate to the **History** tab on the LC Detail screen to see:
- All amendments (accepted, rejected, pending)
- Amendment Number
- Date of each amendment
- Status of each amendment
- Key changes made

### 9.2 Effective Terms Display
The system shows:
- **Original LC Terms**: As initially issued
- **Current Effective Terms**: Original + all accepted amendments

---

## 10. Lock Management

### 9.1 Why Locks?
Amendment locks prevent **concurrent modifications** to the same LC by different users.

### 9.2 Lock Types
- **Auto-lock**: Acquired when creating an amendment draft
- **Released**: When amendment is submitted, rejected, cancelled, or closed
- **Auto-expire**: Locks expire automatically when the expiration time passes

### 9.3 Lock Management Screen (Administrator)
Administrators can view and manage all active amendment locks via the **Lock Management** screen.

**Who is Administrator?**
- Users assigned to the **TF_ADMIN** user group
- Typically users with username `tf-admin` (demo user)
- Contact your system administrator to get TF_ADMIN access

**Accessing the Screen:**
1. Navigate to **Import LC Dashboard**
2. Go to **Amendments** → **Find Amendments**
3. Click **"Lock Management"** button in the header

**Note:** If you don't see the "Lock Management" button, you don't have admin access. Contact your system administrator.

**Screen Features:**
- **View All Active Locks**: Displays all current amendment locks in the system
- **Filter Locks**: Filter by LC Number or User ID
- **View Lock Details**: See who holds the lock, when it was acquired, and expiration time
- **Force Release**: Release locks with admin reason for stale locks

### 9.4 Force Release
Administrators can force-release stale locks if a user session is interrupted.

**Via Lock Management Screen:**
1. Go to Lock Management screen
2. Find the lock you want to release
3. Click **"Force Release"** button
4. Enter the reason for force release
5. Confirm the action

**Via Service Call:**
- Execute the service: `moqui.trade.finance.AmendmentServices.forceRelease#AmendmentLock`
- Parameters: `lcId`, `adminReason`

---

## 11. Financial Impact

### 11.1 Charge Recalculation
When an amendment changes the LC amount:
- **Increase**: Additional charges are calculated and collected
- **Decrease**: Proportional charges are refunded/adjusted

### 11.2 Provision Adjustment
- **Amount Increase**: Additional provisions may be required
- **Amount Decrease**: Excess provisions are released
- **No Change**: Provisions remain unchanged

### 11.3 CBS Integration
Provisions are synchronized with the Core Banking System (CBS):
- Hold additional funds if amount increased
- Release excess funds if amount decreased

---

## 12. Common Scenarios

### Scenario 1: Increase LC Amount
1. Create amendment → Enter new amount
2. Submit → Supervisor approves → IPC approves
3. MT707 generated → Beneficiary accepts
4. Confirmation → Amount updated → Additional charges collected

### Scenario 2: Extend Expiry Date
1. Create amendment → Enter new expiry date
2. Submit → Supervisor approves → IPC approves
3. MT707 generated → Beneficiary accepts
4. Confirmation → Expiry date updated

### Scenario 3: Beneficiary Rejects Amendment
1. Amendment created and approved
2. MT707 sent to beneficiary
3. Beneficiary rejects
4. Original LC terms preserved
5. No changes applied

---

## 13. Troubleshooting

### Issue: "Amendment cannot be created - LC not in Issued status"
**Solution**: Only LCs in Issued status can be amended. Ensure the LC has been issued first.

### Issue: "LC is locked for amendment"
**Solution**: Another user is currently working on an amendment. You can:
1. Wait for them to complete their work
2. Use the Force Release function (if available in your role)
3. Contact administrator to force-release the lock

#### How to Force-Release a Lock (Administrator)
If you have administrator access, you can release a stale lock:

**Option 1: Via Lock Management Screen (Recommended)**
1. Navigate to Find Amendments screen
2. Click "Lock Management" button in the header
3. Find the lock and click "Force Release"
4. Enter reason and confirm

**Option 2: Via Service Call**
- Execute the service: `moqui.trade.finance.AmendmentServices.forceRelease#AmendmentLock`
- Parameters: `lcId`, `adminReason`

**Option 3: Via Database**
1. Access the H2 database
2. Run: `DELETE FROM lc_amendment_lock WHERE lc_id = '<LC_ID>'`
3. Or use Mantle screen: Runtime > Entity Data > LcAmendmentLock > Delete

### Issue: "Duplicate amendment exists"
**Solution**: A pending amendment already exists. Complete or cancel the existing amendment before creating a new one.

### Issue: "MT707 generation failed"
**Solution**: Contact technical support. The SWIFT message generation service may be unavailable.

### Issue: "Access Denied" on Lock Management screen
**Solution**: You need TF_ADMIN role to access this screen. Contact your system administrator.

---

## 14. Technical Reference

### 14.1 Available Services

| Service | Description | Parameters |
|---------|-------------|------------|
| `get#AllActiveLocks` | Retrieves all active amendment locks | `lcNumber` (optional), `userId` (optional) |
| `expire#Locks` | Automatically expires past-time locks | None |
| `forceRelease#AmendmentLock` | Force releases a lock with admin reason | `lcId`, `adminReason` |

### 14.2 Lock Behavior
- Locks are automatically released when: amendment is submitted, rejected, cancelled, or closed
- Locks expire automatically after the expiration time passes
- Lock validation prevents creating amendments on locked LCs

---

## 15. Related Documentation
- **BDD-R8.5_LCAmendment.md**: Full BDD specification
- **TDD_TestReport_R8.5_LCAmendment.md**: Test coverage report
- **User Guide Phase 1**: Core LC functionality

---

*Document Version: 1.1*
*Last Updated: 2026-03-18*

## Changelog

### Version 1.1 (2026-03-18)
- Added Lock Management Screen for administrators
- Added Cancel Amendment functionality in Find Amendments
- Added Force Release via Lock Management UI
- Updated troubleshooting with Lock Management Screen option
- Added Technical Reference section with service APIs
- Fixed lock release behavior for cancelled/closed amendments
