package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.ReadinessLevel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mew_asset_status_log")
public class AssetStatusLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 4)
    private ReadinessLevel oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 4, nullable = false)
    private ReadinessLevel newStatus;

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public ReadinessLevel getOldStatus() { return oldStatus; }
    public void setOldStatus(ReadinessLevel oldStatus) { this.oldStatus = oldStatus; }
    public ReadinessLevel getNewStatus() { return newStatus; }
    public void setNewStatus(ReadinessLevel newStatus) { this.newStatus = newStatus; }
}
