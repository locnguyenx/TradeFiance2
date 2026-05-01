# TradeParty Refactor Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the frontend UI to utilize the new structured Party-Role junction pattern, replacing flat BIC fields with party selectors and supporting "Available With" explicit configuration.

**Architecture:** Update `types.ts` to reflect the removal of flat bank BIC fields and the addition of the `parties` array. Refactor `IssuanceStepper.tsx` to load/save party assignments via the junction payload structure, replacing free-text inputs with dropdowns populated from the master `TradeParty` list. Refactor `InstrumentDetails.tsx` to read party details and BICs from the `parties` array. Finally, update tests to reflect these changes.

**Tech Stack:** Next.js, React, TypeScript

---

## Task 1: Update API Types

**BDD Scenarios:** SC-12, SC-17
**BRD Requirements:** FR-TP-16
**User-Facing:** NO (Internal Types)

**Files:**
- Modify: `frontend/src/api/types.ts`

- [ ] **Step 1: Add TradeInstrumentParty interface and update TradeParty**

In `frontend/src/api/types.ts`, add the new types and update `TradeParty` to include the new fields:

```typescript
export interface TradeInstrumentParty {
  instrumentId: string;
  roleEnumId: string;
  partyId: string;
  partyName?: string;
  swiftBic?: string;
  partyTypeEnumId?: string;
}
```

Update `TradeParty`:
```typescript
export interface TradeParty {
  partyId: string;
  partyName: string;
  partyTypeEnumId?: string;
  accountNumber?: string;
  roleTypeId: string;
  swiftBic?: string;
  address1?: string;
  city?: string;
  countryGeoId?: string;
}
```

- [ ] **Step 2: Update ImportLetterOfCredit interface**

In `frontend/src/api/types.ts`, add the `parties` array and `availableWithEnumId`, and mark the old flat fields as deprecated/optional (or remove them):

```typescript
export interface ImportLetterOfCredit {
  // LC-specific new fields
  chargeAllocationEnumId?: string;
  partialShipmentEnumId?: string;
  transhipmentEnumId?: string;
  latestShipmentDate?: string;
  confirmationEnumId?: string;
  lcTypeEnumId?: string;
  productCatalogId?: string;
  
  // NEW Party Junction Fields
  parties?: TradeInstrumentParty[];
  availableWithEnumId?: string;

  // Deprecated flat fields (keep optional for backward compatibility during transition)
  issuingBankBic?: string;
  advisingBankBic?: string;
  advisingThroughBankBic?: string;
  availableWithBic?: string;
  draweeBankBic?: string;
  availableWithName?: string;
  applicantPartyId?: string;
  beneficiaryPartyId?: string;
  applicantName?: string;
  beneficiaryName?: string;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/types.ts
git commit -m "feat(ui): update API types for TradeParty refactor"
```

---

## Task 2: Refactor Issuance Stepper State and Load/Save Logic

**BDD Scenarios:** SC-12, SC-13, SC-14
**BRD Requirements:** FR-TP-16, US-TP-05
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/IssuanceStepper.tsx`

- [ ] **Step 1: Update initial formData state**

In `frontend/src/components/IssuanceStepper.tsx`, update the `formData` state object. Replace flat text fields with party role ID fields:

```typescript
    const [formData, setFormData] = useState<any>({
        // Base fields
        amount: '',
        currencyUomId: 'USD',
        applicantPartyId: '',
        beneficiaryPartyId: '',
        customerFacilityId: '',
        chargeAllocationEnumId: 'APPLICANT',
        confirmationEnumId: 'WITHOUT',
        productCatalogId: '',
        
        // Removed flat BIC fields and replaced with party role selection
        advisingBankPartyId: '',
        advisingThroughBankPartyId: '',
        availableWithEnumId: 'AVAIL_ANY_BANK',
        negotiatingBankPartyId: '',
        draweeBankPartyId: '',
        
        availableByEnumId: 'SIGHT',
        shipmentPeriodText: '',
        expiryPlace: '',
        latestShipmentDate: '',
        goodsDescription: ''
    });
