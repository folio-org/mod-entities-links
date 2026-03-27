---
feature_id: authority-storage
title: Authority Storage
updated: 2026-03-27
---

# Authority Storage

## What it does
Provides full CRUD operations for MARC authority records, including single-record and bulk creation, retrieval with CQL filtering, update with optimistic locking, soft-delete, and retention-based expiration of deleted records. Deleted (archived) authorities remain queryable via a `deleted` filter and are permanently removed after a configurable retention period.

## Why it exists
Authority records are the canonical source for controlled headings (personal names, corporate bodies, subjects, etc.) used to link bibliographic instance records. The module centralizes authority storage so that other FOLIO modules can create, retrieve, update, and delete authorities through a single REST API, while domain events keep downstream consumers (search indexing, linked data) synchronized.

## Entry point(s)

| Method | Path | Description |
|--------|------|-------------|
| GET | /authority-storage/authorities | Returns a paged collection of authorities; supports `offset`, `limit`, CQL `query`, `deleted` (boolean), and `idOnly` (boolean) parameters |
| GET | /authority-storage/authorities/{id} | Returns a single authority by ID (non-deleted only) |
| POST | /authority-storage/authorities | Creates a new authority record |
| PUT | /authority-storage/authorities/{id} | Updates an existing non-deleted authority (optimistic locking via `_version`) |
| DELETE | /authority-storage/authorities/{id} | Soft-deletes an authority (sets `deleted = true`) |
| POST | /authority-storage/authorities/bulk | Bulk upsert of authority records from an S3 file |
| POST | /authority-storage/expire/authorities | Triggers retention-based hard-delete of expired soft-deleted authorities |

### Domain events produced

| Topic | Event Types | Description |
|-------|-------------|-------------|
| `authorities.authority` | CREATE, UPDATE, DELETE, HARD_DELETE, REINDEX | Published after authority mutations to notify downstream consumers |

## Business rules and constraints
- Authority records use a `deleted` flag for soft-delete; the `authority_archive` table has been consolidated into the `authority` table.
- `GET /authorities` returns only non-deleted records by default; pass `deleted=true` to retrieve soft-deleted (archived) records.
- `GET /authorities/{id}`, `PUT`, and `DELETE` operate only on non-deleted records; attempting these on a deleted authority returns 404.
- Updates require optimistic locking: the request `_version` must be ≥ the stored version; otherwise a 409 Conflict is returned.
- `POST` (create) validates that the referenced `sourceFileId` exists; returns 422 if it does not.
- `PUT` (update) validates the referenced `sourceFileId` exists; returns 404 if it does not.
- `DELETE` soft-deletes the authority and publishes a soft-delete domain event; associated authority data stats are also deleted.
- Bulk upsert creates new records and updates existing ones (matched by ID), publishing the appropriate CREATE or UPDATE events.
- The expire endpoint reads tenant settings to determine whether expiration is enabled and the retention period in days; authorities with `deleted = true` and `updatedDate` older than the retention period are permanently removed and a HARD_DELETE event is published for each.
- The `idOnly=true` parameter returns only authority IDs (supports `text/plain` Accept header for newline-delimited UUID output).
- FQM (FOLIO Query Machine) queries filter on `deleted = false` so that only active authorities appear in FQM entity type results.

## Error behavior
- **404 Not Found**: Authority does not exist or is soft-deleted (for GET by ID, PUT, DELETE).
- **409 Conflict**: Optimistic locking violation on PUT — stored `_version` exceeds request version.
- **422 Unprocessable Content**: Validation failures including duplicate authority ID, missing/invalid source file reference, or invalid request body.
- **400 Bad Request**: Malformed request parameters or constraint violations.

## Configuration

| Variable | Purpose |
|----------|---------|
| `archives.expiration.enabled` (tenant setting, group: `authorities`) | Controls whether retention-based expiration of deleted authorities is active for the tenant |
| `archives.expiration.period` (tenant setting, group: `authorities`) | Number of days after soft-delete before a deleted authority is permanently removed |

## Dependencies and interactions
- **Authority Source Files**: Authorities reference an authority source file via `sourceFileId`; the source file must exist for create and update operations.
- **Kafka (`authorities.authority` topic)**: Domain events (CREATE, UPDATE, DELETE, HARD_DELETE, REINDEX) are published for each authority mutation, consumed by downstream modules for search indexing and linked data synchronization.
- **Authority Data Stats**: Soft-deleting an authority also deletes its associated data statistics records.
- **Tenant Settings Service**: The expire endpoint queries tenant-level settings to determine expiration enablement and retention period.
