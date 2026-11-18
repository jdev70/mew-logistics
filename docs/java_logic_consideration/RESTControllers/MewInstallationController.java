package com.defense.mew.controller;

import com.defense.mew.dto.MewUnitDto;
import com.defense.mew.dto.PersonnelRbacDto;
import com.defense.mew.entity.PersonnelRbac;
import com.defense.mew.repository.MewUnitRepository;
import com.defense.mew.repository.PersonnelRbacRepository;
import com.defense.mew.security.RbacContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mew")
@RequiredArgsConstructor
public class MewInstallationController {

    private final MewUnitRepository unitRepository;
    private final PersonnelRbacRepository personnelRepository;

    @GetMapping("/units")
    public ResponseEntity<List<MewUnitDto>> getUnits(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String installationCode) {
        if (installationCode != null && !installationCode.isBlank()) {
            return ResponseEntity.ok(
                    unitRepository.findByInstallationCode(installationCode).stream().map(MewUnitDto::fromEntity)
                            .toList());
        }
        if (branch != null && !branch.isBlank()) {
            return ResponseEntity.ok(
                    unitRepository.findByServiceBranch(branch.toUpperCase()).stream().map(MewUnitDto::fromEntity)
                            .toList());
        }
        return ResponseEntity.ok(
                unitRepository.findAll().stream().map(MewUnitDto::fromEntity).toList());
    }

    /**
     * Returns the authenticated user's own RBAC profile, clearance, and authorized
     * systems.
     */
    @GetMapping("/rbac/me")
    public ResponseEntity<PersonnelRbacDto> getCurrentUserProfile(HttpServletRequest request) {
        PersonnelRbac currentUser = (PersonnelRbac) request.getAttribute(RbacContextFilter.CURRENT_USER_ATTR);
        return ResponseEntity.ok(PersonnelRbacDto.fromEntity(currentUser));
    }

    /**
     * Lists personnel assigned to a specific UIC (restricted to Command Approvers &
     * SCIF SysAdmins).
     */
    @GetMapping("/rbac/units/{uic}/personnel")
    public ResponseEntity<List<PersonnelRbacDto>> getUnitPersonnel(
            @PathVariable String uic,
            HttpServletRequest request) {
        PersonnelRbac currentUser = (PersonnelRbac) request.getAttribute(RbacContextFilter.CURRENT_USER_ATTR);

        if (!"COMMAND_APPROVER".equals(currentUser.getRbacRole())
                && !"SCIF_SYSADMIN".equals(currentUser.getRbacRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only COMMAND_APPROVER or SCIF_SYSADMIN roles may query unit RBAC rosters.");
        }

        List<PersonnelRbacDto> roster = personnelRepository.findByUnitUic(uic).stream()
                .map(PersonnelRbacDto::fromEntity)
                .toList();
        return ResponseEntity.ok(roster);
    }
}
