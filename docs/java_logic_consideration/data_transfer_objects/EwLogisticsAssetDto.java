package java.data_transfer_objects;

import com.defense.mew.entity.EwLogisticsAsset;
import java.time.LocalDate;

public record EwLogisticsAssetDto(
        String assetId,
        String serialNumber,
        String tamcn,
        String nsn,
        String nomenclature,
        String category,
        String classificationMarking,
        String serviceBranch,
        String installationCode,
        String installationName,
        String uic,
        String command,
        String battalion,
        String subUnit,
        String storageLocation,
        String readinessStatus,
        String gcssEroWorkOrder,
        Boolean cryptoKeyLoaded,
        String custodianEdipi,
        String custodianName,
        String approvingOfficerEdipi,
        LocalDate lastPmcsDate) {
    public static EwLogisticsAssetDto fromEntity(EwLogisticsAsset entity) {
        return new EwLogisticsAssetDto(
                entity.getAssetId(),
                entity.getSerialNumber(),
                entity.getTamcn(),
                entity.getNsn(),
                entity.getNomenclature(),
                entity.getCategory(),
                entity.getClassificationMarking(),
                entity.getServiceBranch(),
                entity.getInstallationCode(),
                entity.getInstallationName(),
                entity.getUnit().getUic(),
                entity.getCommand(),
                entity.getBattalion(),
                entity.getSubUnit(),
                entity.getStorageLocation(),
                entity.getReadinessStatus(),
                entity.getGcssEroWorkOrder(),
                entity.getCryptoKeyLoaded(),
                entity.getCustodian().getEdipi(),
                entity.getCustodianName(),
                entity.getApprovingOfficer().getEdipi(),
                entity.getLastPmcsDate());
    }
}
