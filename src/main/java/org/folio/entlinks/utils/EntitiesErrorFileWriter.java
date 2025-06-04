package org.folio.entlinks.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

public class EntitiesErrorFileWriter implements AutoCloseable {

  private final BufferedWriter errorEntitiesFileWriter;
  private final BufferedWriter errorsFileWriter;
  private final ObjectMapper objectMapper;

  public EntitiesErrorFileWriter(File errorEntitiesFileName, File errorsFileName, ObjectMapper objectMapper)
    throws IOException {
    this.errorEntitiesFileWriter = new BufferedWriter(new FileWriter(errorEntitiesFileName));
    this.errorsFileWriter = new BufferedWriter(new FileWriter(errorsFileName));
    this.objectMapper = objectMapper;
  }

  public <T> void write(T entity, Exception ex, Function<T, String> entityIdentifierGetter) throws IOException {
    errorEntitiesFileWriter.write(objectMapper.writeValueAsString(entity));
    errorEntitiesFileWriter.newLine();
    errorsFileWriter.write(entityIdentifierGetter.apply(entity) + ","
                           + ex.getMessage().replaceAll(System.lineSeparator(), " "));
    errorsFileWriter.newLine();
  }

  @Override
  public void close() throws Exception {
    errorEntitiesFileWriter.close();
    errorsFileWriter.close();
  }
}
