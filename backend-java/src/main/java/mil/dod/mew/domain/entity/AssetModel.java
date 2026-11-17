package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "mew_asset_model")
public class AssetModel extends BaseEntity {

    @Column(name = "system_id", length = 20, unique = true)
    private String systemId;

    @Column(name = "nomenclature", length = 255, nullable = false)
    private String nomenclature;

    @Column(name = "common_name", length = 255)
    private String commonName = "";

    @Column(name = "ew_category", length = 100)
    private String ewCategory = "";

    @Column(name = "asset_class", length = 100)
    private String assetClass = "";

    @Column(name = "compatible_domains", length = 255)
    private String compatibleDomains = "[]";

    @Column(name = "manufacturer", length = 150)
    private String manufacturer = "";

    @Column(name = "cage_code", length = 10)
    private String cageCode = "";

    @Column(name = "national_stock_number", length = 16, unique = true)
    private String nationalStockNumber;

    @Column(name = "lin", length = 10)
    private String lin;

    @PrePersist
    @PreUpdate
    protected void syncCategoryAndCage() {
        if (ewCategory != null && !ewCategory.isEmpty() && (assetClass == null || assetClass.isEmpty())) {
            this.assetClass = ewCategory;
        } else if (assetClass != null && !assetClass.isEmpty() && (ewCategory == null || ewCategory.isEmpty())) {
            this.ewCategory = assetClass;
        }
    }

    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getNomenclature() { return nomenclature; }
    public void setNomenclature(String nomenclature) { this.nomenclature = nomenclature; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getEwCategory() { return ewCategory; }
    public void setEwCategory(String ewCategory) { this.ewCategory = ewCategory; }
    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
    public String getCompatibleDomains() { return compatibleDomains; }
    public void setCompatibleDomains(String compatibleDomains) { this.compatibleDomains = compatibleDomains; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getCageCode() { return cageCode; }
    public void setCageCode(String cageCode) { this.cageCode = cageCode; }
    public String getNationalStockNumber() { return nationalStockNumber; }
    public void setNationalStockNumber(String nationalStockNumber) { this.nationalStockNumber = nationalStockNumber; }
    public String getLin() { return lin; }
    public void setLin(String lin) { this.lin = lin; }
}
