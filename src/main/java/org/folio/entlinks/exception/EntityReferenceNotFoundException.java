package org.folio.entlinks.exception;

public final class EntityReferenceNotFoundException extends RuntimeException {

  private EntityReferenceNotFoundException(String entityName) {
    super(entityName + " with the given 'id' does not exists.");
  }

  public static EntityReferenceNotFoundException forAuthoritySourceFile() {
    return new EntityReferenceNotFoundException("Authority Source File");
  }
}
