package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.service.RequisitionService;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentRequest;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/requisitions")
public class RequisitionController {

    private final RequisitionRepository requisitionRepository;
    private final RequisitionService requisitionService;

    public RequisitionController(RequisitionRepository requisitionRepository,
                                 RequisitionService requisitionService) {
        this.requisitionRepository = requisitionRepository;
        this.requisitionService = requisitionService;
    }

    @GetMapping({"", "/"})
    public List<Requisition> listRequisitions() {
        return requisitionRepository.findAll();
    }

    @PostMapping({"/receive-shipment", "/receive-shipment/"})
    public ResponseEntity<ReceiveShipmentResponse> receiveShipment(
            @Valid @RequestBody ReceiveShipmentRequest request) {
        return ResponseEntity.ok(requisitionService.receiveShipment(request));
    }
}
