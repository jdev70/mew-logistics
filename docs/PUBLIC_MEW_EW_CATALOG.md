# Public MEW/EW Installation Catalog

## Scope

This catalog supports the `synthetic-data` profile. It includes only installations with a public, attributable military installation, platform, EW/MEW mission, sustainment, test, training, or support relationship. It does not represent a real-time order of battle, deployment schedule, or authoritative inventory.

The current public release of DoD installation geospatial data is a baseline for installation identity and location, but it expressly excludes some sites for security and data-availability reasons. The Base Structure Report is CUI. Therefore this catalog must be described as **public-source governed**, never complete or authoritative for operational use.

## Inclusion rules

1. The installation is identified in a public DoD or service source.
2. A public source attributes an EW/MEW platform, mission, system, sustainment, test, or training association to the installation.
3. Platform, class, and JETDS nomenclature are retained only when publicly available.
4. Personnel, equipment serials, readiness, supply, maintenance, and audit events are generated demonstration data.

## Seeded installation groups

| Service | Installations in seed catalog | Public association type |
| --- | --- | --- |
| USN | NSWC Crane; NS Norfolk; Naval Base San Diego; NAS Whidbey Island; NSB New London; CFA Yokosuka | Surface EW sustainment, fleet surface/subsurface, and naval aviation EW |
| USMC | MCB Camp Pendleton; MCB Camp Lejeune; MCAS Cherry Point; MCAS Yuma | Expeditionary and aviation EW support |
| USAF | Offutt AFB; Davis-Monthan AFB; Eglin AFB | ISR, airborne electromagnetic attack, and EW test/evaluation |
| USA | JBLM; Fort Shafter; JBLE | Multidomain/terrestrial EW and watercraft support |

## Source register

- [DoD Military Installations, Ranges, and Training Areas release notes](https://www.acq.osd.mil/eie/imr/rpid/disdi/Downloads/release_notes.pdf) — public installation/location baseline and known coverage limits.
- [Navy installation reference](https://www.navy.mil/Portals/1/public-affairs/2024-owners-operators-manual.pdf?ver=wLr_kIybry089ROOjSodug%3D%3D) — public Navy installation identities.
- [NAVSEA NSWC Crane EW reference](https://www.navsea.navy.mil/Media/News/Article/4203274/nswc-crane-employee-accelerates-critical-electromagnetic-spectrum-capabilities/) — NSWC Crane’s public surface-EW sustainment association.
- [Navy SEWIP public overview](https://www.navsea.navy.mil/Portals/103/Documents/Exhibits/SNA2022/SNA2022-CAPTJasonHall-SEWIP.pdf) — public AN/SLQ-32(V)6 information.
- [Marine Corps installation reference](https://www.hqmc.marines.mil/Portals/61/Users/148/88/3988/Retiree%20Information.pdf) — public Marine Corps installation identities.
- [Air Force EA-37B reference](https://www.dm.af.mil/Media/News/Article/4255968/the-ea-37b-at-dm/) and [Offutt/Davis-Monthan EW integration](https://www.dm.af.mil/Media/Article-View/Article/4366631/rc-135-rivet-joint-ea-37b-compass-call-conduct-historic-sorties/) — public USAF EW associations.
- [Joint Base Lewis-McChord official site](https://home.army.mil/lewis-mcchord/index.php/about) and [Joint-basing reference](https://www.army.mil/article/36298/bases_get_new_names_in_realignment) — public Army/joint installation identities.

## Maintenance

Additions require a source URL, verification date, branch, geographic installation reference, and an attributable MEW/EW association. If a platform’s public homeport changes, update only the catalog association; never use this dataset as a live positioning system.
