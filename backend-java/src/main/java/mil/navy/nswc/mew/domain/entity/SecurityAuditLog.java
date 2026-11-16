package mil.navy.nswc.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mew_security_audit_log")
public class SecurityAuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Personnel actor;

    @Column(name = "actor_name", length = 150)
    private String actorName = "System / Anonymous";

    @Column(name = "actor_role", length = 50)
    private String actorRole = "";

    @Column(name = "actor_uic", length = 20)
    private String actorUic = "";

    @Column(name = "actor_clearance", length = 20)
    private String actorClearance = "";

    @Column(name = "actor_cage_scope", length = 50)
    private String actorCageScope = "";

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "decision", length = 20, nullable = false)
    private String decision = "ALLOW";

    @Column(name = "resource_target", length = 150)
    private String resourceTarget = "";

    @Lob
    @Column(name = "reason")
    private String reason = "";

    public Personnel getActor() { return actor; }
    public void setActor(Personnel actor) { this.actor = actor; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getActorUic() { return actorUic; }
    public void setActorUic(String actorUic) { this.actorUic = actorUic; }
    public String getActorClearance() { return actorClearance; }
    public void setActorClearance(String actorClearance) { this.actorClearance = actorClearance; }
    public String getActorCageScope() { return actorCageScope; }
    public void setActorCageScope(String actorCageScope) { this.actorCageScope = actorCageScope; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getResourceTarget() { return resourceTarget; }
    public void setResourceTarget(String resourceTarget) { this.resourceTarget = resourceTarget; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
