---
feature_id: authority-identifier-types
title: Authority Identifier Types Management
updated: 2026-03-24
---

# Authority Identifier Type Lookup

## What it does
Exposes a paginated, CQL-filterable collection endpoint that returns all known authority identifier types (e.g., LCCN, Canceled LCCN, Control number). Each type carries a stable machine-readable `code`, a human-readable `name`, and a `source` label indicating whether the entry is system-supplied (`folio`) or tenant-defined (`local`).

## Why it exists
Consumers such as the FOLIO UI need a stable reference list of identifier types to correctly label and filter authority records. Centralising these types in a dedicated storage endpoint avoids hard-coding values in clients and allows the list to be extended without application changes.

## Entry point(s)
| Method | Path                        | Description                                                                                                      |
|--------|-----------------------------|------------------------------------------------------------------------------------------------------------------|
| GET    | /authority-identifier-types | Returns a paged collection of authority identifier types; supports `offset`, `limit`, and CQL `query` parameters |

## Business rules and constraints
- `name` and `code` must each be unique across all records; duplicate values are rejected at the database level.
- `name`, `code`, and `source` are required fields on every record.
- The following FOLIO-supplied seed records are present after module installation: 
    - `Canceled LCCN`
    - `Control number`
    - `LCCN`
    - `Other standard identifier`
    - `System control number`.
- Read access requires the permission `authority-identifier-types.collection.get`.
