package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@ToString
@Table(name = "authority_data_stat")
@EntityListeners(AuditingEntityListener.class)
public class AuthorityDataStat implements Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @NotNull
  @ToString.Exclude
  @Column(name = "authority_id", nullable = false)
  private UUID authorityId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "action", nullable = false)
  private AuthorityDataStatAction action;

  @Column(name = "authority_natural_id_old")
  private String authorityNaturalIdOld;

  @Column(name = "authority_natural_id_new")
  private String authorityNaturalIdNew;

  @Column(name = "heading_old")
  private String headingOld;

  @Column(name = "heading_new")
  private String headingNew;

  @Column(name = "heading_type_old")
  private String headingTypeOld;

  @Column(name = "heading_type_new")
  private String headingTypeNew;

  @Column(name = "authority_source_file_old")
  private UUID authoritySourceFileOld;

  @Column(name = "authority_source_file_new")
  private UUID authoritySourceFileNew;

  @Column(name = "started_by_user_id")
  private UUID startedByUserId;

  @CreatedDate
  @Column(name = "started_at")
  private Timestamp startedAt;

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
    AuthorityDataStat that = (AuthorityDataStat) o;
    return id != null && Objects.equals(id, that.id);
  }
}
