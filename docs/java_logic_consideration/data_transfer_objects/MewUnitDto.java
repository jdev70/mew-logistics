package java.data_transfer_objects;

import com.defense.mew.entity.MewUnit;

public record MewUnitDto(
        String uic,
        String serviceBranch,
        String installationCode,
        String installationName,
        String city,
        String state,
        String command,
        String battalion,
        String subUnit,
        String area,
        Boolean scifAccess) {
    public static MewUnitDto fromEntity(MewUnit entity) {
        return new MewUnitDto(
                entity.getUic(),
                entity.getServiceBranch(),
                entity.getInstallationCode(),
                entity.getInstallationName(),
                entity.getCity(),
                entity.getState(),
                entity.getCommand(),
                entity.getBattalion(),
                entity.getSubUnit(),
                entity.getArea(),
                entity.getScifAccess());
    }
}
