package org.folio.entlinks.integration.dto;

import io.vertx.core.json.JsonObject;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;

public record MappingMetadata(MappingParameters mappingParameters, JsonObject mappingRules) { }
