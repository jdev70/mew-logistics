package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.InventoryTransaction;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.InventoryTransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    public List<InventoryBalance> listBalances() {
        return balanceRepository.findAll();
    }

    @GetMapping({"/inventory-transactions", "/inventory-transactions/"})
    public List<InventoryTransaction> listTransactions(
            @RequestParam(value = "installation_id", required = false) UUID installationId,
            @RequestParam(value = "nsn", required = false) String nsn,
            @RequestParam(value = "type", required = false) TransactionType type) {
        if (installationId != null) {
            return transactionRepository.findByBalanceInstallationIdOrderByCreatedAtDesc(installationId);
        }
        if (nsn != null && !nsn.isBlank()) {
            return transactionRepository.findByBalanceMaterialNsnOrderByCreatedAtDesc(nsn);
        }
        if (type != null) {
            return transactionRepository.findByTransactionTypeOrderByCreatedAtDesc(type);
        }
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }
}
