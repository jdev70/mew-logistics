package mil.dod.mew.service;

import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.domain.enums.CommandEchelon;
import mil.dod.mew.domain.enums.FunctionalRole;
import mil.dod.mew.repository.PersonnelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Evaluates only the intentionally fictional synthetic roster for internal consistency. */
@Service
public class SyntheticPersonnelDataQualityService {
    private static final Set<String> SUPPORTED_BRANCHES = Set.of("USN", "USMC", "USAF", "USA");
    private final PersonnelRepository personnel;

    public SyntheticPersonnelDataQualityService(PersonnelRepository personnel) {
        this.personnel = personnel;
    }

    @Transactional(readOnly = true)
    public DataQualityReport assess() {
        List<String> violations = new ArrayList<>();
        Map<FunctionalRole, Integer> roleCounts = new EnumMap<>(FunctionalRole.class);
        for (FunctionalRole role : FunctionalRole.values()) roleCounts.put(role, 0);

        for (Personnel person : personnel.findAll()) {
            String id = person.getPersonnelId() == null ? "<missing personnel_id>" : person.getPersonnelId();
            if (blank(person.getFirstName()) || blank(person.getLastName()) || blank(person.getSpecialtyCode())) {
                violations.add(id + ": fictional identity and specialty fields are required.");
            }
            if (!SUPPORTED_BRANCHES.contains(person.getServiceBranch())) {
                violations.add(id + ": unsupported service branch.");
            }
            if (!validCategoryAndGrade(person)) {
                violations.add(id + ": personnel category and paygrade are inconsistent.");
            }
            if (person.getUnit() == null || person.getInstallation() == null) {
                violations.add(id + ": unit and installation assignments are required.");
            } else {
                if (!Objects.equals(person.getAssignedUic(), person.getUnit().getUic())) {
                    violations.add(id + ": assigned UIC does not match assigned unit.");
                }
                if (person.getUnit().getInstallation() == null || !Objects.equals(person.getInstallation().getId(), person.getUnit().getInstallation().getId())) {
                    violations.add(id + ": assigned installation does not match unit installation.");
                }
                if (!person.getServiceBranch().equals(person.getInstallation().getServiceBranch())) {
                    violations.add(id + ": service branch does not match installation branch.");
                }
                if (person.getFunctionalRole() != null && !validRolePlacement(person)) {
                    violations.add(id + ": functional role is not valid for the assigned unit echelon.");
                }
            }
            if (person.getFunctionalRole() == null) {
                violations.add(id + ": functional role is required.");
            } else {
                roleCounts.compute(person.getFunctionalRole(), (role, count) -> count + 1);
            }
        }
        roleCounts.forEach((role, count) -> {
            if (count == 0) violations.add("Required functional role is not represented: " + role);
        });
        return new DataQualityReport(personnel.count(), Map.copyOf(roleCounts), List.copyOf(violations));
    }

    private boolean validCategoryAndGrade(Personnel person) {
        String category = person.getPersonnelCategory();
        String grade = person.getPaygrade();
        if (blank(category) || blank(grade)) return false;
        return switch (category) {
            case "Active_Duty_Officer" -> grade.matches("O-([1-9]|10)");
            case "Active_Duty_Enlisted" -> grade.matches("E-[1-9]");
            case "Warrant_Officer" -> grade.matches("W-[1-5]");
            case "DoD_Civilian" -> grade.matches("GS-([1-9]|1[0-5])");
            case "Defense_Contractor" -> grade.equals("CONTRACTOR");
            default -> false;
        };
    }

    private boolean validRolePlacement(Personnel person) {
        CommandEchelon echelon = person.getUnit().getEchelonLevel();
        return switch (person.getFunctionalRole()) {
            case SHIPBOARD_TECHNICIAN, UNIT_SUPPO -> echelon == CommandEchelon.COMBATANT_UNIT;
            case SHORE_DEPOT_MANAGER, WARFARE_CENTER_ENGINEER -> echelon != CommandEchelon.COMBATANT_UNIT;
            case FLEET_READINESS_ANALYST -> true;
        };
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record DataQualityReport(long personnelCount, Map<FunctionalRole, Integer> roleCounts,
                                    List<String> violations) {
        public boolean isValid() { return violations.isEmpty(); }
    }
}
