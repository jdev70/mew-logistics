package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.CommandEchelon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "mew_military_unit")
public class MilitaryUnit extends BaseEntity {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "uic", length = 6, unique = true, nullable = false)
    private String uic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private MilitaryUnit parentUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "echelon_level", length = 30, nullable = false)
    private CommandEchelon echelonLevel = CommandEchelon.COMBATANT_UNIT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installation_id", nullable = false)
    private Installation installation;

    @Column(name = "command_type", length = 100, nullable = false)
    private String commandType = "";

    @Column(name = "personnel_readiness_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal personnelReadinessPct = new BigDecimal("100.00");

    @Column(name = "equipment_readiness_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal equipmentReadinessPct = new BigDecimal("100.00");

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUic() { return uic; }
    public void setUic(String uic) { this.uic = uic; }
    public MilitaryUnit getParentUnit() { return parentUnit; }
    public void setParentUnit(MilitaryUnit parentUnit) { this.parentUnit = parentUnit; }
    public CommandEchelon getEchelonLevel() { return echelonLevel; }
    public void setEchelonLevel(CommandEchelon echelonLevel) { this.echelonLevel = echelonLevel; }
    public Installation getInstallation() { return installation; }
    public void setInstallation(Installation installation) { this.installation = installation; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public BigDecimal getPersonnelReadinessPct() { return personnelReadinessPct; }
    public void setPersonnelReadinessPct(BigDecimal personnelReadinessPct) { this.personnelReadinessPct = personnelReadinessPct; }
    public BigDecimal getEquipmentReadinessPct() { return equipmentReadinessPct; }
    public void setEquipmentReadinessPct(BigDecimal equipmentReadinessPct) { this.equipmentReadinessPct = equipmentReadinessPct; }
}
