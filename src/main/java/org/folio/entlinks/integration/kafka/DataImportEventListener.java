package org.folio.entlinks.integration.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.ActionProfile;
import org.folio.AuthoritySourceFile;
import org.folio.Record;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.controller.delegate.AuthoritySourceFileServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.entlinks.integration.kafka.model.Event;
import org.folio.entlinks.integration.kafka.model.EventMetadata;
import org.folio.processing.mapping.defaultmapper.RecordMapperBuilder;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextService;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.spring.tools.config.properties.FolioEnvironment;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataImportEventListener {

  private static final String AUTHORITY_EXTENDED = "AUTHORITY_EXTENDED";
  private static final boolean IS_AUTHORITY_EXTENDED = isAuthorityExtendedMode();

  private static final String MAPPING_RULES = """
    {
      "001": [
        {
          "target": "identifiers.value",
          "description": "Control number",
          "subfield": [],
          "rules": []
        },
        {
          "target": "identifiers.identifierTypeId",
          "description": "Control number",
          "subfield": [],
          "rules": [
            {
              "conditions": [
                {
                  "type": "set_identifier_type_id_by_name",
                  "parameter": {
                    "name": "Control number"
                  }
                }
              ]
            }
          ]
        }
      ],
      "008": [
        {
          "target": "subjectHeadings",
          "description": "Children's subject headings",
          "subfield": [],
          "rules": [
            {
              "conditions": [
                {
                  "type": "char_select",
                  "parameter": {
                    "from": 11,
                    "to": 12
                  }
                }
              ]
            }
          ]
        }
      ],
      "010": [
        {
          "entityPerRepeatedSubfield": true,
          "keepTrailingBackslash": true,
          "entity": [
            {
              "target": "identifiers.identifierTypeId",
              "description": "Identifier Type for LCCN",
              "subfield": [
                "a"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "set_identifier_type_id_by_name",
                      "parameter": {
                        "name": "LCCN"
                      }
                    }
                  ]
                }
              ]
            },
            {
              "target": "identifiers.value",
              "description": "Library of Congress Control Number",
              "subfield": [
                "a"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "trim"
                    }
                  ]
                }
              ]
            },
            {
              "target": "identifiers.identifierTypeId",
              "description": "Identifier Type for Canceled LCCN",
              "subfield": [
                "z"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "set_identifier_type_id_by_name",
                      "parameter": {
                        "name": "Canceled LCCN"
                      }
                    }
                  ]
                }
              ]
            },
            {
              "target": "identifiers.value",
              "description": "Canceled Library of Congress Control Number",
              "subfield": [
                "z"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "trim"
                    }
                  ]
                }
              ]
            }
          ]
        }
      ],
      "024": [
        {
          "entity": [
            {
              "target": "identifiers.identifierTypeId",
              "description": "Type for Other Standard Identifier",
              "applyRulesOnConcatenatedData": true,
              "subfield": [
                "a",
                "c",
                "d",
                "q",
                "z",
                "0",
                "1",
                "2"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "set_identifier_type_id_by_name",
                      "parameter": {
                        "name": "Other standard identifier"
                      }
                    }
                  ]
                }
              ]
            },
            {
              "target": "identifiers.value",
              "description": "Other Standard Identifier",
              "applyRulesOnConcatenatedData": true,
              "subfield": [
                "a",
                "c",
                "d",
                "q",
                "z",
                "0",
                "1",
                "2"
              ],
              "rules": []
            }
          ]
        }
      ],
      "035": [
        {
          "entityPerRepeatedSubfield": true,
          "entity": [
            {
              "target": "identifiers.identifierTypeId",
              "description": "Type for System Control Number",
              "subfield": [
                "a",
                "z"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "set_identifier_type_id_by_value",
                      "parameter": {
                        "names": [
                          "System control number"
                        ]
                      }
                    }
                  ]
                }
              ]
            },
            {
              "target": "identifiers.value",
              "description": "System Control Number",
              "subfield": [
                "a",
                "z"
              ]
            }
          ]
        }
      ],
      "100": [
        {
          "target": "personalName",
          "description": "Heading personal Name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "q",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "applyRulesOnConcatenatedData": true,
          "rules": []
        },
        {
          "target": "personalNameTitle",
          "description": "Heading personal name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "applyRulesOnConcatenatedData": true,
          "rules": []
        }
      ],
      "110": [
        {
          "target": "corporateName",
          "description": "Heading corporate name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "n",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "corporateNameTitle",
          "description": "Heading corporate name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "111": [
        {
          "target": "meetingName",
          "description": "Heading meeting name",
          "subfield": [
            "a",
            "c",
            "d",
            "n",
            "q",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "meetingNameTitle",
          "description": "Heading meeting name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "130": [
        {
          "target": "uniformTitle",
          "description": "Heading uniform title",
          "subfield": [
            "a",
            "d",
            "f",
            "g",
            "h",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "r",
            "s",
            "t",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "147": [
        {
          "target": "namedEvent",
          "description": "Heading named event",
          "subfield": [
            "a",
            "c",
            "d",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "148": [
        {
          "target": "chronTerm",
          "description": "Heading chronological term",
          "subfield": [
            "a",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "150": [
        {
          "target": "topicalTerm",
          "description": "Heading topical term",
          "subfield": [
            "a",
            "b",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "151": [
        {
          "target": "geographicName",
          "description": "Heading geographic name",
          "subfield": [
            "a",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "155": [
        {
          "target": "genreTerm",
          "description": "Heading genre/form term",
          "subfield": [
            "a",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "162": [
        {
          "target": "mediumPerfTerm",
          "description": "Heading medium of performance term",
          "subfield": [
            "a"
          ],
          "rules": []
        }
      ],
      "180": [
        {
          "target": "generalSubdivision",
          "description": "Heading general subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "181": [
        {
          "target": "geographicSubdivision",
          "description": "Heading geographic subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "182": [
        {
          "target": "chronSubdivision",
          "description": "Heading chronological subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "185": [
        {
          "target": "formSubdivision",
          "description": "Heading form subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "400": [
        {
          "target": "sftPersonalName",
          "description": "See from tracing personal Name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "q",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "sftPersonalNameTitle",
          "description": "See from tracing personal name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "410": [
        {
          "target": "sftCorporateName",
          "description": "See from tracing corporate name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "n",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "sftCorporateNameTitle",
          "description": "See from tracing corporate name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "411": [
        {
          "target": "sftMeetingName",
          "description": "See from tracing meeting name",
          "subfield": [
            "a",
            "c",
            "d",
            "n",
            "q",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "sftMeetingNameTitle",
          "description": "See from tracing meeting name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "430": [
        {
          "target": "sftUniformTitle",
          "description": "See from tracing uniform title",
          "subfield": [
            "a",
            "d",
            "f",
            "g",
            "h",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "r",
            "s",
            "t",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "447": [
        {
          "target": "sftNamedEvent",
          "description": "See from tracing named event",
          "subfield": [
            "a",
            "c",
            "d",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "448": [
        {
          "target": "sftChronTerm",
          "description": "See from tracing chronological term",
          "subfield": [
            "a",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "450": [
        {
          "target": "sftTopicalTerm",
          "description": "See from tracing topical term",
          "subfield": [
            "a",
            "b",
            "g",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "451": [
        {
          "target": "sftGeographicName",
          "description": "See from tracing geographic name",
          "subfield": [
            "a",
            "g",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "455": [
        {
          "target": "sftGenreTerm",
          "description": "See from tracing genre/form term",
          "subfield": [
            "a",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "462": [
        {
          "target": "sftMediumPerfTerm",
          "description": "See from tracing medium of performance term",
          "subfield": [
            "a"
          ],
          "rules": []
        }
      ],
      "480": [
        {
          "target": "sftGeneralSubdivision",
          "description": "See from tracing general subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "481": [
        {
          "target": "sftGeographicSubdivision",
          "description": "See from tracing geographic subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "482": [
        {
          "target": "sftChronSubdivision",
          "description": "See from tracing chronological subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "485": [
        {
          "target": "sftFormSubdivision",
          "description": "See from tracing form subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "500": [
        {
          "target": "saftPersonalName",
          "description": "See also from tracing personal Name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "q",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "saftPersonalNameTitle",
          "description": "See also from tracing personal name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "510": [
        {
          "target": "saftCorporateName",
          "description": "See also from tracing corporate name",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "g",
            "n",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "saftCorporateNameTitle",
          "description": "See also from tracing corporate name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "511": [
        {
          "target": "saftMeetingName",
          "description": "See also from tracing meeting name",
          "subfield": [
            "a",
            "c",
            "d",
            "n",
            "q",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "exclusiveSubfield": ["t"],
          "rules": []
        },
        {
          "target": "saftMeetingNameTitle",
          "description": "See also from tracing meeting name title",
          "subfield": [
            "a",
            "b",
            "c",
            "d",
            "f",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t"
          ],
          "requiredSubfield": ["t"],
          "rules": []
        }
      ],
      "530": [
        {
          "target": "saftUniformTitle",
          "description": "See also from tracing uniform title",
          "subfield": [
            "a",
            "d",
            "f",
            "g",
            "h",
            "i",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "r",
            "s",
            "t",
            "v",
            "x",
            "y",
            "z",
            "4"
          ],
          "rules": []
        }
      ],
      "547": [
        {
          "target": "saftNamedEvent",
          "description": "See also from tracing named event",
          "subfield": [
            "a",
            "c",
            "d",
            "g",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "548": [
        {
          "target": "saftChronTerm",
          "description": "See also from tracing chronological term",
          "subfield": [
            "a",
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "550": [
        {
          "target": "saftTopicalTerm",
          "description": "See also from tracing topical term",
          "subfield": [
            "a",
            "b",
            "g",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "551": [
        {
          "target": "saftGeographicName",
          "description": "See also from tracing geographic name",
          "subfield": [
            "a",
            "g",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "555": [
        {
          "target": "saftGenreTerm",
          "description": "See also from tracing genre/form term",
          "subfield": [
            "a",
            "i",
            "v",
            "x",
            "y",
            "z",
            "4",
            "5"
          ],
          "rules": []
        }
      ],
      "562": [
        {
          "target": "saftMediumPerfTerm",
          "description": "See also from tracing medium of performance term",
          "subfield": [
            "a"
          ],
          "rules": []
        }
      ],
      "580": [
        {
          "target": "saftGeneralSubdivision",
          "description": "See also from tracing general subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "581": [
        {
          "target": "saftGeographicSubdivision",
          "description": "See also from tracing geographic subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "582": [
        {
          "target": "saftChronSubdivision",
          "description": "See also from tracing chronological subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "585": [
        {
          "target": "saftFormSubdivision",
          "description": "See also from tracing form subdivision",
          "subfield": [
            "v",
            "x",
            "y",
            "z"
          ],
          "rules": []
        }
      ],
      "667": [
        {
          "entity": [
            {
              "target": "notes.noteTypeId",
              "description": "Authority note type id",
              "subfield": [
                "a"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "set_authority_note_type_id",
                      "parameter": {
                        "name": "Nonpublic general note"
                      }
                    }
                  ]
                }
              ]
            },
            {
              "target": "notes.note",
              "description": "Authority note",
              "subfield": [
                "a"
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "type": "trim"
                    }
                  ]
                }
              ]
            }
          ]
        }
      ],
      "999": [
        {
          "target": "id",
          "description": "Authority id",
          "subfield": [
            "i"
          ],
          "rules": []
        }
      ]
    }
    """;

  private final FolioExecutionContextService executionService;
  private final MessageBatchProcessor messageBatchProcessor;
  private final ObjectMapper objectMapper;
  private final AuthorityServiceDelegate delegate;
  private final KafkaTemplate<String, Event> kafkaTemplate;
  private final AuthoritySourceFileServiceDelegate sourceFileServiceDelegate;

  @KafkaListener(id = "mod-entities-links-data-import-listener",
                 containerFactory = "diListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['data-import'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['data-import'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['data-import'].concurrency}")
  public void handleEvents(List<DataImportEventWrapper> consumerRecords) {
    log.info("Processing data-import event [number of records: {}]", consumerRecords.size());
    var enventByTenant = consumerRecords.stream()
      .collect(Collectors.groupingBy(DataImportEventWrapper::tenant));
    for (var entry : enventByTenant.entrySet()) {
      var tenant = entry.getKey();
      var records = entry.getValue();
      executionService.execute(tenant, Map.of(XOkapiHeaders.TENANT, List.of(tenant)),
        () -> processEvents(tenant, records));
    }
  }

  private Object processEvents(String tenant, List<DataImportEventWrapper> diEvents) {
    for (var diEvent : diEvents) {
      var payload = diEvent.payload();
      var authorityJson = payload.getContext().get("MARC_AUTHORITY");
      try {
        var srsRecord =
          new JsonObject((String) objectMapper.readValue(authorityJson, Record.class).getParsedRecord().getContent());
        var recordMapper = IS_AUTHORITY_EXTENDED
                           ? RecordMapperBuilder.buildMapper(ActionProfile.FolioRecord.MARC_AUTHORITY_EXTENDED.value())
                           : RecordMapperBuilder.buildMapper("MARC_AUTHORITY");

        var authoritySourceFiles = sourceFileServiceDelegate.getAuthoritySourceFiles(0, 1000, null)
          .getAuthoritySourceFiles()
          .stream()
          .map(authoritySourceFileDto -> {
            try {
              return objectMapper.readValue(objectMapper.writeValueAsString(authoritySourceFileDto),
                AuthoritySourceFile.class);
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          })
          .toList();
        var mappingParameters = new MappingParameters();
        mappingParameters.setAuthoritySourceFiles(authoritySourceFiles);
        var rawAuthority = recordMapper.mapRecord(srsRecord, mappingParameters, new JsonObject(MAPPING_RULES));
        var authority = objectMapper.readValue(objectMapper.writeValueAsString(rawAuthority), AuthorityDto.class);
        authority.setSource("MARC");
        var createdAuthority = delegate.createAuthority(authority);
        payload.getContext().put("AUTHORITY", objectMapper.writeValueAsString(createdAuthority));
        var event = new Event();
        //        var eventName = "DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING";
        var eventName = "DI_COMPLETED";
        event.setEventType(eventName);
        event.setEventPayload(objectMapper.writeValueAsString(payload));
        event.setId(UUID.randomUUID().toString());
        var eventMetadata = new EventMetadata();
        eventMetadata.setTenantId(diEvent.tenant());
        eventMetadata.setPublishedBy("mod-entities-links");
        event.setEventMetadata(eventMetadata);

        var authorityCreated =
          new ProducerRecord<String, Event>(KafkaUtils
            .getTenantTopicNameWithNamespace(eventName, FolioEnvironment.getFolioEnvName(), tenant, "Default"),
            event);
        diEvent.headers()
          .forEach((key, value) -> authorityCreated.headers().add(new RecordHeader(key, value.getBytes())));

        kafkaTemplate.send(authorityCreated);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  private static boolean isAuthorityExtendedMode() {
    return Boolean.parseBoolean(
      System.getenv().getOrDefault(AUTHORITY_EXTENDED, "false"));
  }
}
