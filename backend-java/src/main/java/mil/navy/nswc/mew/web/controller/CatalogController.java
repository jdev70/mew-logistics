package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.HullSystemAssignment;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import mil.navy.nswc.mew.domain.entity.NavalHull;
import mil.navy.nswc.mew.domain.entity.Personnel;
import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.repository.HullSystemAssignmentRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import mil.navy.nswc.mew.repository.MaterialCatalogRepository;
import mil.navy.nswc.mew.repository.NavalHullRepository;
import mil.navy.nswc.mew.repository.PersonnelRepository;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final InstallationRepository installationRepository;
    private final PersonnelRepository personnelRepository;
    private final NavalHullRepository navalHullRepository;
    private final HullSystemAssignmentRepository loadoutRepository;
    private final MaterialCatalogRepository materialRepository;
    private final SecurityAuditLogRepository auditLogRepository;

    public CatalogController(InstallationRepository installationRepository,
                             PersonnelRepository personnelRepository,
                             NavalHullRepository navalHullRepository,
                             HullSystemAssignmentRepository loadoutRepository,
                             MaterialCatalogRepository materialRepository,
                             SecurityAuditLogRepository auditLogRepository) {
        this.installationRepository = installationRepository;
        this.personnelRepository = personnelRepository;
        this.navalHullRepository = navalHullRepository;
        this.loadoutRepository = loadoutRepository;
        this.materialRepository = materialRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping({"/installations", "/installations/"})
    public List<Installation> listInstallations() {
        return installationRepository.findAll();
    }

    @GetMapping({"/personnel", "/personnel/"})
    public List<Personnel> listPersonnel() {
        return personnelRepository.findAll();
    }

    @GetMapping({"/hulls", "/hulls/"})
    public List<NavalHull> listHulls() {
        return navalHullRepository.findAll();
    }

    @GetMapping({"/loadouts", "/loadouts/"})
    public List<HullSystemAssignment> listLoadouts() {
        return loadoutRepository.findAll();
    }

    @GetMapping({"/components", "/components/"})
    public List<MaterialCatalog> listComponents() {
        return materialRepository.findAll();
    }

    @GetMapping({"/security-audit-logs", "/security-audit-logs/"})
    public List<SecurityAuditLog> listAuditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
