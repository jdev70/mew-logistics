package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.InventoryTransaction;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.InventoryTransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {

    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;

    public InventoryController(InventoryBalanceRepository balanceRepository,
                               InventoryTransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping({"/inventory-balances", "/inventory-balances/"})
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBalances() {
        return balanceRepository.findAll().stream().map(this::toBalanceDto).collect(Collectors.toList());
    }

    @GetMapping({"/inventory-transactions", "/inventory-transactions/"})
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTransactions(
            @RequestParam(value = "installation_id", required = false) UUID installationId,
            @RequestParam(value = "nsn", required = false) String nsn,
            @RequestParam(value = "type", required = false) TransactionType type) {
        List<InventoryTransaction> list;
        if (installationId != null) {
            list = transactionRepository.findByBalanceInstallationIdOrderByCreatedAtDesc(installationId);
        } else if (nsn != null && !nsn.isBlank()) {
            list = transactionRepository.findByBalanceMaterialNsnOrderByCreatedAtDesc(nsn);
        } else if (type != null) {
            list = transactionRepository.findByTransactionTypeOrderByCreatedAtDesc(type);
        } else {
            list = transactionRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::toTxDto).collect(Collectors.toList());
    }

    private Map<String, Object> toBalanceDto(InventoryBalance b) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", b.getId());
        dto.put("installation", b.getInstallation().getId());
        dto.put("installation_code", b.getInstallation().getInstallationCode());
        dto.put("installation_name", b.getInstallation().getName());
        dto.put("material", b.getMaterial().getId());
        dto.put("component_id", b.getMaterial().getComponentId());
        dto.put("nsn", b.getMaterial().getNsn());
        dto.put("nomenclature", b.getMaterial().getNomenclature());
        dto.put("component_category", b.getMaterial().getComponentCategory());
        dto.put("unit_price", b.getMaterial().getUnitPrice());
        dto.put("critical_item", b.getMaterial().isCriticalItem());
        dto.put("quantity_on_hand", b.getQuantityOnHand());
        dto.put("reorder_point", b.getReorderPoint());
        dto.put("safety_stock", b.getSafetyStock());
        dto.put("needs_replenishment", b.needsReplenishment());
        return dto;
    }

    private Map<String, Object> toTxDto(InventoryTransaction tx) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", tx.getId());
        dto.put("created_at", tx.getCreatedAt());
        dto.put("installation_name", tx.getBalance().getInstallation().getName());
        dto.put("nsn", tx.getBalance().getMaterial().getNsn());
        dto.put("nomenclature", tx.getBalance().getMaterial().getNomenclature());
        dto.put("transaction_type", tx.getTransactionType().name());
        dto.put("transaction_type_display", tx.getTransactionType().getLabel());
        dto.put("quantity", tx.getQuantity());
        dto.put("previous_balance", tx.getPreviousBalance());
        dto.put("new_balance", tx.getNewBalance());
        dto.put("reference_document", tx.getReferenceDocument());
        dto.put("performed_by_name", tx.getPerformedBy() != null
                ? (tx.getPerformedBy().getRank() + " " + tx.getPerformedBy().getLastName()).trim()
                : "System / Warehouse");
        return dto;
    }
}
