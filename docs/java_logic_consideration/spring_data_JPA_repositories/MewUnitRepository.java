package com.defense.mew.repository;

import com.defense.mew.entity.MewUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MewUnitRepository extends JpaRepository<MewUnit, String> {

    List<MewUnit> findByInstallationCode(String installationCode);

    List<MewUnit> findByServiceBranch(String serviceBranch);

    List<MewUnit> findByScifAccessTrue();
}
