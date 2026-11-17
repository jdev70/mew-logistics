package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.MafStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mew_work_order")
public class WorkOrder extends BaseEntity {

    @Column(name = "jcn", length = 20, unique = true)
    private String jcn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private MafStatus status = MafStatus.OPEN;

    @Column(name = "priority_code", nullable = false)
    private Integer priorityCode = 2;

    @Column(name = "casrep_number", length = 30)
    private String casrepNumber = "";

    @Column(name = "is_grounding_fault", nullable = false)
    private boolean groundingFault = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hull_id")
    private NavalHull hull;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @Column(name = "eic", length = 10)
    private String eic = "D720100";

    @Column(name = "apl_ael", length = 15)
    private String aplAel = "881040291A";

    @Column(name = "when_discovered_code", length = 2)
    private String whenDiscoveredCode = "D";

    @Column(name = "equipment_status_code", length = 2)
    private String equipmentStatusCode = "2";

    @Column(name = "malfunction_code", length = 5)
    private String malfunctionCode = "160";

    @Lob
    @Column(name = "description_of_fault", nullable = false)
    private String descriptionOfFault = "";

    /** Technician narrative recorded before the maintenance action is submitted to QA. */
    @Lob
    @Column(name = "completed_work")
    private String completedWork = "";

    @Column(name = "maintenance_hours", precision = 6, scale = 2)
    private BigDecimal maintenanceHours = BigDecimal.ZERO;

    @Column(name = "maintenance_submitted_at")
    private LocalDateTime maintenanceSubmittedAt;

    /** Navy MAF-style technician resolution fields, retained individually for QA and reporting. */
    @Column(name = "action_taken_code", length = 30)
    private String actionTakenCode = "";

    @Lob @Column(name = "initial_diagnostics")
    private String initialDiagnostics = "";

    @Lob @Column(name = "safety_tag_out")
    private String safetyTagOut = "";

    @Lob @Column(name = "isolation_inspection")
    private String isolationInspection = "";

    @Lob @Column(name = "fault_identification")
    private String faultIdentification = "";

    @Lob @Column(name = "repair_replacement")
    private String repairReplacement = "";

    @Lob @Column(name = "restoration_testing")
    private String restorationTesting = "";

    @Column(name = "final_equipment_status", length = 200)
    private String finalEquipmentStatus = "";

    @Column(name = "work_center", length = 20, nullable = false)
    private String workCenter = "CE02";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Personnel technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qa_inspector_id")
    private Personnel qaInspector;

    public String getJcn() { return jcn; }
    public void setJcn(String jcn) { this.jcn = jcn; }
    public MafStatus getStatus() { return status; }
    public void setStatus(MafStatus status) { this.status = status; }
    public Integer getPriorityCode() { return priorityCode; }
    public void setPriorityCode(Integer priorityCode) { this.priorityCode = priorityCode; }
    public String getCasrepNumber() { return casrepNumber; }
    public void setCasrepNumber(String casrepNumber) { this.casrepNumber = casrepNumber; }
    public boolean isGroundingFault() { return groundingFault; }
    public void setGroundingFault(boolean groundingFault) { this.groundingFault = groundingFault; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public NavalHull getHull() { return hull; }
    public void setHull(NavalHull hull) { this.hull = hull; }
    public Installation getInstallation() { return installation; }
    public void setInstallation(Installation installation) { this.installation = installation; }
    public String getEic() { return eic; }
    public void setEic(String eic) { this.eic = eic; }
    public String getAplAel() { return aplAel; }
    public void setAplAel(String aplAel) { this.aplAel = aplAel; }
    public String getWhenDiscoveredCode() { return whenDiscoveredCode; }
    public void setWhenDiscoveredCode(String whenDiscoveredCode) { this.whenDiscoveredCode = whenDiscoveredCode; }
    public String getEquipmentStatusCode() { return equipmentStatusCode; }
    public void setEquipmentStatusCode(String equipmentStatusCode) { this.equipmentStatusCode = equipmentStatusCode; }
    public String getMalfunctionCode() { return malfunctionCode; }
    public void setMalfunctionCode(String malfunctionCode) { this.malfunctionCode = malfunctionCode; }
    public String getDescriptionOfFault() { return descriptionOfFault; }
    public void setDescriptionOfFault(String descriptionOfFault) { this.descriptionOfFault = descriptionOfFault; }
    public String getCompletedWork() { return completedWork; }
    public void setCompletedWork(String completedWork) { this.completedWork = completedWork; }
    public BigDecimal getMaintenanceHours() { return maintenanceHours; }
    public void setMaintenanceHours(BigDecimal maintenanceHours) { this.maintenanceHours = maintenanceHours; }
    public LocalDateTime getMaintenanceSubmittedAt() { return maintenanceSubmittedAt; }
    public void setMaintenanceSubmittedAt(LocalDateTime maintenanceSubmittedAt) { this.maintenanceSubmittedAt = maintenanceSubmittedAt; }
    public String getActionTakenCode() { return actionTakenCode; }
    public void setActionTakenCode(String actionTakenCode) { this.actionTakenCode = actionTakenCode; }
    public String getInitialDiagnostics() { return initialDiagnostics; }
    public void setInitialDiagnostics(String initialDiagnostics) { this.initialDiagnostics = initialDiagnostics; }
    public String getSafetyTagOut() { return safetyTagOut; }
    public void setSafetyTagOut(String safetyTagOut) { this.safetyTagOut = safetyTagOut; }
    public String getIsolationInspection() { return isolationInspection; }
    public void setIsolationInspection(String isolationInspection) { this.isolationInspection = isolationInspection; }
    public String getFaultIdentification() { return faultIdentification; }
    public void setFaultIdentification(String faultIdentification) { this.faultIdentification = faultIdentification; }
    public String getRepairReplacement() { return repairReplacement; }
    public void setRepairReplacement(String repairReplacement) { this.repairReplacement = repairReplacement; }
    public String getRestorationTesting() { return restorationTesting; }
    public void setRestorationTesting(String restorationTesting) { this.restorationTesting = restorationTesting; }
    public String getFinalEquipmentStatus() { return finalEquipmentStatus; }
    public void setFinalEquipmentStatus(String finalEquipmentStatus) { this.finalEquipmentStatus = finalEquipmentStatus; }
    public String getWorkCenter() { return workCenter; }
    public void setWorkCenter(String workCenter) { this.workCenter = workCenter; }
    public Personnel getTechnician() { return technician; }
    public void setTechnician(Personnel technician) { this.technician = technician; }
    public Personnel getQaInspector() { return qaInspector; }
    public void setQaInspector(Personnel qaInspector) { this.qaInspector = qaInspector; }
}
