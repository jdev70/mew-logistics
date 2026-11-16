package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.repository.WorkOrderRepository;
import mil.navy.nswc.mew.service.WorkOrderService;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
public class WorkOrderController {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderRepository workOrderRepository,
                               WorkOrderService workOrderService) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderService = workOrderService;
    }

    @GetMapping({"", "/"})
    public List<WorkOrder> listWorkOrders() {
        return workOrderRepository.findAll();
    }

    @GetMapping({"/{id}", "/{id}/"})
    public WorkOrder getWorkOrder(@PathVariable @NonNull UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
    }

    @PostMapping({"/{id}/close", "/{id}/close/"})
    public ResponseEntity<WorkOrder> closeWorkOrder(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(workOrderService.closeWorkOrder(id));
    }
}
