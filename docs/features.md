# Module Features

This module provides the following features:

| Feature                                                                                    | Description                                                                                                                                                                                                           |
|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Authority Identifier Types Management](features/authority-identifier-types-management.md) | Provides a paginated, CQL-filterable collection endpoint for retrieving authority identifier types (e.g., LCCN, Canceled LCCN) that clients use to classify and filter authority records.                             |
| [Authority Storage](features/authority-storage.md)                                         | Full CRUD for MARC authority records with soft-delete, CQL filtering, optimistic locking, bulk upsert, and retention-based expiration of deleted records.                                                             |
| [Data Import Authority Record Processing](features/data-import-authority-processing.md)    | Synchronizes MARC authority record changes (create, update, delete) from data-import Kafka events into authority storage, and tracks cancelled import jobs to allow all module instances to identify affected events. |
