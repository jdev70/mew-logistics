package mil.dod.mew.service;

import mil.dod.mew.domain.entity.Asset;
import mil.dod.mew.domain.entity.MilitaryUnit;
import mil.dod.mew.domain.entity.UnitReadinessSnapshot;
import mil.dod.mew.domain.enums.ReadinessLevel;
import mil.dod.mew.repository.AssetRepository;
import mil.dod.mew.repository.MilitaryUnitRepository;
import mil.dod.mew.repository.UnitReadinessSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job replacing Celery Beat for daily DoDAF unit readiness snapshots.
 */
@Service
public class ReadinessSchedulerService {

    private final MilitaryUnitRepository unitRepository;
    private final AssetRepository assetRepository;
    private final UnitReadinessSnapshotRepository snapshotRepository;

    public ReadinessSchedulerService(MilitaryUnitRepository unitRepository,
                                     AssetRepository assetRepository,
                                     UnitReadinessSnapshotRepository snapshotRepository) {
        this.unitRepository = unitRepository;
        this.assetRepository = assetRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generateDailyReadinessSnapshots() {
        LocalDate today = LocalDate.now();
        List<MilitaryUnit> units = unitRepository.findAll();

        for (MilitaryUnit unit : units) {
            List<Asset> assets = assetRepository.findByAssignedUnitId(unit.getId());
            int fmc = (int) assets.stream().filter(a -> a.getReadinessStatus() == ReadinessLevel.FMC).count();
            int pmc = (int) assets.stream().filter(a -> a.getReadinessStatus() == ReadinessLevel.PMC).count();
            int nmc = assets.size() - fmc - pmc;

            UnitReadinessSnapshot snapshot = snapshotRepository
                    .findByUnitIdAndSnapshotDate(unit.getId(), today)
                    .orElseGet(UnitReadinessSnapshot::new);

            snapshot.setUnit(unit);
            snapshot.setSnapshotDate(today);
            snapshot.setTotalAssets(assets.size());
            snapshot.setFmcAssets(fmc);
            snapshot.setPmcAssets(pmc);
            snapshot.setNmcAssets(nmc);
            snapshotRepository.save(snapshot);
        }
    }
}
