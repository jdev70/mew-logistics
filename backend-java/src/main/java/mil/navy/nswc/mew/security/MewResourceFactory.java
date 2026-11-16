package mil.navy.nswc.mew.security;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.domain.entity.NavalHull;
import mil.navy.nswc.mew.domain.entity.HullSystemAssignment;
import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.security.model.ClearanceLevel;
import mil.navy.nswc.mew.security.model.MewResource;
import mil.navy.nswc.mew.security.model.MilitaryBranch;

/** Creates authorization resources from the existing persistence model during the migration. */
public final class MewResourceFactory {
    private MewResourceFactory() { }

    public static MewResource forAsset(Asset asset) {
        Installation installation = asset.getCurrentLocation();
        if (installation == null && asset.getAssignedUnit() != null) {
            installation = asset.getAssignedUnit().getInstallation();
        }
        return new MewResource(String.valueOf(asset.getId()), installationCode(installation),
                branch(asset.getServiceBranch()), ClearanceLevel.SECRET,
                asset.getAssetModel() == null ? null : asset.getAssetModel().getCageCode());
    }

    public static MewResource forWorkOrder(WorkOrder workOrder) {
        Asset asset = workOrder.getAsset();
        Installation installation = workOrder.getInstallation();
        if (installation == null && asset != null) {
            installation = asset.getCurrentLocation();
        }
        return new MewResource(String.valueOf(workOrder.getId()), installationCode(installation),
                asset == null ? MilitaryBranch.JOINT : branch(asset.getServiceBranch()), ClearanceLevel.SECRET,
                asset == null || asset.getAssetModel() == null ? null : asset.getAssetModel().getCageCode());
    }

    public static MewResource forRequisition(Requisition requisition) {
        Installation installation = requisition.getRequestingUnit() == null ? null : requisition.getRequestingUnit().getInstallation();
        if (installation == null && requisition.getTiedToAsset() != null) {
            installation = requisition.getTiedToAsset().getCurrentLocation();
        }
        return new MewResource(String.valueOf(requisition.getId()), installationCode(installation),
                installation == null ? MilitaryBranch.JOINT : branch(installation.getServiceBranch()), ClearanceLevel.SECRET,
                requisition.getMaterial() == null ? null : requisition.getMaterial().getCageCode());
    }

    public static MewResource forInventoryBalance(InventoryBalance balance) {
        Installation installation = balance.getInstallation();
        return new MewResource(String.valueOf(balance.getId()), installationCode(installation),
                installation == null ? MilitaryBranch.JOINT : branch(installation.getServiceBranch()), ClearanceLevel.SECRET,
                balance.getMaterial() == null ? null : balance.getMaterial().getCageCode());
    }

    public static MewResource forInstallation(Installation installation) {
        return new MewResource(String.valueOf(installation.getId()), installationCode(installation),
                branch(installation.getServiceBranch()), ClearanceLevel.SECRET, null);
    }

    public static MewResource forHull(NavalHull hull) {
        Installation installation = hull.getHomeportInstallation();
        return new MewResource(String.valueOf(hull.getId()), installationCode(installation),
                installation == null ? MilitaryBranch.JOINT : branch(installation.getServiceBranch()), ClearanceLevel.SECRET, null);
    }

    public static MewResource forLoadout(HullSystemAssignment loadout) {
        return loadout.getAsset() != null ? forAsset(loadout.getAsset()) : forHull(loadout.getHull());
    }

    private static String installationCode(Installation installation) {
        return installation == null || installation.getInstallationCode() == null ? "" : installation.getInstallationCode();
    }

    private static MilitaryBranch branch(String serviceBranch) {
        if (serviceBranch == null || serviceBranch.isBlank()) {
            return MilitaryBranch.JOINT;
        }
        try {
            return MilitaryBranch.valueOf(serviceBranch.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return MilitaryBranch.JOINT;
        }
    }
}
