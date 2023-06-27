package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "authority_data")
public class AuthorityData extends AuditableEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @NotNull
  @Column(name = "natural_id", nullable = false, length = 100)
  private String naturalId;

  @Column(name = "state")
  private boolean deleted;

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    AuthorityData that = (AuthorityData) o;
    return id != null && Objects.equals(id, that.id);
  }
}
