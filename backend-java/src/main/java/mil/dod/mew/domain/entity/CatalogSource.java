package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

/**
 * A reviewed public-source manifest entry. It contains attribution metadata only;
 * it must never contain operational, personnel, or credential data.
 */
@Entity
@Table(name = "mew_catalog_source")
public class CatalogSource extends BaseEntity {

    @Column(name = "source_id", nullable = false, unique = true, length = 60)
    private String sourceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_version", length = 100)
    private String sourceVersion = "";

    @Column(name = "retrieved_on")
    private LocalDate retrievedOn;

    @Column(name = "license_notes", length = 1000)
    private String licenseNotes = "";

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "DRAFT";

    @Column(name = "last_reviewed_on")
    private LocalDate lastReviewedOn;

    @Column(name = "reviewer", length = 150)
    private String reviewer = "";

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public LocalDate getRetrievedOn() { return retrievedOn; }
    public void setRetrievedOn(LocalDate retrievedOn) { this.retrievedOn = retrievedOn; }
    public String getLicenseNotes() { return licenseNotes; }
    public void setLicenseNotes(String licenseNotes) { this.licenseNotes = licenseNotes; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public LocalDate getLastReviewedOn() { return lastReviewedOn; }
    public void setLastReviewedOn(LocalDate lastReviewedOn) { this.lastReviewedOn = lastReviewedOn; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
}
