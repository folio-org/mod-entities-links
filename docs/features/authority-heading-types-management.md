---
feature_id: authority-heading-types
title: Authority Heading Types Management
updated: 2026-03-30
---

# Authority Heading Types Management

## What it does
Provides a read-only, paginated, CQL-filterable collection endpoint for retrieving authority heading types. Heading types classify the kind of heading an authority record carries (e.g., Personal Name, Corporate Name, Topical Term). The dataset is seeded at schema creation time and exposed so that clients can look up valid heading type codes, names, and queryability flags.

## Why it exists
Authority records store a `headingType` field whose value must correspond to a known heading type code. By exposing heading types through a dedicated API, clients (UI, FQM, search) can dynamically retrieve the valid set of heading types—including which ones are queryable—without hard-coding the list. The `queryable` flag distinguishes primary heading types from their truncated internal variants that should not appear in user-facing filters.

## Entry point(s)
| Method | Path                     | Description                                                                                                   |
|--------|--------------------------|---------------------------------------------------------------------------------------------------------------|
| GET    | /authority-heading-types | Returns a paged collection of authority heading types; supports `offset`, `limit`, and CQL `query` parameters |

## Business rules and constraints
- The endpoint is read-only (GET only); heading types are managed exclusively through database migrations.
- The `authority_heading_type` table enforces unique constraints on both `name` and `code`.
- The database is seeded with 26 heading types on schema creation: 13 primary (queryable) and 13 truncated variants (non-queryable).
- Primary heading types (`queryable = true`): Personal Name, Personal Name Title, Corporate Name, Corporate Name Title, Meeting Name, Meeting Name Title, Uniform Title, Named Event, Topical Term, Geographic Name, Genre Term, Chron Term, Medium Perf Term.
- Truncated variants (`queryable = false`) mirror the primary types with a `Trunc` suffix and are used internally.
- CQL filtering is supported on all fields (`id`, `name`, `code`, `queryable`).
- The heading type data is also registered as a private FQM entity type (`authority-heading-type`) for use in FOLIO Query Machine queries.

## Dependencies and interactions
- **FQM (FOLIO Query Machine)**: The `authority-heading-type` entity type is configured in `fqm-config.toml` as a private entity type backed by the `authority_heading_type` table, enabling FQM to query heading types for filter value lists.
- Read access requires the permission `authority-heading-types.collection.get`.