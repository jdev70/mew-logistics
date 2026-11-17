package mil.dod.mew.web.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Technician-provided evidence submitted with a completed maintenance action. */
public class MaintenanceCompletionRequest {
    /** Generated server-side from the structured resolution sections. */
    private String completedWork;

    @NotNull(message = "maintenance_hours is required.")
    @DecimalMin(value = "0.25", message = "maintenance_hours must be at least 0.25.")
    private BigDecimal maintenanceHours;

    @NotBlank(message = "action_taken_code is required.")
    private String actionTakenCode;

    @NotBlank(message = "initial_diagnostics is required.")
    private String initialDiagnostics;

    @NotBlank(message = "safety_tag_out is required.")
    private String safetyTagOut;

    @NotBlank(message = "isolation_inspection is required.")
    private String isolationInspection;

    @NotBlank(message = "fault_identification is required.")
    private String faultIdentification;

    @NotBlank(message = "repair_replacement is required.")
    private String repairReplacement;

    @NotBlank(message = "restoration_testing is required.")
    private String restorationTesting;

    @NotBlank(message = "final_equipment_status is required.")
    private String finalEquipmentStatus;

    public String getCompletedWork() { return completedWork; }
    public void setCompletedWork(String completedWork) { this.completedWork = completedWork; }
    public BigDecimal getMaintenanceHours() { return maintenanceHours; }
    public void setMaintenanceHours(BigDecimal maintenanceHours) { this.maintenanceHours = maintenanceHours; }
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
}
