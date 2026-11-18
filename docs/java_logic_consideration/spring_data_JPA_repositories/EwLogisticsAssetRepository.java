package com.defense.mew.repository;

import com.defense.mew.entity.EwLogisticsAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EwLogisticsAssetRepository extends JpaRepository<EwLogisticsAsset, String> {

    Optional<EwLogisticsAsset> findBySerialNumber(String serialNumber);

    List<EwLogisticsAsset> findByUnitUic(String uic);

    List<EwLogisticsAsset> findByInstallationCode(String installationCode);

    List<EwLogisticsAsset> findByReadinessStatusNot(String readinessStatus);

    List<EwLogisticsAsset> findByCustodianEdipi(String custodianEdipi);

    List<EwLogisticsAsset> findByCryptoKeyLoadedTrueAndClassificationMarking(String classificationMarking);

    @Query("SELECT a.readinessStatus, COUNT(a) FROM EwLogisticsAsset a WHERE a.serviceBranch = :branch GROUP BY a.readinessStatus")
    List<Object[]> countReadinessByBranch(@Param("branch") String branch);
}
