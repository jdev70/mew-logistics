package mil.navy.nswc.mew.service;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.MilitaryUnit;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
import mil.navy.nswc.mew.web.dto.UnitDashboardDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final MilitaryUnitRepository unitRepository;
    private final AssetRepository assetRepository;

    public DashboardService(MilitaryUnitRepository unitRepository, AssetRepository assetRepository) {
        this.unitRepository = unitRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public List<UnitDashboardDto> getUnitDashboardMetrics(String assetClass) {
        List<MilitaryUnit> units = unitRepository.findAll();
        return units.stream().map(unit -> {
            List<Asset> assets = assetRepository.findByAssignedUnitId(unit.getId());
            if (assetClass != null && !assetClass.trim().isEmpty()) {
                assets = assets.stream()
                        .filter(a -> assetClass.equalsIgnoreCase(a.getAssetModel().getAssetClass()))
                        .collect(Collectors.toList());
            }

            double avgMtbf = assets.stream()
                    .mapToDouble(a -> a != null && a.getMtbfHours() != null ? a.getMtbfHours() : 0.0)
                    .average()
                    .orElse(0.0);
            double avgMttr = assets.stream()
                    .mapToDouble(a -> a != null && a.getMttrHours() != null ? a.getMttrHours() : 0.0)
                    .average()
                    .orElse(0.0);
            double avgAo = assets.stream()
                    .mapToDouble(a -> a != null && a.getOperationalAvailabilityPct() != null
                            ? a.getOperationalAvailabilityPct()
                            : 100.0)
                    .average()
                    .orElse(100.0);
            long fmc = assets.stream().filter(a -> a.getReadinessStatus() == ReadinessLevel.FMC).count();
            long degraded = assets.size() - fmc;

            UnitDashboardDto dto = new UnitDashboardDto();
            dto.setId(unit.getId());
            dto.setName(unit.getName());
            dto.setUic(unit.getUic());
            dto.setCommandType(unit.getCommandType());
            dto.setInstallationName(unit.getInstallation() != null ? unit.getInstallation().getName() : "N/A");
            dto.setPersonnelReadinessPct(unit.getPersonnelReadinessPct());
            dto.setEquipmentReadinessPct(unit.getEquipmentReadinessPct());
            dto.setFleetAvgMtbf(avgMtbf);
            dto.setFleetAvgMttr(avgMttr);
            dto.setFleetAvgAo(avgAo);
            dto.setTotalAssets((long) assets.size());
            dto.setFmcAssets(fmc);
            dto.setDegradedAssets(degraded);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportUnitMetricsCsv(String assetClass) {
        List<UnitDashboardDto> rows = getUnitDashboardMetrics(assetClass);
        StringBuilder sb = new StringBuilder();
        sb.append("Unit Name,UIC,Command Type,Installation,Personnel Readiness (%),Equipment Readiness (%),Avg MTBF (Hours),Avg MTTR (Hours),Operational Availability (Ao %)\n");
        for (UnitDashboardDto row : rows) {
            sb.append(escapeCsv(row.getName())).append(',')
              .append(escapeCsv(row.getUic())).append(',')
              .append(escapeCsv(row.getCommandType())).append(',')
              .append(escapeCsv(row.getInstallationName())).append(',')
              .append(row.getPersonnelReadinessPct()).append(',')
              .append(row.getEquipmentReadinessPct()).append(',')
              .append(row.getFleetAvgMtbf()).append(',')
              .append(row.getFleetAvgMttr()).append(',')
              .append(row.getFleetAvgAo()).append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
