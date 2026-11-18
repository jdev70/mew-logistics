package com.defense.mew.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "joint_mew_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MewUnit {

    @Id
    @Column(name = "uic", length = 16, nullable = false)
    private String uic;

    @Column(name = "command", nullable = false)
    private String command;

    @Column(name = "battalion", nullable = false)
    private String battalion;

    @Column(name = "sub_unit", nullable = false)
    private String subUnit;

    @Column(name = "area", nullable = false)
    private String area;

    @Column(name = "scif_access", nullable = false)
    private Boolean scifAccess;

    @Column(name = "service_branch", length = 10, nullable = false)
    private String serviceBranch;

    @Column(name = "installation_code", length = 32, nullable = false)
    private String installationCode;

    @Column(name = "installation_name", nullable = false)
    private String installationName;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", length = 8, nullable = false)
    private String state;

    @Column(name = "email_domain", length = 32, nullable = false)
    private String emailDomain;
}
