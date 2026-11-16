package mil.navy.nswc.mew.security;
import mil.navy.nswc.mew.security.model.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class MewPermissionsManagerTest {
    private final MewPermissionsManager manager = new MewPermissionsManager();
    private final MewResource norfolk = new MewResource("asset-1", "NAVSTA-NORVA", MilitaryBranch.USN, ClearanceLevel.SECRET, "03538");
    @Test void allowsInstallationTechnicianAtOwnInstallation() { assertTrue(manager.evaluateAccess(new MewUser("tech", MilitaryBranch.USN, CommandTier.INSTALLATION_UNIT, ClearanceLevel.SECRET, "NAVSTA-NORVA", Set.of(), Set.of(), Set.of(Permission.LOG_MAINTENANCE_ACTION)), Permission.LOG_MAINTENANCE_ACTION, norfolk).authorized()); }
    @Test void deniesCrossInstallationAccess() { assertFalse(manager.evaluateAccess(new MewUser("tech", MilitaryBranch.USN, CommandTier.INSTALLATION_UNIT, ClearanceLevel.SECRET, "NSWC-CRANE", Set.of(), Set.of(), Set.of(Permission.LOG_MAINTENANCE_ACTION)), Permission.LOG_MAINTENANCE_ACTION, norfolk).authorized()); }
    @Test void enforcesContractorCageScope() { assertFalse(manager.evaluateAccess(new MewUser("oem", MilitaryBranch.USN, CommandTier.OPERATIONAL_CMD, ClearanceLevel.TS_SCI, "NSWC-CRANE", Set.of(), Set.of("99999"), Set.of(Permission.LOG_MAINTENANCE_ACTION)), Permission.LOG_MAINTENANCE_ACTION, norfolk).authorized()); }
}