```

- [ ] **Step 2: Update draft loading logic**

In the `useEffect` that loads an existing draft:

```typescript
                const getPartyId = (parties: any[], role: string) => parties?.find(p => p.roleEnumId === role)?.partyId || '';

                setFormData(prev => ({
                    ...prev,
                    ...lc,
                    productCatalogId: lc.productCatalogId || '',
                    applicantPartyId: getPartyId(lc.parties || [], 'TP_APPLICANT') || lc.applicantPartyId || '',
                    beneficiaryPartyId: getPartyId(lc.parties || [], 'TP_BENEFICIARY') || lc.beneficiaryPartyId || '',
                    // Use flat fields as fallback for legacy drafts if parties array is missing
                    applicant: lc.applicantName || lc.applicantPartyName || '',
                    beneficiary: lc.beneficiaryName || lc.beneficiaryPartyName || '',
                    availableWithEnumId: lc.availableWithEnumId || 'AVAIL_ANY_BANK',
                    advisingBankPartyId: getPartyId(lc.parties || [], 'TP_ADVISING_BANK'),
                    advisingThroughBankPartyId: getPartyId(lc.parties || [], 'TP_ADVISE_THROUGH_BANK'),
                    negotiatingBankPartyId: getPartyId(lc.parties || [], 'TP_NEGOTIATING_BANK'),
                    draweeBankPartyId: getPartyId(lc.parties || [], 'TP_DRAWEE_BANK'),
                    availableByEnumId: lc.availableByEnumId || 'SIGHT',
                    shipmentPeriodText: lc.shipmentPeriodText || ''
                }));
```

- [ ] **Step 3: Update `handleSaveDraft` and `handleSubmit` to build parties payload**

Inside both `handleSaveDraft` and `handleSubmit`, before constructing the `payload`, build the `parties` array:

```typescript
        const buildPartiesPayload = () => {
            const result = [];
            if (formData.applicantPartyId) result.push({ roleEnumId: 'TP_APPLICANT', partyId: formData.applicantPartyId });
            if (formData.beneficiaryPartyId) result.push({ roleEnumId: 'TP_BENEFICIARY', partyId: formData.beneficiaryPartyId });
            if (formData.advisingBankPartyId) result.push({ roleEnumId: 'TP_ADVISING_BANK', partyId: formData.advisingBankPartyId });
            if (formData.advisingThroughBankPartyId) result.push({ roleEnumId: 'TP_ADVISE_THROUGH_BANK', partyId: formData.advisingThroughBankPartyId });
            if (formData.draweeBankPartyId) result.push({ roleEnumId: 'TP_DRAWEE_BANK', partyId: formData.draweeBankPartyId });
            if (formData.availableWithEnumId === 'AVAIL_SPECIFIC_BANK' && formData.negotiatingBankPartyId) {
                result.push({ roleEnumId: 'TP_NEGOTIATING_BANK', partyId: formData.negotiatingBankPartyId });
            }
            return result;
        };
```

Update the `payload` object in both functions:

```typescript
            const payload = {
                ...formData,
                amount: Number(formData.amount),
                businessStateId: 'LC_DRAFT', // or LC_SUBMITTED for handleSubmit
                parties: buildPartiesPayload(),
                availableWithEnumId: formData.availableWithEnumId
            };
            
            // Delete legacy flat fields so they aren't sent to the backend
            delete payload.advisingBankBic;
            delete payload.advisingThroughBankBic;
            delete payload.issuingBankBic;
            delete payload.availableWithBic;
            delete payload.draweeBankBic;
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/IssuanceStepper.tsx
git commit -m "feat(ui): refactor IssuanceStepper state and payload construction"
```

---

## Task 3: Refactor Issuance Stepper Form UI

**BDD Scenarios:** SC-12, SC-13
**BRD Requirements:** FR-TP-09, US-TP-04
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/IssuanceStepper.tsx`

- [ ] **Step 1: Compute party lists**

At the top of the component render function, compute the filtered lists:
```typescript
    const commercialParties = parties.filter(p => p.partyTypeEnumId === 'PARTY_COMMERCIAL' || !p.partyTypeEnumId);
    const bankParties = parties.filter(p => p.partyTypeEnumId === 'PARTY_BANK');
```

- [ ] **Step 2: Refactor Beneficiary Input**

Change the `beneficiary` textarea to a select dropdown in the `Parties & Limits` section:

