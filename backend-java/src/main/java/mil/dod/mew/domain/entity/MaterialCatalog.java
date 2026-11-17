package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mew_material_catalog")
public class MaterialCatalog extends BaseEntity {

    @Column(name = "component_id", length = 20, unique = true)
    private String componentId;

    @Column(name = "nsn", length = 16, unique = true, nullable = false)
    private String nsn;

    @Column(name = "nomenclature", length = 255, nullable = false)
    private String nomenclature;

    @Column(name = "part_number", length = 60)
    private String partNumber = "";

    @Column(name = "cage_code", length = 10)
    private String cageCode = "";

    @Column(name = "manufacturer", length = 150)
    private String manufacturer = "";

    @Column(name = "component_category", length = 100)
    private String componentCategory = "";

    @Column(name = "repairability_code", length = 50)
    private String repairabilityCode = "Depot_Repairable_LRU";

    @Column(name = "unit_of_issue", length = 4, nullable = false)
    private String unitOfIssue = "EA";

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "critical_item", nullable = false)
    private boolean criticalItem = false;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays = 14;

    @Column(name = "expected_mtbf_hours", nullable = false)
    private Double expectedMtbfHours = 2500.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_lru_id")
    private MaterialCatalog parentLru;

    @ManyToMany
    @JoinTable(
            name = "mew_material_compat_sys",
            joinColumns = @JoinColumn(name = "material_id"),
            inverseJoinColumns = @JoinColumn(name = "asset_model_id")
    )
    private Set<AssetModel> compatibleSystems = new HashSet<>();

    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }
    public String getNsn() { return nsn; }
    public void setNsn(String nsn) { this.nsn = nsn; }
    public String getNomenclature() { return nomenclature; }
    public void setNomenclature(String nomenclature) { this.nomenclature = nomenclature; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getCageCode() { return cageCode; }
    public void setCageCode(String cageCode) { this.cageCode = cageCode; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getComponentCategory() { return componentCategory; }
    public void setComponentCategory(String componentCategory) { this.componentCategory = componentCategory; }
    public String getRepairabilityCode() { return repairabilityCode; }
    public void setRepairabilityCode(String repairabilityCode) { this.repairabilityCode = repairabilityCode; }
    public String getUnitOfIssue() { return unitOfIssue; }
    public void setUnitOfIssue(String unitOfIssue) { this.unitOfIssue = unitOfIssue; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public boolean isCriticalItem() { return criticalItem; }
    public void setCriticalItem(boolean criticalItem) { this.criticalItem = criticalItem; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Double getExpectedMtbfHours() { return expectedMtbfHours; }
    public void setExpectedMtbfHours(Double expectedMtbfHours) { this.expectedMtbfHours = expectedMtbfHours; }
    public MaterialCatalog getParentLru() { return parentLru; }
    public void setParentLru(MaterialCatalog parentLru) { this.parentLru = parentLru; }
    public Set<AssetModel> getCompatibleSystems() { return compatibleSystems; }
    public void setCompatibleSystems(Set<AssetModel> compatibleSystems) { this.compatibleSystems = compatibleSystems; }
}
