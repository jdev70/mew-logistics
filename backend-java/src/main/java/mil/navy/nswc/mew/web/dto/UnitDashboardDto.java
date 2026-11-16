package mil.navy.nswc.mew.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UnitDashboardDto {
    private UUID id;
    private String name;
    private String uic;
    private String commandType;
    private String installationName;
    private BigDecimal personnelReadinessPct;
    private BigDecimal equipmentReadinessPct;
    private Double fleetAvgMtbf;
    private Double fleetAvgMttr;
    private Double fleetAvgAo;
    private Long totalAssets;
    private Long fmcAssets;
    private Long degradedAssets;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUic() { return uic; }
    public void setUic(String uic) { this.uic = uic; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public String getInstallationName() { return installationName; }
    public void setInstallationName(String installationName) { this.installationName = installationName; }
    public BigDecimal getPersonnelReadinessPct() { return personnelReadinessPct; }
    public void setPersonnelReadinessPct(BigDecimal personnelReadinessPct) { this.personnelReadinessPct = personnelReadinessPct; }
    public BigDecimal getEquipmentReadinessPct() { return equipmentReadinessPct; }
    public void setEquipmentReadinessPct(BigDecimal equipmentReadinessPct) { this.equipmentReadinessPct = equipmentReadinessPct; }
    public Double getFleetAvgMtbf() { return fleetAvgMtbf; }
    public void setFleetAvgMtbf(Double fleetAvgMtbf) { this.fleetAvgMtbf = fleetAvgMtbf; }
    public Double getFleetAvgMttr() { return fleetAvgMttr; }
    public void setFleetAvgMttr(Double fleetAvgMttr) { this.fleetAvgMttr = fleetAvgMttr; }
    public Double getFleetAvgAo() { return fleetAvgAo; }
    public void setFleetAvgAo(Double fleetAvgAo) { this.fleetAvgAo = fleetAvgAo; }
    public Long getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Long totalAssets) { this.totalAssets = totalAssets; }
    public Long getFmcAssets() { return fmcAssets; }
    public void setFmcAssets(Long fmcAssets) { this.fmcAssets = fmcAssets; }
    public Long getDegradedAssets() { return degradedAssets; }
    public void setDegradedAssets(Long degradedAssets) { this.degradedAssets = degradedAssets; }
}
