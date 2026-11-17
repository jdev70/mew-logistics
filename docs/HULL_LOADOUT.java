package com.mew.logistics.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String installationCode; // e.g., "NS-NORFOLK", "NAS-WHIDBEY"
    private String name;             // e.g., "Naval Station Norfolk"
    
    @Enumerated(EnumType.STRING)
    private MilitaryBranch branch;   // USN, USMC, USAF, USA
    
    private String aor;              // Area of Responsibility: INDOPACOM, EUCOM, CENTCOM, NORTHCOM
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformHull {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String hullIdentifier;   // e.g., "DDG-114", "BuNo-168934", "AF-19-0042", "LSV-4"
    
    private String platformName;     // e.g., "USS Ralph Johnson", "EA-18G Growler"
    private String platformClass;    // e.g., "Arleigh Burke Flight IIA"
    private String baselineConfig;   // e.g., "Aegis Baseline 9 / TI-16"

    @Enumerated(EnumType.STRING)
    private MilitaryBranch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation homeInstallation;

    @OneToMany(mappedBy = "platformHull", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SystemLoadout> systemLoadouts = new ArrayList<>();
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLoadout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jetdsDesignation; // e.g., "AN/SLQ-32(V)7"
    private String systemNomenclature; // e.g., "SEWIP Block 3 Electronic Attack"
    private String serialNumber;     // e.g., "SN-SLQ32-88412"
    private String nsn;              // 13-digit National Stock Number: "5865-01-682-4419"
    private String cageCode;         // 5-char Manufacturer ID: "06481" (Northrop Grumman), "96214" (Raytheon)

    @Enumerated(EnumType.STRING)
    private ReadinessStatus readinessStatus; // FMC, PMC, NMC

    private Double operationalReadinessRate; // e.g., 94.5%

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_hull_id")
    private PlatformHull platformHull;
}
