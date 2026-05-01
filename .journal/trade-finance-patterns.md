# Trade Finance Business Domain Patterns

## 1. Trade Finance Party Architecture (Junction Pattern)

### Problem
Flat BIC/name fields on instruments are inflexible and hard to scale for multi-party roles (Advising, Negotiating, Reimbursing, etc.).

### Solution: Role-Based Junction
Use a structured junction model to separate party identities from their roles on a specific instrument.

**1. Base Identity (TradeParty)**
Stores name, address, and KYC status.
```xml
<entity entity-name="TradeParty" package="trade">
    <field name="partyId" type="id" is-pk="true"/>
    <field name="partyTypeEnumId" type="id"/> <!-- PARTY_COMMERCIAL, PARTY_BANK -->
    <field name="partyName" type="text-medium"/>
</entity>
```

**2. Bank Extension (TradePartyBank)**
One-to-one extension for SWIFT-specific attributes.
```xml
<entity entity-name="TradePartyBank" package="trade">
    <field name="partyId" type="id" is-pk="true"/>
    <field name="swiftBic" type="text-short"/>
    <field name="hasActiveRMA" type="text-indicator"/>
</entity>
```

**3. Instrument Junction (TradeInstrumentParty)**
Maps parties to instruments with a specific role.
```xml
<entity entity-name="TradeInstrumentParty" package="trade">
    <field name="instrumentId" type="id" is-pk="true"/>
    <field name="roleEnumId" type="id" is-pk="true"/> <!-- TP_APPLICANT, TP_BENEFICIARY, etc. -->
    <field name="partyId" type="id"/>
</entity>
```

### Benefits
- **Extensibility**: Add new roles (e.g., `TP_INTERMEDIARY`) without schema changes.
- **Data Integrity**: Single source of truth for BICs and addresses.
- **View Performance**: Join via `TradeInstrumentParty` with filtered `roleEnumId` for specific aliases.

---

## 2. Immutability Guard Pattern (Domain Enforcement)

### Problem
Users modifying core financial terms of Issued instruments.

### Solution
Service-level guard checking business state before allowing updates to sensitive fields.

```xml
<service verb="update" noun="ImportLetterOfCredit">
    <in-parameters>
        <parameter name="instrumentId" required="true"/>
        <parameter name="lcAmount" type="BigDecimal"/>
        <parameter name="lcCurrencyUomId"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc"/>
        
        <!-- FR-LIF-35: Immutability Guard for Issued Instruments -->
        <if condition="lc?.businessStateId == 'LC_ISSUED' &amp;&amp; (lcAmount != null || lcCurrencyUomId != null)">
            <return message="Cannot modify financial terms of an Issued LC via update. Use Amendment workflow."/>
        </if>
        
        <!-- Continue with update if not issued or no financial changes -->
    </actions>
</service>
```

### Key Considerations
- **Non-Breaking Return**: Using `<return message="..." />` instead of `error="true"` allows the UI to display a warning without a hard system failure if appropriate.
- **Draft Modification**: Ensure the guard only triggers for terminal states (e.g., `LC_ISSUED`, `LC_CLOSED`).
- **Comprehensive Check**: Include both primary field names and aliases (e.g., `amount` and `lcAmount`) if `auto-parameters` are used.
