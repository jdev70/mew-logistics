package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "mew_installation")
public class Installation extends BaseEntity {

    @Column(name = "installation_id", length = 20, unique = true)
    private String installationId;

    @Column(name = "installation_code", length = 30, unique = true)
    private String installationCode;

    @Column(name = "service_branch", length = 10, nullable = false)
    private String serviceBranch = "JOINT";

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "dodaac", length = 6, unique = true, nullable = false)
    private String dodaac;

    @Column(name = "city", length = 100)
    private String city = "";

    @Column(name = "state_or_country", length = 100)
    private String stateOrCountry = "";

    @Column(name = "location_display", length = 200)
    private String locationDisplay = "";

    @Column(name = "region", length = 100, nullable = false)
    private String region = "";

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "mission_category", length = 50)
    private String missionCategory = "";

    @Column(name = "primary_ew_focus", length = 255)
    private String primaryEwFocus = "";

    @Column(name = "dsn_prefix", length = 10)
    private String dsnPrefix = "";

    @Column(name = "max_ew_billets", nullable = false)
    private Integer maxEwBillets = 0;

    @Column(name = "scif_accredited", nullable = false)
    private boolean scifAccredited = true;

    @Column(name = "rf_anechoic_chamber", nullable = false)
    private boolean rfAnechoicChamber = false;

    @Column(name = "readiness_rating", length = 50)
    private String readinessRating = "";

    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;

    /** Public-source provenance for the installation reference record. */
    @Column(name = "catalog_source_id", length = 60)
    private String catalogSourceId = "";

    @Column(name = "catalog_source_url", length = 1000)
    private String catalogSourceUrl = "";

    @Column(name = "catalog_verified_on")
    private LocalDate catalogVerifiedOn;

    @Column(name = "public_association_type", length = 100)
    private String publicAssociationType = "";

    @Column(name = "catalog_confidence", length = 40)
    private String catalogConfidence = "PUBLIC_ATTRIBUTED";

    /** Reviewed public sources supporting this installation's catalog assertions. */
    @ManyToMany
    @JoinTable(name = "mew_installation_catalog_source",
            joinColumns = @JoinColumn(name = "installation_id"),
            inverseJoinColumns = @JoinColumn(name = "catalog_source_id"))
    private Set<CatalogSource> catalogSources = new LinkedHashSet<>();

    @Column(name = "synthetic_operational_data", nullable = false)
    private boolean syntheticOperationalData = true;

    @PrePersist
    @PreUpdate
    protected void populateLocationDisplay() {
        if ((locationDisplay == null || locationDisplay.isEmpty())
                && city != null && !city.isEmpty()
                && stateOrCountry != null && !stateOrCountry.isEmpty()) {
            this.locationDisplay = city + ", " + stateOrCountry;
        }
    }

    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
    public String getInstallationCode() { return installationCode; }
    public void setInstallationCode(String installationCode) { this.installationCode = installationCode; }
    public String getServiceBranch() { return serviceBranch; }
    public void setServiceBranch(String serviceBranch) { this.serviceBranch = serviceBranch; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDodaac() { return dodaac; }
    public void setDodaac(String dodaac) { this.dodaac = dodaac; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStateOrCountry() { return stateOrCountry; }
    public void setStateOrCountry(String stateOrCountry) { this.stateOrCountry = stateOrCountry; }
    public String getLocationDisplay() { return locationDisplay; }
    public void setLocationDisplay(String locationDisplay) { this.locationDisplay = locationDisplay; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getMissionCategory() { return missionCategory; }
    public void setMissionCategory(String missionCategory) { this.missionCategory = missionCategory; }
    public String getPrimaryEwFocus() { return primaryEwFocus; }
    public void setPrimaryEwFocus(String primaryEwFocus) { this.primaryEwFocus = primaryEwFocus; }
    public String getDsnPrefix() { return dsnPrefix; }
    public void setDsnPrefix(String dsnPrefix) { this.dsnPrefix = dsnPrefix; }
    public Integer getMaxEwBillets() { return maxEwBillets; }
    public void setMaxEwBillets(Integer maxEwBillets) { this.maxEwBillets = maxEwBillets; }
    public boolean isScifAccredited() { return scifAccredited; }
    public void setScifAccredited(boolean scifAccredited) { this.scifAccredited = scifAccredited; }
    public boolean isRfAnechoicChamber() { return rfAnechoicChamber; }
    public void setRfAnechoicChamber(boolean rfAnechoicChamber) { this.rfAnechoicChamber = rfAnechoicChamber; }
    public String getReadinessRating() { return readinessRating; }
    public void setReadinessRating(String readinessRating) { this.readinessRating = readinessRating; }
    public LocalDate getLastInspectionDate() { return lastInspectionDate; }
    public void setLastInspectionDate(LocalDate lastInspectionDate) { this.lastInspectionDate = lastInspectionDate; }
    public String getCatalogSourceId() { return catalogSourceId; }
    public void setCatalogSourceId(String catalogSourceId) { this.catalogSourceId = catalogSourceId; }
    public String getCatalogSourceUrl() { return catalogSourceUrl; }
    public void setCatalogSourceUrl(String catalogSourceUrl) { this.catalogSourceUrl = catalogSourceUrl; }
    public LocalDate getCatalogVerifiedOn() { return catalogVerifiedOn; }
    public void setCatalogVerifiedOn(LocalDate catalogVerifiedOn) { this.catalogVerifiedOn = catalogVerifiedOn; }
    public String getPublicAssociationType() { return publicAssociationType; }
    public void setPublicAssociationType(String publicAssociationType) { this.publicAssociationType = publicAssociationType; }
    public String getCatalogConfidence() { return catalogConfidence; }
    public void setCatalogConfidence(String catalogConfidence) { this.catalogConfidence = catalogConfidence; }
    public Set<CatalogSource> getCatalogSources() { return catalogSources; }
    public void setCatalogSources(Set<CatalogSource> catalogSources) { this.catalogSources = catalogSources; }
    public void addCatalogSource(CatalogSource catalogSource) { this.catalogSources.add(catalogSource); }
    public boolean isSyntheticOperationalData() { return syntheticOperationalData; }
    public void setSyntheticOperationalData(boolean syntheticOperationalData) { this.syntheticOperationalData = syntheticOperationalData; }
}
