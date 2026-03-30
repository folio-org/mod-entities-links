package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Strings;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.folio.entlinks.utils.ConsortiumUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "authority")
public class Authority extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  public static final String ID_COLUMN = "id";
  public static final String NATURAL_ID_COLUMN = "natural_id";
  public static final String SOURCE_FILE_COLUMN = "source_file_id";
  public static final String SOURCE_COLUMN = "source";
  public static final String HEADING_COLUMN = "heading";
  public static final String HEADING_TYPE_COLUMN = "heading_type";
  public static final String VERSION_COLUMN = "_version";
  public static final String SUBJECT_HEADING_CODE_COLUMN = "subject_heading_code";
  public static final String SFT_HEADINGS_COLUMN = "sft_headings";
  public static final String SAFT_HEADINGS_COLUMN = "saft_headings";
  public static final String IDENTIFIERS_COLUMN = "identifiers";
  public static final String NOTES_COLUMN = "notes";
  public static final String DELETED_COLUMN = "deleted";

  @Id
  @Column(name = ID_COLUMN, nullable = false)
  private UUID id;

  @Column(name = NATURAL_ID_COLUMN)
  private String naturalId;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SOURCE_FILE_COLUMN, nullable = false)
  private AuthoritySourceFile authoritySourceFile;

  @Column(name = SOURCE_COLUMN)
  private String source;

  @Column(name = HEADING_COLUMN)
  private String heading;

  @Column(name = HEADING_TYPE_COLUMN)
  private String headingType;

  @Version
  @Column(name = VERSION_COLUMN, nullable = false)
  private int version;

  @Column(name = SUBJECT_HEADING_CODE_COLUMN)
  private Character subjectHeadingCode;

  @Column(name = SFT_HEADINGS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> sftHeadings;

  @Column(name = SAFT_HEADINGS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> saftHeadings;

  @Column(name = IDENTIFIERS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityIdentifier> identifiers;

  @Column(name = NOTES_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityNote> notes;

  @Column(name = DELETED_COLUMN)
  private boolean deleted = false;

  @Transient
  private boolean isNew = true;

  public Authority(Authority other) {
    super(other);
    this.id = other.id;
    this.naturalId = other.naturalId;
    this.authoritySourceFile = copyAuthoritySourceFile(other.authoritySourceFile);
    this.source = other.source;
    this.heading = other.heading;
    this.headingType = other.headingType;
    this.version = other.version;
    this.subjectHeadingCode = other.subjectHeadingCode;
    this.sftHeadings = copyHeadings(other.getSftHeadings());
    this.saftHeadings = copyHeadings(other.getSaftHeadings());
    this.identifiers = copyIdentifiers(other);
    this.notes = copyNotes(other);
    this.deleted = other.deleted;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Authority that = (Authority) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @PrePersist
  void prePersist() {
    this.isNew = false;
  }

  @PostLoad
  void postLoad() {
    this.isNew = false;
  }

  private @NonNull List<AuthorityNote> copyNotes(Authority other) {
    return Optional.ofNullable(other.getNotes()).orElse(List.of()).stream()
      .map(AuthorityNote::new)
      .toList();
  }

  private @NonNull List<AuthorityIdentifier> copyIdentifiers(Authority other) {
    return Optional.ofNullable(other.getIdentifiers()).orElse(List.of()).stream()
      .map(AuthorityIdentifier::new)
      .toList();
  }

  private @NonNull List<HeadingRef> copyHeadings(List<HeadingRef> other) {
    return Optional.ofNullable(other).orElse(List.of()).stream()
      .map(HeadingRef::new)
      .toList();
  }

  private @Nullable AuthoritySourceFile copyAuthoritySourceFile(AuthoritySourceFile sourceFile) {
    return Optional.ofNullable(sourceFile)
      .map(sf -> Hibernate.unproxy(sf, AuthoritySourceFile.class))
      .map(AuthoritySourceFile::new)
      .orElse(null);
  }

  public static AuthorityBuilder builder() {
    return new AuthorityBuilder();
  }

  public static class AuthorityBuilder {
    private UUID id;
    private String naturalId;
    private AuthoritySourceFile authoritySourceFile;
    private String source;
    private String heading;
    private String headingType;
    private int version;
    private Character subjectHeadingCode;
    private List<HeadingRef> sftHeadings;
    private List<HeadingRef> saftHeadings;
    private List<AuthorityIdentifier> identifiers;
    private List<AuthorityNote> notes;
    private boolean deleted;
    private Timestamp updatedDate;
    private Timestamp createdDate;
    private UUID createdByUserId;
    private UUID updatedByUserId;

    AuthorityBuilder() {
    }

    public AuthorityBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public AuthorityBuilder naturalId(String naturalId) {
      this.naturalId = naturalId;
      return this;
    }

    public AuthorityBuilder authoritySourceFile(AuthoritySourceFile authoritySourceFile) {
      this.authoritySourceFile = authoritySourceFile;
      return this;
    }

    public AuthorityBuilder source(String source) {
      this.source = source;
      return this;
    }

    public AuthorityBuilder heading(String heading) {
      this.heading = heading;
      return this;
    }

    public AuthorityBuilder headingType(String headingType) {
      this.headingType = headingType;
      return this;
    }

    public AuthorityBuilder version(int version) {
      this.version = version;
      return this;
    }

    public AuthorityBuilder subjectHeadingCode(Character subjectHeadingCode) {
      this.subjectHeadingCode = subjectHeadingCode;
      return this;
    }

    public AuthorityBuilder sftHeadings(List<HeadingRef> sftHeadings) {
      this.sftHeadings = sftHeadings;
      return this;
    }

    public AuthorityBuilder saftHeadings(List<HeadingRef> saftHeadings) {
      this.saftHeadings = saftHeadings;
      return this;
    }

    public AuthorityBuilder identifiers(List<AuthorityIdentifier> identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    public AuthorityBuilder notes(List<AuthorityNote> notes) {
      this.notes = notes;
      return this;
    }

    public AuthorityBuilder deleted(boolean deleted) {
      this.deleted = deleted;
      return this;
    }

    public AuthorityBuilder updatedDate(Timestamp updatedDate) {
      this.updatedDate = updatedDate;
      return this;
    }

    public AuthorityBuilder createdDate(Timestamp createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public AuthorityBuilder updatedByUserId(UUID updatedBy) {
      this.updatedByUserId = updatedBy;
      return this;
    }

    public AuthorityBuilder createdByUserId(UUID createdBy) {
      this.createdByUserId = createdBy;
      return this;
    }

    public Authority build() {
      var authority = new Authority();
      authority.setId(id);
      authority.setNaturalId(naturalId);
      authority.setAuthoritySourceFile(authoritySourceFile);
      authority.setSource(source);
      authority.setHeading(heading);
      authority.setHeadingType(headingType);
      authority.setVersion(version);
      authority.setSubjectHeadingCode(subjectHeadingCode);
      authority.setSftHeadings(sftHeadings);
      authority.setSaftHeadings(saftHeadings);
      authority.setIdentifiers(identifiers);
      authority.setNotes(notes);
      authority.setDeleted(deleted);
      authority.setUpdatedDate(updatedDate);
      authority.setCreatedDate(createdDate);
      authority.setUpdatedByUserId(updatedByUserId);
      authority.setCreatedByUserId(createdByUserId);
      return authority;
    }

    public String toString() {
      return "Authority.AuthorityBuilder("
          + "id=" + this.id
          + ", naturalId=" + this.naturalId
          + ", authoritySourceFile=" + this.authoritySourceFile
          + ", source=" + this.source
          + ", heading=" + this.heading
          + ", headingType=" + this.headingType
          + ", version=" + this.version
          + ", subjectHeadingCode=" + this.subjectHeadingCode
          + ", sftHeadings=" + this.sftHeadings
          + ", saftHeadings=" + this.saftHeadings
          + ", identifiers=" + this.identifiers
          + ", notes=" + this.notes
          + ", deleted=" + this.deleted
          + ")";
    }
  }
}
