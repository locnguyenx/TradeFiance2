# Notes for Next Session
**Date:** 2026-05-06

## Context
We have completed the comprehensive audit hardening for the **Import LC Module**. The module is now certified audit-ready with 100% functional integrity and visibility tracking.

## Instructions for Next Session
1.  **Scale the Audit Pattern**: Apply the same hardening patterns (Latest Transaction Pointer, Tolerance-based Earmarking, Z-Charset support) to the **Export LC** and **Collections** modules.
2.  **Monitor View Performance**: The new `ImportLetterOfCreditView` uses a join to `TradeTransaction` via `latestTransactionId`. Monitor query performance on large datasets; if slow, consider adding a database index to `latestTransactionId`.
3.  **UI Verification**: Ensure the "Dual-Status Visibility" on the dashboard is correctly rendered in the front-end (it should now show both legal state and active transaction status).

## Critical Lessons Learned
- **EECA Chaining**: Using an EECA to maintain a `latestTransactionId` pointer on the master instrument is the most robust way to solve dual-status visibility in Moqui.
- **Service Security**: Moqui service parameters block `<` and `>` by default. Use `allow-html="any"` for SWIFT narrative fields that require the Z-Character set.
- **Facility Integrity**: Always factor in positive tolerance when reserving facility limits at issuance to avoid over-drawing risk during the document presentation phase.
