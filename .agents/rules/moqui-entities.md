---
paths:
  - "runtime/component/TradeFinance/entity/**/*.xml"
---

# Moqui Database Constraints

## 1. Entity Definitions
* **Primary Keys:** Use exactly one `<field>` with `is-pk="true"` as a single-column surrogate key.
* **Audit Stamps:** Do not manually add `lastUpdatedStamp` or `createdTxStamp`; the framework injects these automatically.
* **Data Types:** Use Moqui semantic types (`id`, `text-short`, `date`), NEVER raw SQL types like `VARCHAR`.

## 2. View Entities & Caching
* **Views:** Use `<view-entity>` and `<member-entity>` for SQL-free joins. Do not write raw SQL queries in services.
* **Caching:** Enable entity caching (`cache="true"`) ONLY for static configuration data (e.g., Enumerations, StatusItems). Never cache highly transactional tables.

## 3. Entity comments (MANDATORY)
* Each Entity must have meaningful comments to explain the purpose of this Entity, related business requirements and/or system processing, usage

