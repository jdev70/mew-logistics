package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.ReadinessLevel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mew_asset")
public class Asset extends BaseEntity {

    @Column(name = "assignment_id", length = 20, unique = true)
    private String assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_model_id", nullable = false)
    private AssetModel assetModel;

    @Column(name = "serial_number", length = 100, unique = true, nullable = false)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_unit_id", nullable = false)
    private MilitaryUnit assignedUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private Installation currentLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hull_id")
    private NavalHull hull;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_role_id")
    private MissionRole missionRole;

    @Column(name = "quantity_installed", nullable = false)
    private Integer quantityInstalled = 1;

    @Column(name = "operational_status", length = 60, nullable = false)
    private String operationalStatus = "FOC (Full Operational Capability)";

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", length = 4, nullable = false)
    private ReadinessLevel readinessStatus = ReadinessLevel.FMC;

    @Column(name = "last_status_change", nullable = false)
    private Instant lastStatusChange = Instant.now();

    @Column(name = "engine_hours", precision = 10, scale = 2, nullable = false)
    private BigDecimal engineHours = BigDecimal.ZERO;

    @Column(name = "mtbf_hours", nullable = false)
    private Double mtbfHours = 0.0;

    @Column(name = "mttr_hours", nullable = false)
    private Double mttrHours = 0.0;

    @Column(name = "operational_availability_pct", nullable = false)
    private Double operationalAvailabilityPct = 100.0;

    @Column(name = "domain", length = 50)
    private String domain;

    @Column(name = "service_branch", length = 10)
    private String serviceBranch;

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
    public AssetModel getAssetModel() { return assetModel; }
    public void setAssetModel(AssetModel assetModel) { this.assetModel = assetModel; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public MilitaryUnit getAssignedUnit() { return assignedUnit; }
    public void setAssignedUnit(MilitaryUnit assignedUnit) { this.assignedUnit = assignedUnit; }
    public Installation getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Installation currentLocation) { this.currentLocation = currentLocation; }
    public NavalHull getHull() { return hull; }
    public void setHull(NavalHull hull) { this.hull = hull; }
    public MissionRole getMissionRole() { return missionRole; }
    public void setMissionRole(MissionRole missionRole) { this.missionRole = missionRole; }
    public Integer getQuantityInstalled() { return quantityInstalled; }
    public void setQuantityInstalled(Integer quantityInstalled) { this.quantityInstalled = quantityInstalled; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }
    public ReadinessLevel getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(ReadinessLevel readinessStatus) { this.readinessStatus = readinessStatus; }
    public Instant getLastStatusChange() { return lastStatusChange; }
    public void setLastStatusChange(Instant lastStatusChange) { this.lastStatusChange = lastStatusChange; }
    public BigDecimal getEngineHours() { return engineHours; }
    public void setEngineHours(BigDecimal engineHours) { this.engineHours = engineHours; }
    public Double getMtbfHours() { return mtbfHours; }
    public void setMtbfHours(Double mtbfHours) { this.mtbfHours = mtbfHours; }
    public Double getMttrHours() { return mttrHours; }
    public void setMttrHours(Double mttrHours) { this.mttrHours = mttrHours; }
    public Double getOperationalAvailabilityPct() { return operationalAvailabilityPct; }
    public void setOperationalAvailabilityPct(Double operationalAvailabilityPct) { this.operationalAvailabilityPct = operationalAvailabilityPct; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getServiceBranch() { return serviceBranch; }
    public void setServiceBranch(String serviceBranch) { this.serviceBranch = serviceBranch; }
}