```tsx
                            <div className="field-group">
                                <label htmlFor="beneficiary" className="required-label">Beneficiary (Tag 59)</label>
                                <select 
                                    id="beneficiary"
                                    value={formData.beneficiaryPartyId}
                                    onChange={e => {
                                        const party = commercialParties.find(p => p.partyId === e.target.value);
                                        setFormData({...formData, beneficiaryPartyId: e.target.value, beneficiary: party?.partyName || ''});
                                    }}
                                    className={(swiftErrors.beneficiaryName || swiftErrors.beneficiary) ? 'is-invalid' : ''}
                                >
                                    <option value="">Select Beneficiary...</option>
                                    {commercialParties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName}</option>)}
                                </select>
                                {(swiftErrors.beneficiaryName || swiftErrors.beneficiary) && <p className="error-text text-xs mt-1">{swiftErrors.beneficiaryName || swiftErrors.beneficiary}</p>}
                            </div>
```

- [ ] **Step 3: Refactor Advising and Advise Through Banks**

Change the free-text inputs for `advisingBankBic` and `advisingThroughBankBic` to select dropdowns using `bankParties`:

```tsx
                            <div className="field-group">
                                <label htmlFor="advisingBankPartyId">Advising Bank (Tag 57A)</label>
                                <select 
                                    id="advisingBankPartyId"
                                    className={swiftErrors.advisingBankBic ? 'is-invalid' : ''}
                                    value={formData.advisingBankPartyId}
                                    onChange={e => setFormData({...formData, advisingBankPartyId: e.target.value})}
                                >
                                    <option value="">Select Advising Bank...</option>
                                    {bankParties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName} {p.swiftBic ? `(${p.swiftBic})` : ''}</option>)}
                                </select>
                                {swiftErrors.advisingBankBic && <p className="error-text text-xs mt-1">{swiftErrors.advisingBankBic}</p>}
                            </div>

                            <div className="field-group">
                                <label htmlFor="advisingThroughBankPartyId">Advising Through Bank (Tag 58A)</label>
                                <select 
                                    id="advisingThroughBankPartyId"
                                    className={swiftErrors.advisingThroughBankBic ? 'is-invalid' : ''}
                                    value={formData.advisingThroughBankPartyId}
                                    onChange={e => setFormData({...formData, advisingThroughBankPartyId: e.target.value})}
                                >
                                    <option value="">Select Advise Through Bank...</option>
                                    {bankParties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName} {p.swiftBic ? `(${p.swiftBic})` : ''}</option>)}
                                </select>
                                {swiftErrors.advisingThroughBankBic && <p className="error-text text-xs mt-1">{swiftErrors.advisingThroughBankBic}</p>}
                            </div>
```

- [ ] **Step 4: Refactor Available With (Tag 41a) and Drawee Bank**

In the `Financials & Dates` section, update `Available With` and `Drawee Bank`:

```tsx
                                <div className="field-group">
                                    <label>Available With (Tag 41A/D)</label>
                                    <div className="flex gap-4 mb-2 mt-2">
                                        <label className="flex items-center gap-2 cursor-pointer">
                                            <input type="radio" checked={formData.availableWithEnumId === 'AVAIL_ANY_BANK'} onChange={() => setFormData({...formData, availableWithEnumId: 'AVAIL_ANY_BANK'})} /> Any Bank
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer">
                                            <input type="radio" checked={formData.availableWithEnumId === 'AVAIL_SPECIFIC_BANK'} onChange={() => setFormData({...formData, availableWithEnumId: 'AVAIL_SPECIFIC_BANK'})} /> Specific Bank
                                        </label>
                                    </div>
                                    {formData.availableWithEnumId === 'AVAIL_SPECIFIC_BANK' && (
                                        <select 
                                            id="negotiatingBankPartyId"
                                            className={swiftErrors.availableWithBic ? 'is-invalid' : ''}
                                            value={formData.negotiatingBankPartyId}
                                            onChange={e => setFormData({...formData, negotiatingBankPartyId: e.target.value})}
                                        >
                                            <option value="">Select Negotiating Bank...</option>
                                            {bankParties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName} {p.swiftBic ? `(${p.swiftBic})` : ''}</option>)}
                                        </select>
                                    )}
                                    {swiftErrors.availableWithBic && <p className="error-text text-xs mt-1">{swiftErrors.availableWithBic}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="draweeBankPartyId">Drawee Bank (Tag 42A)</label>
                                    <select 
                                        id="draweeBankPartyId"
                                        className={swiftErrors.draweeBankBic ? 'is-invalid' : ''}
                                        value={formData.draweeBankPartyId}
                                        onChange={e => setFormData({...formData, draweeBankPartyId: e.target.value})}
                                    >
                                        <option value="">Select Drawee Bank...</option>
                                        {bankParties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName} {p.swiftBic ? `(${p.swiftBic})` : ''}</option>)}
                                    </select>
                                    {swiftErrors.draweeBankBic && <p className="error-text text-xs mt-1">{swiftErrors.draweeBankBic}</p>}
                                </div>
```
Remove `availableWithName` textarea entirely, since the backend handles Tag 41D fallback based on the selected party's address.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/IssuanceStepper.tsx
git commit -m "feat(ui): refactor IssuanceStepper form to use party selectors"
```

---

## Task 4: Refactor Instrument Details View

**BDD Scenarios:** SC-17
**BRD Requirements:** US-TP-04
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/InstrumentDetails.tsx`

