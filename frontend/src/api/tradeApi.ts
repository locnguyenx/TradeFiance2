import { TradeInstrument, ImportLetterOfCredit, TradeParty, TradeProductCatalog, FeeConfiguration, UserAuthorityProfile, QueueItem } from './types';

const API_BASE = (typeof process !== 'undefined' && process.env.NEXT_PUBLIC_API_URL) 
  ? `${process.env.NEXT_PUBLIC_API_URL}/rest/s1/trade` 
  : '/rest/s1/trade';

export interface Kpis {
  pendingDrafts: number;
  expiringSoon: number;
  discrepantDocs: number;
}

export interface LcListResponse {
  lcList: (TradeInstrument & ImportLetterOfCredit)[];
  lcListCount: number;
}

export class ApiError extends Error {
  constructor(public status: number, public statusText: string, public body: any) {
    super(`API Error ${status}: ${statusText}`);
    this.name = 'ApiError';
  }
}

export const tradeApi = {
  credentials: '',

  setCredentials(username: string, password: string) {
    const creds = `${username}:${password}`;
    this.credentials = typeof btoa !== 'undefined' 
      ? btoa(creds) 
      : Buffer.from(creds).toString('base64');
  },

  clearCredentials() {
    this.credentials = '';
  },

  async _fetch(url: string, init?: RequestInit) {
    const headers = { ...init?.headers } as Record<string, string>;
    
    // Add Authorization header if credentials are set
    if (this.credentials) {
      headers['Authorization'] = `Basic ${this.credentials}`;
    }

    // Moqui CSRF handling: send moquiSessionToken from cookies if it exists
    if (typeof document !== 'undefined') {
        const cookies = document.cookie.split(';');
        const tokenCookie = cookies.find(c => c.trim().startsWith('moquiSessionToken='));
        if (tokenCookie) {
            const token = tokenCookie.split('=')[1];
            if (token) headers['moquiSessionToken'] = token;
        }
    }

    const res = await fetch(url, { 
        ...init, 
        headers,
        credentials: 'include' // Always include cookies for session persistence
    });
    
    if (!res.ok) {
      let errorBody = null;
      try {
        errorBody = await res.json();
      } catch (e) {
        try {
          errorBody = { message: await res.text() };
        } catch (e2) {
          errorBody = { message: 'Unknown error' };
        }
      }
      throw new ApiError(res.status, res.statusText, errorBody);
    }

    const resClone = res.clone();
    try {
      const json = await resClone.json();
      console.log(`DEBUG: fetch ${init?.method || 'GET'} ${url} -> ${res.status}`, { 
        requestBody: init?.body, 
        responseBody: json 
      });
    } catch (e) {
      console.log(`DEBUG: fetch ${init?.method || 'GET'} ${url} -> ${res.status} (no json body)`);
    }
    return res;
  },

  async login(username: string, password: string): Promise<{ loggedIn: boolean }> {
    const res = await this._fetch(`${API_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const result = await res.json();
    if (result.loggedIn) {
      this.setCredentials(username, password);
    }
    return result;
  },

  async getKpis(): Promise<Kpis> {
    const res = await this._fetch(`${API_BASE}/kpis`);
    const json = await res.json();
    return json.kpis || json;
  },

  async getImportLcs(params?: Record<string, any>): Promise<LcListResponse> {
    const query = params ? '?' + new URLSearchParams(params).toString() : '';
    const res = await this._fetch(`${API_BASE}/import-lc${query}`);
    return res.json();
  },

  async getImportLc(instrumentId: string): Promise<TradeInstrument & ImportLetterOfCredit> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}`);
    return res.json();
  },

  async getInstrument(instrumentId: string): Promise<TradeInstrument> {
    const res = await this._fetch(`${API_BASE}/instrument/${instrumentId}`);
    return res.json();
  },

  async authorize(instrumentId: string, userId?: string): Promise<{ isAuthorized: boolean }> {
    const res = await this._fetch(`${API_BASE}/authorize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ instrumentId, userId }),
    });
    return res.json();
  },

  async createLc(params: any): Promise<{ instrumentId: string; transactionRef: string; errors?: string[]; error?: string }> {
    const res = await this._fetch(`${API_BASE}/import-lc`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    return res.json();
  },

  async updateLc(instrumentId: string, data: any): Promise<any> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async validateLcSwiftFields(instrumentId: string, entityType: string): Promise<{ errors: { fieldName: string; message: string; violationType: string }[] }> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}/validate?entityType=${entityType}`);
    return res.json();
  },

  async getStandardClauses(type?: string): Promise<any[]> {
    const query = type ? `?clauseTypeEnumId=${type}` : '';
    const res = await this._fetch(`${API_BASE}/standard-clauses${query}`);
    const json = await res.json();
    return json.clauseList || [];
  },

  async getAuditLogs(transactionRef?: string): Promise<any[]> {
    const query = transactionRef ? `?transactionRef=${transactionRef}` : '';
    const res = await this._fetch(`${API_BASE}/audit-logs${query}`);
    const json = await res.json();
    return json.auditLogList || [];
  },

  async createLcPresentation(instrumentId: string, data: any): Promise<any> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}/presentation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async createLcAmendment(instrumentId: string, data: any): Promise<any> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}/amendment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async updateProductConfig(key: string, value: string): Promise<any> {
    const res = await this._fetch(`${API_BASE}/product-config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ configKey: key, configValue: value }),
    });
    return res.json();
  },

  async getApprovals(params?: { tier?: string; productType?: string; actionType?: string; priority?: string }): Promise<{ approvalsList: QueueItem[] }> {
    const query = params ? '?' + new URLSearchParams(params as Record<string, string>).toString() : '';
    const res = await this._fetch(`${API_BASE}/approvals${query}`);
    return res.json();
  },

  async getProductCatalog(): Promise<{ productList: TradeProductCatalog[] }> {
    const res = await this._fetch(`${API_BASE}/product-catalog`);
    return res.json();
  },

  async updateProductCatalog(productId: string, data: Partial<TradeProductCatalog>): Promise<any> {
    const res = await this._fetch(`${API_BASE}/product-catalog/${productId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async getFeeConfigurations(): Promise<{ feeList: FeeConfiguration[] }> {
    const res = await this._fetch(`${API_BASE}/fee-configurations`);
    return res.json();
  },

  async updateFeeConfiguration(feeConfigurationId: string, data: Partial<FeeConfiguration>): Promise<any> {
    const res = await this._fetch(`${API_BASE}/fee-configurations/${feeConfigurationId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async getParties(search?: string): Promise<{ partyList: TradeParty[] }> {
    const query = search ? `?search=${encodeURIComponent(search)}` : '';
    const res = await this._fetch(`${API_BASE}/parties${query}`);
    return res.json();
  },

  async getParty(partyId: string): Promise<TradeParty> {
    const res = await this._fetch(`${API_BASE}/parties/${partyId}`);
    return res.json();
  },

  async getUserAuthorityProfiles(): Promise<{ profileList: UserAuthorityProfile[] }> {
    const res = await this._fetch(`${API_BASE}/authority-profiles`);
    return res.json();
  },

  async rejectToMaker(instrumentId: string, rejectionReason: string): Promise<any> {
    const res = await this._fetch(`${API_BASE}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ instrumentId, rejectionReason }),
    });
    return res.json();
  },

  async waiveDiscrepancy(presentationId: string): Promise<any> {
    const res = await this._fetch(`${API_BASE}/import-lc/presentation/${presentationId}/waive`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicantDecisionEnumId: 'WAIVED' }),
    });
    return res.json();
  },

  async settleLcPresentation(instrumentId: string, presentationId: string, data: any): Promise<any> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}/settlement`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ presentationId, ...data }),
    });
    return res.json();
  },

  async updateUserAuthorityProfile(userAuthorityId: string, data: Partial<UserAuthorityProfile>): Promise<any> {
    const res = await this._fetch(`${API_BASE}/authority-profiles/${userAuthorityId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return res.json();
  },

  async getExposureData(): Promise<any> {
    const res = await this._fetch(`${API_BASE}/exposure-data`);
    return res.json();
  },

  async getFacilityDetail(facilityId: string): Promise<any> {
    const res = await this._fetch(`${API_BASE}/facilities?facilityId=${facilityId}`);
    return res.json();
  },
};
