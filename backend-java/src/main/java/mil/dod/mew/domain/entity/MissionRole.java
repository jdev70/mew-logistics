package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "mew_mission_role")
public class MissionRole extends BaseEntity {

    @Column(name = "role_id", length = 20, unique = true, nullable = false)
    private String roleId;

    @Column(name = "role_code", length = 30, unique = true, nullable = false)
    private String roleCode;

    @Column(name = "role_name", length = 150, nullable = false)
    private String roleName;

    @Lob
    @Column(name = "description")
    private String description = "";

    @Column(name = "compatible_domains", length = 255)
    private String compatibleDomains = "[]";

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCompatibleDomains() { return compatibleDomains; }
    public void setCompatibleDomains(String compatibleDomains) { this.compatibleDomains = compatibleDomains; }
}