- [ ] **Step 1: Add helper functions**

Inside the `InstrumentDetails` component, add helpers to extract party details from the `parties` array:

```typescript
  const getPartyName = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.partyName || party?.partyId || '';
  };

  const getBankBic = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.swiftBic || '---';
  };
```

- [ ] **Step 2: Update Parties section**

Update the `Parties & BICs` section to use the helper functions, prioritizing the junction array but falling back to the view aliases (`applicantPartyName` / `beneficiaryPartyName`) for legacy records:

```tsx
            <section className="audit-section">
              <SectionHeader id="parties" title="Parties & BICs" icon={<Users size={20} />} />
              <div className="data-table">
                <DataField label="Applicant (Obligor)" value={getPartyName('TP_APPLICANT') || instrument.applicantPartyName || instrument.applicantName || instrument.applicantPartyId} highlight />
                <DataField label="Beneficiary (Payee)" value={getPartyName('TP_BENEFICIARY') || instrument.beneficiaryPartyName || instrument.beneficiaryName || instrument.beneficiaryPartyId} highlight />
                <div className="row-divider">Banking Network Details</div>
                <DataField label="Issuing Bank BIC" value={getBankBic('TP_ISSUING_BANK')} />
                <DataField label="Advising Bank BIC" value={getBankBic('TP_ADVISING_BANK')} />
                <DataField label="Available with Bank" value={instrument.availableWithEnumId === 'AVAIL_ANY_BANK' ? 'ANY BANK' : getBankBic('TP_NEGOTIATING_BANK')} />
                <DataField label="Drawee Bank" value={getBankBic('TP_DRAWEE_BANK')} />
              </div>
            </section>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/InstrumentDetails.tsx
git commit -m "feat(ui): refactor InstrumentDetails to read from party junction"
```

---

## Task 5: Refactor Frontend Tests

**BDD Scenarios:** SC-12
**BRD Requirements:** FR-TP-16
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/IssuanceStepper.test.tsx`

- [ ] **Step 1: Update form interaction in tests**

In `completeStep0` function, replace the `Beneficiary` text input with a select dropdown interaction:

```typescript
        // Change from text input to select
        fireEvent.change(screen.getByLabelText(/Beneficiary \(Tag 59\)/i), { target: { value: 'GLOBAL_CORP' } });
```

- [ ] **Step 2: Update field presence assertions**

In the "renders all required BIC fields in Step 1 and 2" test, remove assertions checking for "BIC" text fields and instead assert the presence of dropdowns or radio buttons:

```typescript
    it('renders all required Bank fields in Step 1 and 2', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        // Step 1 (Parties)
        expect(screen.getByLabelText(/Advising Through Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Advising Bank/i)).toBeInTheDocument();
        
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); 
        });
        
        // Wait for transition to Step 2 (Financials)
        await waitFor(() => expect(screen.getByText(/Step 2/i)).toBeInTheDocument());
        
        expect(screen.getByText(/Available With \(Tag 41A\/D\)/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Any Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Specific Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Drawee Bank/i)).toBeInTheDocument();
    });
```

- [ ] **Step 3: Run the test and commit**

```bash
cd frontend && npm test -- src/components/IssuanceStepper.test.tsx
git add frontend/src/components/IssuanceStepper.test.tsx
git commit -m "test(ui): align IssuanceStepper tests with TradeParty refactor"
```
