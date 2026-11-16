# 1. Fetch existing installations to map installationCode -> UUID dynamically
$installations = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/installations/" -Headers $headers
$instMap = @{}
foreach ($inst in $installations) {
    $instMap[$inst.installation_code] = $inst.id
}

# ==========================================
# U.S. MARINE CORPS (USMC) PERSONNEL
# ==========================================

# 1st Radio Battalion - Enlisted SIGINT/EW Operator (2651)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-2001"
    dodId = "1409823411"
    firstName = "Marcus"
    middleInitial = "J"
    lastName = "Vance"
    personnelCategory = "ENLISTED"
    paygrade = "E-6"
    rank = "SSgt"
    specialtyCode = "2651"
    title = "Special Intelligence System Administrator/Communicator"
    functionalRole = "FIELD_EW_OPERATOR"
    assignedUic = "M20110"
    authorizedCageCodes = "26916,1V0L6"
    installation = @{ id = $instMap["MCB-PENDLETON"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# 2d Radio Battalion - Cyberspace & EW Officer (1702)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-2002"
    dodId = "1409823412"
    firstName = "Elena"
    middleInitial = "R"
    lastName = "Rostova"
    personnelCategory = "OFFICER"
    paygrade = "O-3"
    rank = "Capt"
    specialtyCode = "1702"
    title = "Cyberspace Warfare Officer"
    functionalRole = "CYBER_EW_OFFICER"
    assignedUic = "M20120"
    authorizedCageCodes = "1V0L6,14304"
    installation = @{ id = $instMap["MCB-LEJEUNE"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# ==========================================
# U.S. ARMY (USA) PERSONNEL
# ==========================================

# 11th Cyber Battalion - Electromagnetic Warfare Specialist (17E)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-3001"
    dodId = "1508734190"
    firstName = "Devon"
    middleInitial = "K"
    lastName = "Brooks"
    personnelCategory = "ENLISTED"
    paygrade = "E-5"
    rank = "SGT"
    specialtyCode = "17E"
    title = "Electromagnetic Warfare Specialist"
    functionalRole = "FIELD_EW_OPERATOR"
    assignedUic = "W4G1AA"
    authorizedCageCodes = "03538,1V0L6"
    installation = @{ id = $instMap["FT-GORDON"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# III Armored Corps - Electronic Warfare Technician (170B)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-3002"
    dodId = "1508734191"
    firstName = "Hector"
    middleInitial = "L"
    lastName = "Mendoza"
    personnelCategory = "WARRANT_OFFICER"
    paygrade = "W-3"
    rank = "CW3"
    specialtyCode = "170B"
    title = "Electronic Warfare Technician"
    functionalRole = "CYBER_EW_OFFICER"
    assignedUic = "W8C2AA"
    authorizedCageCodes = "03538,64622"
    installation = @{ id = $instMap["FT-CAVAZOS"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# ==========================================
# U.S. AIR FORCE (USAF) PERSONNEL
# ==========================================

# 55th Electronic Combat Group - Airborne ISR/EW Operator (1A8X2)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-4001"
    dodId = "1607645280"
    firstName = "Sarah"
    middleInitial = "M"
    lastName = "Jenkins"
    personnelCategory = "ENLISTED"
    paygrade = "E-7"
    rank = "MSgt"
    specialtyCode = "1A8X2"
    title = "Airborne Intelligence, Surveillance, and Reconnaissance Operator"
    functionalRole = "AIRBORNE_EW_SPECIALIST"
    assignedUic = "FF4877"
    authorizedCageCodes = "94117"
    installation = @{ id = $instMap["AFB-DM"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# 350th Spectrum Warfare Wing - Cyber Warfare Operations (1B4X1)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-4002"
    dodId = "1607645281"
    firstName = "Liam"
    middleInitial = "T"
    lastName = "OConnor"
    personnelCategory = "ENLISTED"
    paygrade = "E-6"
    rank = "TSgt"
    specialtyCode = "1B4X1"
    title = "Cyber Warfare Operations Specialist"
    functionalRole = "CYBER_EW_OFFICER"
    assignedUic = "FF2823"
    authorizedCageCodes = "94117,07668"
    installation = @{ id = $instMap["AFB-EGLIN"] }
    securityClearance = "TS/SCI"
    operationalStatus = "DEPLOYABLE"
    active = $true
} | ConvertTo-Json -Depth 3)

# ==========================================
# JOINT COMMAND (USSTRATCOM JEMSOC - CROSS-BRANCH RBAC ADMIN)
# ==========================================

# Offutt AFB - Joint Electromagnetic Spectrum Operations Director (13B / Joint)
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/personnel/" -Headers $headers -Body (@{
    personnelId = "PERS-9001"
    dodId = "1105512399"
    firstName = "Arthur"
    middleInitial = "D"
    lastName = "Sterling"
    personnelCategory = "OFFICER"
    paygrade = "O-7"
    rank = "Brig Gen"
    specialtyCode = "13B"
    title = "Director, Joint Electromagnetic Spectrum Operations Center (JEMSOC)"
    functionalRole = "JOINT_SPECTRUM_DIRECTOR"
    assignedUic = "HJEMSOC"
    authorizedCageCodes = "03538,26916,1V0L6,14304,64622,94117,07668"
    installation = @{ id = $instMap["AFB-OFFUTT"] }
    securityClearance = "TS/SCI"
    operationalStatus = "COMMAND_STAFF"
    active = $true
} | ConvertTo-Json -Depth 3)
