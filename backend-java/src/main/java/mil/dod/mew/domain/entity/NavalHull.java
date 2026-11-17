package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.EmconPosture;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mew_naval_hull")
public class NavalHull extends BaseEntity {

    @Column(name = "hull_id", length = 20, unique = true, nullable = false)
    private String hullId;

    @Column(name = "hull_number", length = 20, unique = true, nullable = false)
    private String hullNumber;

    @Column(name = "vessel_name", length = 150, nullable = false)
    private String vesselName;

    @Column(name = "ship_class", length = 150, nullable = false)
    private String shipClass;

    @Column(name = "domain", length = 50, nullable = false)
    private String domain = "Surface";

    @Column(name = "homeport", length = 150, nullable = false)
    private String homeport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeport_installation_id")
    private Installation homeportInstallation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "emcon_posture", length = 20, nullable = false)
    private EmconPosture emconPosture = EmconPosture.CHARLIE;

    public String getHullId() { return hullId; }
    public void setHullId(String hullId) { this.hullId = hullId; }
    public String getHullNumber() { return hullNumber; }
    public void setHullNumber(String hullNumber) { this.hullNumber = hullNumber; }
    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }
    public String getShipClass() { return shipClass; }
    public void setShipClass(String shipClass) { this.shipClass = shipClass; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getHomeport() { return homeport; }
    public void setHomeport(String homeport) { this.homeport = homeport; }
    public Installation getHomeportInstallation() { return homeportInstallation; }
    public void setHomeportInstallation(Installation homeportInstallation) { this.homeportInstallation = homeportInstallation; }
    public MilitaryUnit getUnit() { return unit; }
    public void setUnit(MilitaryUnit unit) { this.unit = unit; }
    public EmconPosture getEmconPosture() { return emconPosture; }
    public void setEmconPosture(EmconPosture emconPosture) { this.emconPosture = emconPosture; }
}
