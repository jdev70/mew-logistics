package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.CatalogFactCategory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDate;

/**
 * A reviewed assertion about one catalog target. Exactly one target is allowed so
 * the provenance, confidence, and rationale are never ambiguously shared.
 */
@Entity
@Table(name = "mew_catalog_attribution")
public class CatalogAttribution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_source_id", nullable = false)
    private CatalogSource catalogSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naval_hull_id")
    private NavalHull navalHull;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hull_system_assignment_id")
    private HullSystemAssignment hullSystemAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_category", nullable = false, length = 40)
    private CatalogFactCategory factCategory;

    @Column(name = "confidence", nullable = false, length = 40)
    private String confidence = "PUBLIC_ATTRIBUTED";

    @Column(name = "rationale", nullable = false, length = 2000)
    private String rationale = "";

    @Column(name = "reviewer", nullable = false, length = 150)
    private String reviewer = "";

    @Column(name = "reviewed_on", nullable = false)
    private LocalDate reviewedOn;

    @PrePersist
    @PreUpdate
    protected void validateTarget() {
        int targets = (installation == null ? 0 : 1)
                + (navalHull == null ? 0 : 1)
                + (hullSystemAssignment == null ? 0 : 1);
        if (catalogSource == null || targets != 1 || factCategory == null || reviewedOn == null
                || confidence == null || confidence.isBlank() || rationale == null || rationale.isBlank()
                || reviewer == null || reviewer.isBlank()) {
            throw new IllegalStateException("A catalog attribution requires a source, exactly one target, category, confidence, rationale, reviewer, and review date.");
        }
    }

    public CatalogSource getCatalogSource() { return catalogSource; }
    public void setCatalogSource(CatalogSource catalogSource) { this.catalogSource = catalogSource; }
    public Installation getInstallation() { return installation; }
    public void setInstallation(Installation installation) { this.installation = installation; }
    public NavalHull getNavalHull() { return navalHull; }
    public void setNavalHull(NavalHull navalHull) { this.navalHull = navalHull; }
    public HullSystemAssignment getHullSystemAssignment() { return hullSystemAssignment; }
    public void setHullSystemAssignment(HullSystemAssignment hullSystemAssignment) { this.hullSystemAssignment = hullSystemAssignment; }
    public CatalogFactCategory getFactCategory() { return factCategory; }
    public void setFactCategory(CatalogFactCategory factCategory) { this.factCategory = factCategory; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public LocalDate getReviewedOn() { return reviewedOn; }
    public void setReviewedOn(LocalDate reviewedOn) { this.reviewedOn = reviewedOn; }
}
