---
feature_id: data-import-authority-processing
title: Data Import Authority Record Processing
updated: 2026-03-12
---

# Data Import Authority Record Processing

## What it does

When MARC authority records are created, modified, or deleted through a FOLIO data-import job, this module receives the corresponding Kafka events and synchronizes those changes into authority storage — creating, updating, or deleting authority records as appropriate. When a data-import job is cancelled, the module records the job ID so that in-flight events belonging to that job can be identified by the module.

## Why it exists

Authority record data originates in SRS (Source Record Storage) via data-import pipelines. Without this integration, changes made to MARC authority records during a bulk import would not be reflected in the authority storage that downstream linking and suggestion features depend on. The cancellation-awareness mechanism prevents stale or partial imports from producing incorrect authority state in multi-instance deployments.

## Entry point(s)

| Type | Topic Pattern                                                                                                                      | Description |
|------|------------------------------------------------------------------------------------------------------------------------------------|-------------|
| Kafka Consumer | `(${ENV}\.)[A-Za-z0-9-]+\.\w+\.DI_SRS_MARC_AUTHORITY_RECORD_(CREATED\|MODIFIED_READY_FOR_POST_PROCESSING \|DELETED \|NOT_MATCHED)` | Receives batches of MARC authority record events (created, modified, deleted, not matched) |
| Kafka Consumer | `(${ENV}\.)[A-Za-z0-9-]+\.\w+\.DI_JOB_CANCELLED`                                                                                   | Receives DI_JOB_CANCELLED notifications to track cancelled job IDs |

### Event processing — MARC authority events

- **When processed**: in batches, grouped by tenant; all records in a batch are processed concurrently and the listener waits for all to complete before committing
- **Event types handled**:
  - `DI_SRS_MARC_AUTHORITY_RECORD_CREATED` — triggers authority creation
  - `DI_SRS_MARC_AUTHORITY_RECORD_MODIFIED_READY_FOR_POST_PROCESSING` — triggers authority update
  - `DI_SRS_MARC_AUTHORITY_RECORD_DELETED` — triggers authority deletion
  - `DI_SRS_MARC_AUTHORITY_RECORD_NOT_MATCHED` — received but no handler action is taken
- **Processing behavior**: each event is routed through the `EventManager` pipeline which selects the eligible handler based on the event's action/mapping profile; the handler mutates authority storage and publishes an outgoing event

### Event processing — job cancellation events

- **When processed**: one message at a time (non-batch)
- **Event type handled**: `DI_JOB_CANCELLED`
- **Processing behavior**: the `jobExecutionId` and `tenantId` headers are extracted and the job ID is registered in the local in-memory cache; no authority storage mutations occur

## Business rules and constraints

- **Create**: a new authority record is created from the MARC payload using the configured mapping rules and MARC record mapper
- **Update**: the existing authority is fetched first to carry its `version` field forward (optimistic locking)
- **Delete**: the authority is deleted by the `AUTHORITY_RECORD_ID` value stored in the event context
- **Cancelled jobs**: a job cancellation is stored per-tenant with a composite key `tenantId:jobId`; the cache is populated independently of MARC event consumption so that all module instances can reference it

## Caching

**Cancelled job cache** (`data-import-canceled-job-cache`):

All module instances maintain their own in-memory Caffeine cache of cancelled job IDs. Because each instance subscribes to `DI_JOB_CANCELLED` using a **unique consumer group** (one per instance), every instance independently receives every cancellation event and populates its own cache. The cache entry for a given job expires after the configured TTL.

Cluster correctness: a cancellation registered by one instance is **not** propagated to other instances via shared state — each instance learns about the cancellation directly from Kafka. There is a brief window between a job cancellation event being published and all instances receiving it; MARC authority events processed in that window may not yet see the cancelled job in cache.

## Configuration

| Env var | Description | Default |
|---------|-------------|---------|
| `KAFKA_DI_CONSUMER_CONCURRENCY` | Number of concurrent Kafka consumer threads for MARC authority events | `4`     |
| `KAFKA_DI_CANCELED_CONSUMER_CONCURRENCY` | Number of concurrent Kafka consumer threads for DI_JOB_CANCELLED events | `1`     |
| `DI_CANCELED_JOB_CACHE_TTL` | How long a cancelled job ID remains in cache | `24h`   |
| `DI_CANCELED_JOB_CACHE_MAX_SIZE` | Maximum number of cancelled job entries per module instance | `500`   |
| `KAFKA_DI_INVENTORY_AUTHORITY_UPDATED_PARTITIONS` | Partition count for the `DI_INVENTORY_AUTHORITY_UPDATED` topic | `8`     |
| `KAFKA_DI_INVENTORY_AUTHORITY_UPDATED_REPLICATION_FACTOR` | Replication factor for the `DI_INVENTORY_AUTHORITY_UPDATED` topic | —       |
| `KAFKA_DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING_PARTITIONS` | Partition count for the `DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING` topic | `8`     |
| `KAFKA_DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING_REPLICATION_FACTOR` | Replication factor for the `DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING` topic | —       |

## Dependencies and interactions

**Consumed topics (input):**

| Topic pattern | Event meaning |
|---------------|---------------|
| `*.DI_SRS_MARC_AUTHORITY_RECORD_CREATED` | New MARC authority record ingested via data import |
| `*.DI_SRS_MARC_AUTHORITY_RECORD_MODIFIED_READY_FOR_POST_PROCESSING` | Existing MARC authority record modified |
| `*.DI_SRS_MARC_AUTHORITY_RECORD_DELETED` | MARC authority record deleted |
| `*.DI_SRS_MARC_AUTHORITY_RECORD_NOT_MATCHED` | MARC record with no matching authority (informational) |
| `*.DI_JOB_CANCELLED` | Data-import job was cancelled |

**Published topics (output):**

| Topic | Produced by | Meaning |
|-------|-------------|---------|
| `DI_INVENTORY_AUTHORITY_CREATED` | `AuthorityCreateEventHandler`, `AuthorityDeleteEventHandler` | Signals authority record was created or delete processing is complete |
| `DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING` | `AuthorityCreateEventHandler` (via post-processing chain) | Triggers downstream post-processing for a newly created authority |
| `DI_INVENTORY_AUTHORITY_UPDATED` | `AuthorityUpdateEventHandler` | Signals that an authority record was updated |
