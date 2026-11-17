package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.domain.enums.WorkOrderLifecycleEventType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/** Immutable, structured audit entry for a work-order state transition. */
@Entity
@Table(name = "mew_work_order_lifecycle_event")
public class WorkOrderLifecycleEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Personnel actor;

    @Column(name = "actor_name", nullable = false, length = 150)
    private String actorName = "System";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private WorkOrderLifecycleEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private MafStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private MafStatus newStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason = "";

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public Personnel getActor() { return actor; }
    public void setActor(Personnel actor) { this.actor = actor; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public WorkOrderLifecycleEventType getEventType() { return eventType; }
    public void setEventType(WorkOrderLifecycleEventType eventType) { this.eventType = eventType; }
    public MafStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(MafStatus previousStatus) { this.previousStatus = previousStatus; }
    public MafStatus getNewStatus() { return newStatus; }
    public void setNewStatus(MafStatus newStatus) { this.newStatus = newStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
