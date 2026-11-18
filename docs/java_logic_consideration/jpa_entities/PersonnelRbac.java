package com.defense.mew.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "joint_mew_personnel_rbac")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonnelRbac {

    @Id
    @Column(name = "edipi", length = 16, nullable = false)
    private String edipi;

    @Column(name = "service_branch", length = 10, nullable = false)
    private String serviceBranch;

    @Column(name = "rank", length = 16, nullable = false)
    private String rank;

    @Column(name = "last_name", length = 64, nullable = false)
    private String lastName;

    @Column(name = "first_name", length = 64, nullable = false)
    private String firstName;

    @Column(name = "mil_email", length = 128, unique = true, nullable = false)
    private String milEmail;

    @Column(name = "installation_code", length = 32, nullable = false)
    private String installationCode;

    @Column(name = "installation_name", nullable = false)
    private String installationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uic", referencedColumnName = "uic", nullable = false)
    private MewUnit unit;

    @Column(name = "command", nullable = false)
    private String command;

    @Column(name = "battalion", nullable = false)
    private String battalion;

    @Column(name = "sub_unit", nullable = false)
    private String subUnit;

    @Column(name = "area", nullable = false)
    private String area;

    @Column(name = "mos", length = 16, nullable = false)
    private String mos;

    @Column(name = "mos_title", nullable = false)
    private String mosTitle;

    @Column(name = "rbac_role", length = 32, nullable = false)
    private String rbacRole;

    @Column(name = "access_scope", length = 32, nullable = false)
    private String accessScope;

    @Column(name = "clearance_level", length = 16, nullable = false)
    private String clearanceLevel;

    @Column(name = "scif_badge_active", nullable = false)
    private Boolean scifBadgeActive;

    @Column(name = "authorized_systems", length = 512, nullable = false)
    private String authorizedSystems;

    @Column(name = "account_status", length = 32, nullable = false)
    private String accountStatus;

    @Column(name = "last_access_review", nullable = false)
    private LocalDate lastAccessReview;

    @Transient
    public List<String> getAuthorizedSystemsList() {
        if (authorizedSystems == null || authorizedSystems.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(authorizedSystems.split("\\|"));
    }
}
