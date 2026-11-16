package mil.navy.nswc.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "mew_unit_readiness_snap",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "snapshot_date"})
)
public class UnitReadinessSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private MilitaryUnit unit;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate = LocalDate.now();

    @Column(name = "total_personnel", nullable = false)
    private Integer totalPersonnel = 0;

    @Column(name = "deployable_personnel", nullable = false)
    private Integer deployablePersonnel = 0;

    @Column(name = "total_assets", nullable = false)
    private Integer totalAssets = 0;

    @Column(name = "fmc_assets", nullable = false)
    private Integer fmcAssets = 0;

    @Column(name = "pmc_assets", nullable = false)
    private Integer pmcAssets = 0;

    @Column(name = "nmc_assets", nullable = false)
    private Integer nmcAssets = 0;

    public double getOperationalReadinessRate() {
        if (totalAssets == null || totalAssets == 0) {
            return 0.0;
        }
        return ((double) (fmcAssets + pmcAssets) / totalAssets) * 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MilitaryUnit getUnit() { return unit; }
    public void setUnit(MilitaryUnit unit) { this.unit = unit; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalPersonnel() { return totalPersonnel; }
    public void setTotalPersonnel(Integer totalPersonnel) { this.totalPersonnel = totalPersonnel; }
    public Integer getDeployablePersonnel() { return deployablePersonnel; }
    public void setDeployablePersonnel(Integer deployablePersonnel) { this.deployablePersonnel = deployablePersonnel; }
    public Integer getTotalAssets() { return totalAssets; }
    public void setTotalAssets(Integer totalAssets) { this.totalAssets = totalAssets; }
    public Integer getFmcAssets() { return fmcAssets; }
    public void setFmcAssets(Integer fmcAssets) { this.fmcAssets = fmcAssets; }
    public Integer getPmcAssets() { return pmcAssets; }
    public void setPmcAssets(Integer pmcAssets) { this.pmcAssets = pmcAssets; }
    public Integer getNmcAssets() { return nmcAssets; }
    public void setNmcAssets(Integer nmcAssets) { this.nmcAssets = nmcAssets; }
}
