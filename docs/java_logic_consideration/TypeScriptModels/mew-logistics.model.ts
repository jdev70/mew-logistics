export interface MewUnit {
    uic: string;
    serviceBranch: 'USMC' | 'USA' | 'USAF' | 'USN';
    installationCode: string;
    installationName: string;
    city: string;
    state: string;
    command: string;
    battalion: string;
    subUnit: string;
    area: string;
    scifAccess: boolean;
}

export interface PersonnelRbac {
    edipi: string;
    serviceBranch: string;
    rank: string;
    firstName: string;
    lastName: string;
    milEmail: string;
    installationCode: string;
    uic: string;
    battalion: string;
    subUnit: string;
    mos: string;
    mosTitle: string;
    rbacRole: string;
    accessScope: string;
    clearanceLevel: string;
    scifBadgeActive: boolean;
    authorizedSystems: string[];
    accountStatus: string;
    lastAccessReview: string;
}

export interface EwLogisticsAsset {
    assetId: string;
    serialNumber: string;
    tamcn: string;
    nsn: string;
    nomenclature: string;
    category: string;
    classificationMarking: 'TS//SCI' | 'SECRET//NOFORN' | 'SECRET' | 'CONFIDENTIAL';
    serviceBranch: 'USMC' | 'USA' | 'USAF' | 'USN';
    installationCode: string;
    installationName: string;
    uic: string;
    command: string;
    battalion: string;
    subUnit: string;
    storageLocation: string;
    readinessStatus: 'FMC' | 'PMC-M' | 'PMC-S' | 'NMCM';
    gcssEroWorkOrder: string;
    cryptoKeyLoaded: boolean;
    custodianEdipi: string;
    custodianName: string;
    approvingOfficerEdipi: string;
    lastPmcsDate: string;
}

export interface AssetReadinessUpdateRequest {
    readinessStatus: 'FMC' | 'PMC-M' | 'PMC-S' | 'NMCM';
    gcssEroWorkOrder: string;
    cryptoKeyLoaded: boolean;
}
