# mod-entities-links

Copyright (C) 2022-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

  * [Introduction](#introduction)
  * [Additional Information](#additional-information)
    * [Issue tracker](#issue-tracker)
    * [API Documentation](#api-documentation)
    * [Module Documentation](#module-documentation)
    * [Code analysis](#code-analysis)
    * [Download and configuration](#download-and-configuration)
    * [Development tips](#development-tips)


## Introduction

This module provides a storing and processing functionality for links between entities.

## Module features
### Consortium support
#### Authorities
Authorities are not propagated to member tenants. In cases when member tenants need some knowledge of shared authority - it queries the central tenant.
#### Instance-authority links
Instance-authority links are not propagated to member tenants.
Links for local bib and shared authority could be saved. Natural id from central authority is filled on retrieval.
#### Authority data statistics
Authority data statistics are propagated to member tenants. This allows user to query authority update reports in member tenants
including central tenant link count data.
To reflect shared authority changes for linked bib records in member tenants - linked shared authority updates are also handled for
member tenants. This means that link update events for local links are sent to member tenants when shared authority is updated in central tenant.
To include success/failures for statistics from central tenant - link update report events are also propagated to
member tenants. This allows member tenant reports to include success/failure counts from central tenant.
Authorities delete on central tenant also trigger data statistics deletion on member tenants.

## Additional Information
### Issue tracker

See project [MODELINKS](https://issues.folio.org/browse/MODELINKS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

### API Documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-entities-links).

### Module Documentation
This module's [Documentation](doc/documentation.md)

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-entities-links).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-entities-links/)

### Development tips

The development tips are described on the following page: [Development tips](doc/development.md)
