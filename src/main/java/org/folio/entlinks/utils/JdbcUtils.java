package org.folio.entlinks.utils;

import static java.util.Collections.nCopies;

import lombok.experimental.UtilityClass;
import org.folio.spring.FolioExecutionContext;

@UtilityClass
public class JdbcUtils {

  public static String getSchemaName(FolioExecutionContext context) {
    return context.getFolioModuleMetadata().getDBSchemaName(context.getTenantId());
  }

  public static String getFullPath(FolioExecutionContext context, String tableName) {
    return getSchemaName(context) + "." + tableName;
  }

  public static String getParamPlaceholder(int size) {
    return String.join(",", nCopies(size, "?"));
  }

}
