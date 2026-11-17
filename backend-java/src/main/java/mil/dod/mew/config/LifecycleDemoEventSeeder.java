package mil.dod.mew.config;

import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.entity.WorkOrderLifecycleEvent;
import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.domain.enums.WorkOrderLifecycleEventType;
import mil.dod.mew.repository.PersonnelRepository;
import mil.dod.mew.repository.WorkOrderLifecycleEventRepository;
import mil.dod.mew.repository.WorkOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Adds fictional, deterministic lifecycle history to the local interview/demo data. */
@Component
@Order(2)
@Profile({"default", "local", "oracle-local"})
public class LifecycleDemoEventSeeder implements CommandLineRunner {
    private final WorkOrderRepository workOrders;
    private final PersonnelRepository personnel;
    private final WorkOrderLifecycleEventRepository events;

    public LifecycleDemoEventSeeder(WorkOrderRepository workOrders, PersonnelRepository personnel,
                                    WorkOrderLifecycleEventRepository events) {
        this.workOrders = workOrders; this.personnel = personnel; this.events = events;
    }

    @Override @Transactional
    public void run(String... ignored) {
        if (events.count() > 0) return;
        Personnel tech = personnel.findByPersonnelId("MEW-PER-0002").orElse(null);
        Personnel reviewer = personnel.findByPersonnelId("MEW-PER-0000").orElse(null);
        workOrders.findByJcn("N21944-CE02-0003").ifPresent(workOrder -> event(workOrder, tech,
                WorkOrderLifecycleEventType.TECHNICIAN_SUBMITTED_FOR_QA, MafStatus.IN_WORK, MafStatus.QA_PENDING,
                30, "Technician submitted fictional completion evidence for independent QA review."));
        workOrders.findByJcn("N21944-CE02-0004").ifPresent(workOrder -> {
            event(workOrder, tech, WorkOrderLifecycleEventType.TECHNICIAN_SUBMITTED_FOR_QA, MafStatus.IN_WORK, MafStatus.QA_PENDING, 58, "Technician submitted fictional repair evidence for QA review.");
            event(workOrder, reviewer, WorkOrderLifecycleEventType.REWORK_REQUESTED, MafStatus.QA_PENDING, MafStatus.IN_WORK, 52, "QA requested rework: repeat restoration test with the antenna-control voltage under load.");
        });
        workOrders.findByJcn("N21944-CE02-0005").ifPresent(workOrder -> {
            event(workOrder, tech, WorkOrderLifecycleEventType.TECHNICIAN_SUBMITTED_FOR_QA, MafStatus.IN_WORK, MafStatus.QA_PENDING, 96, "Technician submitted completed maintenance evidence.");
            event(workOrder, reviewer, WorkOrderLifecycleEventType.QA_APPROVED_FOR_RETURN_TO_SERVICE, MafStatus.QA_PENDING, MafStatus.RETURNED_TO_SERVICE, 84, "QA accepted fictional restoration-test evidence.");
            event(workOrder, reviewer, WorkOrderLifecycleEventType.RETURNED_TO_SERVICE, MafStatus.RETURNED_TO_SERVICE, MafStatus.RETURNED_TO_SERVICE, 82, "Equipment returned to service after QA approval.");
            event(workOrder, reviewer, WorkOrderLifecycleEventType.WORK_ORDER_CLOSED, MafStatus.RETURNED_TO_SERVICE, MafStatus.CLOSED, 76, "Administrative closure recorded after return to service.");
        });
    }

    private void event(WorkOrder workOrder, Personnel actor, WorkOrderLifecycleEventType type,
                       MafStatus previous, MafStatus next, long hoursAgo, String reason) {
        WorkOrderLifecycleEvent event = new WorkOrderLifecycleEvent();
        event.setWorkOrder(workOrder); event.setActor(actor);
        event.setActorName(actor == null ? "Synthetic demo actor" : (actor.getRank() + " " + actor.getFirstName() + " " + actor.getLastName()).trim());
        event.setEventType(type); event.setPreviousStatus(previous); event.setNewStatus(next);
        event.setOccurredAt(Instant.now().minus(hoursAgo, ChronoUnit.HOURS)); event.setReason(reason);
        events.save(event);
    }
}
