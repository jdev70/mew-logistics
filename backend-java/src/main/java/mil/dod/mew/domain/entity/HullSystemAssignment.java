package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "mew_hull_sys_assign")
public class HullSystemAssignment extends BaseEntity {

    @Column(name = "assignment_id", length = 20, unique = true, nullable = false)
    private String assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hull_id", nullable = false)
    private NavalHull hull;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private AssetModel system;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_role_id", nullable = false)
    private MissionRole missionRole;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "hull_number", length = 20)
    private String hullNumber = "";

    @Column(name = "vessel_name", length = 150)
    private String vesselName = "";

    @Column(name = "ship_class", length = 150)
    private String shipClass = "";

    @Column(name = "domain", length = 50)
    private String domain = "Surface";

    @Column(name = "homeport", length = 150)
    private String homeport = "";

    @Column(name = "nomenclature", length = 255)
    private String nomenclature = "";

    @Column(name = "common_name", length = 255)
    private String commonName = "";

    @Column(name = "ew_category", length = 100)
    private String ewCategory = "";

    @Column(name = "quantity_installed", nullable = false)
    private Integer quantityInstalled = 1;

    @Column(name = "role_code", length = 30)
    private String roleCode = "";

    @Column(name = "role_name", length = 150)
    private String roleName = "";

    @Column(name = "operational_status", length = 60)
    private String operationalStatus = "FOC (Full Operational Capability)";

    @Column(name = "installation_date")
    private LocalDate installationDate;

    /** Reviewed public sources supporting this platform-system association. */
    @ManyToMany
    @JoinTable(name = "mew_loadout_catalog_source",
            joinColumns = @JoinColumn(name = "loadout_id"),
            inverseJoinColumns = @JoinColumn(name = "catalog_source_id"))
    private Set<CatalogSource> catalogSources = new LinkedHashSet<>();

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
    public NavalHull getHull() { return hull; }
    public void setHull(NavalHull hull) { this.hull = hull; }
    public AssetModel getSystem() { return system; }
    public void setSystem(AssetModel system) { this.system = system; }
    public MissionRole getMissionRole() { return missionRole; }
    public void setMissionRole(MissionRole missionRole) { this.missionRole = missionRole; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public String getHullNumber() { return hullNumber; }
    public void setHullNumber(String hullNumber) { this.hullNumber = hullNumber; }
    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }
    public String getShipClass() { return shipClass; }
    public void setShipClass(String shipClass) { this.shipClass = shipClass; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getHomeport() { return homeport; }
    public void setHomeport(String homeport) { this.homeport = homeport; }
    public String getNomenclature() { return nomenclature; }
    public void setNomenclature(String nomenclature) { this.nomenclature = nomenclature; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getEwCategory() { return ewCategory; }
    public void setEwCategory(String ewCategory) { this.ewCategory = ewCategory; }
    public Integer getQuantityInstalled() { return quantityInstalled; }
    public void setQuantityInstalled(Integer quantityInstalled) { this.quantityInstalled = quantityInstalled; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }
    public Set<CatalogSource> getCatalogSources() { return catalogSources; }
    public void setCatalogSources(Set<CatalogSource> catalogSources) { this.catalogSources = catalogSources; }
    public void addCatalogSource(CatalogSource catalogSource) { this.catalogSources.add(catalogSource); }
}
