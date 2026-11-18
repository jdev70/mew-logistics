// MEW Logistics API Service (mew-logistics.service.ts)

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    AssetReadinessUpdateRequest,
    EwLogisticsAsset,
    MewUnit,
    PersonnelRbac,
} from '../models/mew-logistics.model';

@Injectable({
    providedIn: 'root',
})
export class MewLogisticsService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = 'http://localhost:8080/api/v1/mew';

    getCurrentUserProfile(): Observable<PersonnelRbac> {
        return this.http.get<PersonnelRbac>(`${this.baseUrl}/rbac/me`);
    }

    getUnits(branch?: string, installationCode?: string): Observable<MewUnit[]> {
        let params = new HttpParams();
        if (branch) params = params.set('branch', branch);
        if (installationCode) params = params.set('installationCode', installationCode);
        return this.http.get<MewUnit[]>(`${this.baseUrl}/units`, { params });
    }

    getAuthorizedAssets(filters?: {
        installationCode?: string;
        uic?: string;
        readinessStatus?: string;
    }): Observable<EwLogisticsAsset[]> {
        let params = new HttpParams();
        if (filters?.installationCode) {
            params = params.set('installationCode', filters.installationCode);
        }
        if (filters?.uic) {
            params = params.set('uic', filters.uic);
        }
        if (filters?.readinessStatus) {
            params = params.set('readinessStatus', filters.readinessStatus);
        }
        return this.http.get<EwLogisticsAsset[]>(`${this.baseUrl}/assets`, { params });
    }

    updateAssetReadiness(
        assetId: string,
        payload: AssetReadinessUpdateRequest
    ): Observable<EwLogisticsAsset> {
        return this.http.patch<EwLogisticsAsset>(
            `${this.baseUrl}/assets/${assetId}/readiness`,
            payload
        );
    }
}
