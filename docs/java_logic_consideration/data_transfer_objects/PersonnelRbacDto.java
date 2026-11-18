package java.data_transfer_objects;

import com.defense.mew.entity.PersonnelRbac;
import java.time.LocalDate;
import java.util.List;

public record PersonnelRbacDto(
        String edipi,
        String serviceBranch,
        String rank,
        String firstName,
        String lastName,
        String milEmail,
        String installationCode,
        String uic,
        String battalion,
        String subUnit,
        String mos,
        String mosTitle,
        String rbacRole,
        String accessScope,
        String clearanceLevel,
        Boolean scifBadgeActive,
        List<String> authorizedSystems,
        String accountStatus,
        LocalDate lastAccessReview) {
    public static PersonnelRbacDto fromEntity(PersonnelRbac entity) {
        return new PersonnelRbacDto(
                entity.getEdipi(),
                entity.getServiceBranch(),
                entity.getRank(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getMilEmail(),
                entity.getInstallationCode(),
                entity.getUnit().getUic(),
                entity.getBattalion(),
                entity.getSubUnit(),
                entity.getMos(),
                entity.getMosTitle(),
                entity.getRbacRole(),
                entity.getAccessScope(),
                entity.getClearanceLevel(),
                entity.getScifBadgeActive(),
                entity.getAuthorizedSystemsList(),
                entity.getAccountStatus(),
                entity.getLastAccessReview());
    }
}
