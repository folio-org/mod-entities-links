package org.folio.entlinks.service.authority;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.utils.EntitiesErrorFileWriter;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityS3Service {

  private final BulkAuthorityS3Client s3Client;
  private final ObjectMapper objectMapper;
  private final AuthorityMapper mapper;

  public int processAuthorities(AuthoritiesBulkContext bulkContext,
                                Consumer<List<Authority>> bulkConsumer) {
    log.info("processAuthorities::Processing bulk authority request [filename: {}]", bulkContext.getInitialFilePath());
    AtomicInteger errorCounter = new AtomicInteger();
    var stringAuthorities = s3Client.readFile(bulkContext.getInitialFilePath());
    var invalidFormatAuthorities = new LinkedList<Map.Entry<String, Exception>>();
    var authorities = parseAuthorities(stringAuthorities, invalidFormatAuthorities);
    var failedAuthorities = new LinkedList<Map.Entry<Authority, Exception>>();

    try {
      bulkConsumer.accept(authorities);
    } catch (Exception e) {
      log.error("processAuthorities::Batch failed: [message: {}]", e.getMessage());
      log.debug("processAuthorities::Batch failed", e);

      for (var authority : authorities) {
        try {
          bulkConsumer.accept(List.of(authority));
        } catch (Exception ex) {
          failedAuthorities.add(Map.entry(authority, ex));
        }
      }
    }

    processFailures(invalidFormatAuthorities, failedAuthorities, errorCounter, bulkContext);

    try {
      bulkContext.deleteLocalFiles();
    } catch (IOException e) {
      log.error("processAuthorities::Exception during temp files deletion: ", e);
    }
    return errorCounter.get();
  }

  private List<Authority> parseAuthorities(List<String> stringAuthorities,
                                           List<Map.Entry<String, Exception>> invalidFormatAuthorities) {
    var authorities = new LinkedList<Authority>();

    for (String stringAuthority : stringAuthorities) {
      try {
        var authorityDto = toAuthorityDto(stringAuthority);
        var authority = mapper.toEntity(authorityDto);
        authorities.add(authority);
      } catch (Exception ex) {
        invalidFormatAuthorities.add(Map.entry(stringAuthority, ex));
      }
    }

    return authorities;
  }

  private void processFailures(List<Map.Entry<String, Exception>> invalidFormatAuthorities,
                               List<Map.Entry<Authority, Exception>> failedAuthorities,
                               AtomicInteger errorCounter, AuthoritiesBulkContext bulkContext) {

    if (invalidFormatAuthorities.isEmpty() && failedAuthorities.isEmpty()) {
      return;
    }

    log.warn("processFailures:: [file: {}, invalid format count: {}, process failures count: {}]",
      bulkContext.getInitialFilePath(), invalidFormatAuthorities.size(), failedAuthorities.size());
    try (var writer = new EntitiesErrorFileWriter(bulkContext.getLocalFailedEntitiesFile(),
      bulkContext.getLocalErrorsFile(), objectMapper)) {
      for (var authority : invalidFormatAuthorities) {
        errorCounter.incrementAndGet();
        writer.write(authority.getKey(), authority.getValue(),
          a -> stringAuthorityIdGetter(bulkContext.getInitialFilePath(), a));
      }
      for (var authority : failedAuthorities) {
        errorCounter.incrementAndGet();
        writer.write(mapper.toDto(authority.getKey()), authority.getValue(), a -> a.getId().toString());
      }
    } catch (Exception ex) {
      log.error("processFailures::Processing bulk authority request failed.", ex);
    }

    s3Client.uploadErrorFiles(bulkContext);
  }

  private String stringAuthorityIdGetter(String initialFilePath, String stringAuthority) {
    try {
      var authorityJson = objectMapper.readValue(stringAuthority, HashMap.class);
      return String.valueOf(authorityJson.get("id"));
    } catch (Exception ex) {
      log.warn("stringAuthorityIdGetter:: unable to get authority id [file: {}, authority string: {}]",
        initialFilePath, stringAuthority);
      return null;
    }
  }

  private AuthorityDto toAuthorityDto(String s) {
    try {
      return objectMapper.readValue(s, AuthorityDto.class);
    } catch (JacksonException e) {
      throw new FolioIntegrationException("Unexpected json parsing exception", e);
    }
  }
}
