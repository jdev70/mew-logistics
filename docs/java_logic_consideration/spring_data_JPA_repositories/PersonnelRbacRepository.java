package com.defense.mew.repository;

import com.defense.mew.entity.PersonnelRbac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonnelRbacRepository extends JpaRepository<PersonnelRbac, String> {

    Optional<PersonnelRbac> findByMilEmailIgnoreCase(String milEmail);

    List<PersonnelRbac> findByUnitUic(String uic);

    List<PersonnelRbac> findByInstallationCodeAndRbacRole(String installationCode, String rbacRole);

    List<PersonnelRbac> findByClearanceLevelAndScifBadgeActiveTrue(String clearanceLevel);

    @Query("SELECT p FROM PersonnelRbac p WHERE p.lastAccessReview < :cutoffDate AND p.accountStatus = 'ACTIVE'")
    List<PersonnelRbac> findAccountsDueForRecertification(@Param("cutoffDate") LocalDate cutoffDate);
}
