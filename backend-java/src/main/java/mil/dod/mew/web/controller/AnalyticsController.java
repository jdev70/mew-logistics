package mil.dod.mew.web.controller;

import mil.dod.mew.domain.entity.Asset;
import mil.dod.mew.domain.entity.Installation;
import mil.dod.mew.repository.AssetRepository;
import mil.dod.mew.repository.InstallationRepository;
import mil.dod.mew.repository.WorkOrderRepository;
import mil.dod.mew.repository.WorkOrderLifecycleEventRepository;
import mil.dod.mew.repository.RequisitionRepository;
import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.domain.enums.WorkOrderLifecycleEventType;
import mil.dod.mew.security.MewResourceFactory;
import mil.dod.mew.security.model.Permission;
import mil.dod.mew.service.RbacPolicyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics/overview")
public class AnalyticsController {

    private final AssetRepository assetRepository;
    private final InstallationRepository installationRepository;
    private final RbacPolicyService rbacPolicyService;
    private final WorkOrderRepository workOrders;
    private final WorkOrderLifecycleEventRepository lifecycleEvents;
    private final RequisitionRepository requisitions;

    public AnalyticsController(AssetRepository assetRepository,
                               InstallationRepository installationRepository,
                               RbacPolicyService rbacPolicyService, WorkOrderRepository workOrders,
                               WorkOrderLifecycleEventRepository lifecycleEvents, RequisitionRepository requisitions) {
        this.assetRepository = assetRepository;
        this.installationRepository = installationRepository;
        this.rbacPolicyService = rbacPolicyService;
        this.workOrders = workOrders;
        this.lifecycleEvents = lifecycleEvents;
        this.requisitions = requisitions;
    }

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsOverview() {
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(asset -> rbacPolicyService.canAccess(Permission.VIEW_GLOBAL_READINESS,
                        MewResourceFactory.forAsset(asset)))
                .collect(Collectors.toList());
        List<Map<String, Object>> aoBreakdown = assets.stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("system_id", a.getAssetModel() != null ? a.getAssetModel().getSystemId() : "SYS-101");
            item.put("nomenclature", a.getAssetModel() != null ? a.getAssetModel().getNomenclature() : "MEW System");
            item.put("common_name", a.getAssetModel() != null ? a.getAssetModel().getCommonName() : "");
            String domain = a.getDomain() != null && !a.getDomain().isBlank()
                    ? a.getDomain()
                    : (a.getHull() != null && a.getHull().getDomain() != null
                            ? a.getHull().getDomain()
                            : "Surface");
            item.put("domain", domain);
            item.put("total_count", 1);
            item.put("fmc_count", "FMC".equals(a.getReadinessStatus().name()) ? 1 : 0);
            item.put("avg_ao", a.getOperationalAvailabilityPct());
            item.put("avg_mtbf", a.getMtbfHours());
            item.put("avg_mttr", a.getMttrHours());
            return item;
        }).collect(Collectors.toList());

        List<Installation> installations = installationRepository.findAll().stream()
                .filter(installation -> rbacPolicyService.canAccess(Permission.VIEW_GLOBAL_READINESS,
                        MewResourceFactory.forInstallation(installation)))
                .collect(Collectors.toList());
        List<Map<String, Object>> nodes = installations.stream().map(inst -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("code", inst.getInstallationCode());
            node.put("name", inst.getName());
            node.put("lat", inst.getLatitude() != null ? inst.getLatitude() : 36.85);
            node.put("lon", inst.getLongitude() != null ? inst.getLongitude() : -76.29);
            node.put("category", inst.getMissionCategory());
            return node;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ao_system_breakdown", aoBreakdown);
        payload.put("component_analytics", Collections.emptyList());
        payload.put("supply_bottleneck_funnel", Collections.emptyList());
        payload.put("nec_staffing_matrix", Collections.emptyList());
        payload.put("geospatial_nodes", nodes);
        payload.put("proximity_depot_routes", Collections.emptyList());
        List<mil.dod.mew.domain.entity.WorkOrderLifecycleEvent> events = lifecycleEvents.findAll();
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("qa_pending", workOrders.findAll().stream().filter(w -> w.getStatus() == MafStatus.QA_PENDING).count());
        lifecycle.put("rework_events", events.stream().filter(e -> e.getEventType() == WorkOrderLifecycleEventType.REWORK_REQUESTED).count());
        lifecycle.put("returned_to_service", workOrders.findAll().stream().filter(w -> w.getStatus() == MafStatus.RETURNED_TO_SERVICE).count());
        lifecycle.put("closed", workOrders.findAll().stream().filter(w -> w.getStatus() == MafStatus.CLOSED).count());
        lifecycle.put("transition_events", events.size());
        Map<UUID, List<mil.dod.mew.domain.entity.WorkOrderLifecycleEvent>> eventsByWorkOrder = events.stream()
                .collect(Collectors.groupingBy(event -> event.getWorkOrder().getId()));
        List<Double> qaTurnaround = eventsByWorkOrder.values().stream().map(history -> durationHours(history,
                WorkOrderLifecycleEventType.TECHNICIAN_SUBMITTED_FOR_QA,
                WorkOrderLifecycleEventType.QA_APPROVED_FOR_RETURN_TO_SERVICE)).filter(hours -> hours != null).collect(Collectors.toList());
        List<Double> closureTurnaround = eventsByWorkOrder.values().stream().map(history -> durationHours(history,
                WorkOrderLifecycleEventType.RETURNED_TO_SERVICE, WorkOrderLifecycleEventType.WORK_ORDER_CLOSED)).filter(hours -> hours != null).collect(Collectors.toList());
        lifecycle.put("avg_qa_turnaround_hours", qaTurnaround.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        lifecycle.put("avg_closure_turnaround_hours", closureTurnaround.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        lifecycle.put("avg_qa_aging_hours", workOrders.findAll().stream().filter(w -> w.getStatus() == MafStatus.QA_PENDING)
                .map(w -> eventsByWorkOrder.getOrDefault(w.getId(), Collections.emptyList())).mapToDouble(history -> {
                    Instant submitted = history.stream().filter(e -> e.getEventType() == WorkOrderLifecycleEventType.TECHNICIAN_SUBMITTED_FOR_QA)
                            .map(e -> e.getOccurredAt()).max(Instant::compareTo).orElse(null);
                    return submitted == null ? 0 : Duration.between(submitted, Instant.now()).toMinutes() / 60.0;
                }).average().orElse(0));
        long qaDecisions = events.stream().filter(e -> e.getEventType() == WorkOrderLifecycleEventType.REWORK_REQUESTED
                || e.getEventType() == WorkOrderLifecycleEventType.QA_APPROVED_FOR_RETURN_TO_SERVICE).count();
        long reworkEvents = ((Number) lifecycle.get("rework_events")).longValue();
        lifecycle.put("rework_rate_pct", qaDecisions == 0 ? 0 : (reworkEvents * 100.0) / qaDecisions);
        payload.put("lifecycle_metrics", lifecycle);
        Map<String, Object> supply = new LinkedHashMap<>();
        List<mil.dod.mew.domain.entity.Requisition> demand = requisitions.findAll();
        long received = demand.stream().filter(r -> "Received".equalsIgnoreCase(r.getStatus())).count();
        long open = demand.size() - received;
        supply.put("open_requisitions", open);
        supply.put("fill_rate_pct", demand.isEmpty() ? 0 : received * 100.0 / demand.size());
        supply.put("priority_one_open", demand.stream().filter(r -> r.getPriority() != null && r.getPriority() == 1 && !"Received".equalsIgnoreCase(r.getStatus())).count());
        supply.put("avg_estimated_lead_days", demand.stream().filter(r -> r.getEstimatedDeliveryDate() != null).mapToLong(r -> java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), r.getEstimatedDeliveryDate())).average().orElse(0));
        payload.put("supply_metrics", supply);
        return payload;
    }

    private Double durationHours(List<mil.dod.mew.domain.entity.WorkOrderLifecycleEvent> history,
                                 WorkOrderLifecycleEventType start, WorkOrderLifecycleEventType end) {
        Instant started = history.stream().filter(event -> event.getEventType() == start).map(event -> event.getOccurredAt()).min(Instant::compareTo).orElse(null);
        Instant ended = history.stream().filter(event -> event.getEventType() == end).map(event -> event.getOccurredAt()).max(Instant::compareTo).orElse(null);
        return started == null || ended == null ? null : Duration.between(started, ended).toMinutes() / 60.0;
    }
}
