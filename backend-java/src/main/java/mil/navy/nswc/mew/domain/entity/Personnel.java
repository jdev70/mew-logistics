package mil.navy.nswc.mew.domain.entity;

import mil.navy.nswc.mew.domain.enums.FunctionalRole;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mew_personnel")
public class Personnel extends BaseEntity {

    @Column(name = "personnel_id", length = 20, unique = true)
    private String personnelId;

    /** Immutable subject issued by the configured identity provider. */
    @Column(name = "identity_subject", length = 150, unique = true)
    private String identitySubject;

    @Column(name = "dod_id", length = 10, unique = true)
    private String dodId;

    @Column(name = "first_name", length = 100)
    private String firstName = "";

    @Column(name = "middle_initial", length = 1)
    private String middleInitial = "";

    @Column(name = "last_name", length = 100)
    private String lastName = "";

    @Column(name = "personnel_category", length = 50, nullable = false)
    private String personnelCategory = "Active_Duty_Enlisted";

    @Column(name = "paygrade", length = 20)
    private String paygrade = "";

    @Column(name = "rank", length = 30, nullable = false)
    private String rank = "";

    @Column(name = "specialty_code", length = 100)
    private String specialtyCode = "";

    @Column(name = "title", length = 150)
    private String title = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "functional_role", length = 40, nullable = false)
    private FunctionalRole functionalRole = FunctionalRole.SHIPBOARD_TECHNICIAN;

    @Column(name = "assigned_uic", length = 10)
    private String assignedUic = "";

    @Column(name = "authorized_cage_codes", length = 500)
    private String authorizedCageCodes = "[]";

    @Column(name = "authorized_installations", length = 500)
    private String authorizedInstallations = "[]";

    @Column(name = "service_branch", length = 10, nullable = false)
    private String serviceBranch = "USN";

    @Column(name = "command_tier", length = 30, nullable = false)
    private String commandTier = "INSTALLATION_UNIT";

    /** Comma-delimited Permission enum values. ALL is permitted for local seed data only. */
    @Column(name = "authorization_permissions", length = 2000, nullable = false)
    private String authorizationPermissions = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @Column(name = "security_clearance", length = 50, nullable = false)
    private String securityClearance = "Secret";

    @Column(name = "operational_status", length = 50, nullable = false)
    private String operationalStatus = "In_Garrison";

    public String getPersonnelId() { return personnelId; }
    public void setPersonnelId(String personnelId) { this.personnelId = personnelId; }
    public String getIdentitySubject() { return identitySubject; }
    public void setIdentitySubject(String identitySubject) { this.identitySubject = identitySubject; }
    public String getDodId() { return dodId; }
    public void setDodId(String dodId) { this.dodId = dodId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleInitial() { return middleInitial; }
    public void setMiddleInitial(String middleInitial) { this.middleInitial = middleInitial; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPersonnelCategory() { return personnelCategory; }
    public void setPersonnelCategory(String personnelCategory) { this.personnelCategory = personnelCategory; }
    public String getPaygrade() { return paygrade; }
    public void setPaygrade(String paygrade) { this.paygrade = paygrade; }
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
    public String getSpecialtyCode() { return specialtyCode; }
    public void setSpecialtyCode(String specialtyCode) { this.specialtyCode = specialtyCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public FunctionalRole getFunctionalRole() { return functionalRole; }
    public void setFunctionalRole(FunctionalRole functionalRole) { this.functionalRole = functionalRole; }
    public String getAssignedUic() { return assignedUic; }
    public void setAssignedUic(String assignedUic) { this.assignedUic = assignedUic; }
    public String getAuthorizedCageCodes() { return authorizedCageCodes; }
    public void setAuthorizedCageCodes(String authorizedCageCodes) { this.authorizedCageCodes = authorizedCageCodes; }
    public String getAuthorizedInstallations() { return authorizedInstallations; }
    public void setAuthorizedInstallations(String authorizedInstallations) { this.authorizedInstallations = authorizedInstallations; }
    public String getServiceBranch() { return serviceBranch; }
    public void setServiceBranch(String serviceBranch) { this.serviceBranch = serviceBranch; }
    public String getCommandTier() { return commandTier; }
    public void setCommandTier(String commandTier) { this.commandTier = commandTier; }
    public String getAuthorizationPermissions() { return authorizationPermissions; }
    public void setAuthorizationPermissions(String authorizationPermissions) { this.authorizationPermissions = authorizationPermissions; }
    public MilitaryUnit getUnit() { return unit; }
    public void setUnit(MilitaryUnit unit) { this.unit = unit; }
    public Installation getInstallation() { return installation; }
    public void setInstallation(Installation installation) { this.installation = installation; }
    public String getSecurityClearance() { return securityClearance; }
    public void setSecurityClearance(String securityClearance) { this.securityClearance = securityClearance; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
}
