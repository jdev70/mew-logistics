package com.defense.mew.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "joint_mew_logistics_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EwLogisticsAsset {

    @Id
    @Column(name = "asset_id", length = 24, nullable = false)
    private String assetId;

    @Column(name = "serial_number", length = 32, unique = true, nullable = false)
    private String serialNumber;

    @Column(name = "tamcn", length = 16, nullable = false)
    private String tamcn;

    @Column(name = "nsn", length = 24, nullable = false)
    private String nsn;

    @Column(name = "nomenclature", nullable = false)
    private String nomenclature;

    @Column(name = "category", length = 128, nullable = false)
    private String category;

    @Column(name = "classification_marking", length = 32, nullable = false)
    private String classificationMarking;

    @Column(name = "service_branch", length = 10, nullable = false)
    private String serviceBranch;

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

    @Column(name = "storage_location", nullable = false)
    private String storageLocation;

    @Column(name = "readiness_status", length = 16, nullable = false)
    private String readinessStatus;

    @Column(name = "gcss_ero_work_order", length = 24, nullable = false)
    private String gcssEroWorkOrder;

    @Column(name = "crypto_key_loaded", nullable = false)
    private Boolean cryptoKeyLoaded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodian_edipi", referencedColumnName = "edipi", nullable = false)
    private PersonnelRbac custodian;

    @Column(name = "custodian_name", length = 64, nullable = false)
    private String custodianName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approving_officer_edipi", referencedColumnName = "edipi", nullable = false)
    private PersonnelRbac approvingOfficer;

    @Column(name = "last_pmcs_date", nullable = false)
    private LocalDate lastPmcsDate;
}
