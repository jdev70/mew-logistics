package mil.dod.mew;

import mil.dod.mew.domain.enums.FunctionalRole;
import mil.dod.mew.service.SyntheticPersonnelDataQualityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"mew.synthetic.scenario=demo", "mew.synthetic.history-days=7"})
@ActiveProfiles("synthetic-data")
class SyntheticPersonnelDataQualityServiceTest {
    @Autowired private SyntheticPersonnelDataQualityService dataQuality;

    @Test
    void fictionalRosterHasValidGradesAssignmentsAndRoleCoverage() {
        SyntheticPersonnelDataQualityService.DataQualityReport report = dataQuality.assess();

        assertTrue(report.isValid(), () -> String.join("; ", report.violations()));
        assertTrue(report.personnelCount() > 0);
        for (FunctionalRole role : FunctionalRole.values()) {
            assertTrue(report.roleCounts().get(role) > 0, () -> "Missing role " + role);
        }
        assertEquals(FunctionalRole.values().length, report.roleCounts().size());
    }
}
