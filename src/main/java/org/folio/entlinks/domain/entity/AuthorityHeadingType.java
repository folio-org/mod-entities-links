package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@Table(name = "authority_heading_type", uniqueConstraints = {
  @UniqueConstraint(name = "authority_heading_type_name_unq", columnNames = {"name"}),
  @UniqueConstraint(name = "authority_heading_type_code_unq", columnNames = {"code"})
})
public class AuthorityHeadingType implements Persistable<UUID>, Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "code", nullable = false, unique = true, length = 100)
  private String code;

  @Column(name = "queryable", nullable = false)
  private Boolean queryable;

  @Transient
  private boolean isNew = true;

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }
}
