package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@ToString
@Table(name = "authority_data_stat")
public class AuthorityDataStat extends AuditableEntity implements Identifiable<UUID> {

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

  @Column(name = "lb_total")
  private int lbTotal;

  @Column(name = "lb_updated")
  private int lbUpdated;

  @Column(name = "lb_failed")
  private int lbFailed;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false)
  private AuthorityDataStatStatus status;

  @Column(name = "fail_cause")
  private String failCause;

  @Column(name = "started_by_user_id")
  private UUID startedByUserId;

  @CreatedDate
  @Column(name = "started_at")
  private Timestamp startedAt;

  @Column(name = "completed_at")
  private Timestamp completedAt;

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

  public AuthorityDataStat copy() {
    var copy = new AuthorityDataStat();
    copy.setId(this.getId());
    copy.setAuthorityId(this.getAuthorityId());
    copy.setAction(this.getAction());
    copy.setAuthorityNaturalIdOld(this.getAuthorityNaturalIdOld());
    copy.setAuthorityNaturalIdNew(this.getAuthorityNaturalIdNew());
    copy.setHeadingOld(this.getHeadingOld());
    copy.setHeadingNew(this.getHeadingNew());
    copy.setHeadingTypeOld(this.getHeadingTypeOld());
    copy.setHeadingTypeNew(this.getHeadingTypeNew());
    copy.setAuthoritySourceFileOld(this.getAuthoritySourceFileOld());
    copy.setAuthoritySourceFileNew(this.getAuthoritySourceFileNew());
    copy.setLbTotal(this.getLbTotal());
    copy.setLbUpdated(this.getLbUpdated());
    copy.setLbFailed(this.getLbFailed());
    copy.setStatus(this.getStatus());
    copy.setFailCause(this.getFailCause());
    copy.setStartedByUserId(this.getStartedByUserId());
    copy.setStartedAt(this.getStartedAt());
    copy.setCompletedAt(this.getCompletedAt());
    return copy;
  }
}
