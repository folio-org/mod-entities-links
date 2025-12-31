package org.folio.entlinks.utils;

import static java.util.Collections.nCopies;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

@UtilityClass
public class JdbcUtils {

  public static String getSchemaName(FolioExecutionContext context) {
    return context.getFolioModuleMetadata().getDBSchemaName(context.getTenantId());
  }

  public static String getSchemaName(FolioExecutionContext context, String tenantId) {
    return context.getFolioModuleMetadata().getDBSchemaName(tenantId);
  }

  public static String getFullPath(FolioExecutionContext context, String tableName) {
    return getSchemaName(context) + "." + tableName;
  }

  public static String getFullPath(FolioModuleMetadata folioModuleMetadata, String tenant, String tableName) {
    return folioModuleMetadata.getDBSchemaName(tenant) + "." + tableName;
  }

  public static String getParamPlaceholder(int size) {
    return String.join(",", nCopies(size, "?"));
  }

  public static String getParamPlaceholder(int size, int paramsNum) {
    if (size < 1 || paramsNum < 1) {
      throw new IllegalArgumentException("Size and paramsNum must be greater than 0");
    }
    var paramGroups = IntStream.range(0, paramsNum)
      .mapToObj(i -> "?")
      .collect(Collectors.joining(","));
    return String.join(",", nCopies(size, '(' + paramGroups + ')'));
  }
}
