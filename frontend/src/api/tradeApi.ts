// ABOUTME: tradeApi.ts provides a unified client for interacting with the Moqui Trade Finance REST API.
// ABOUTME: Handles fetching KPIs, listing Import LCs, and submitting authorization actions.

const API_BASE = (typeof process !== 'undefined' && process.env.NEXT_PUBLIC_API_URL) 
  ? `${process.env.NEXT_PUBLIC_API_URL}/rest/s1/trade` 
  : '/rest/s1/trade';

export interface ImportLc {
  instrumentId: string;
  transactionRef: string;
  businessStateId: string;
  baseEquivalentAmount: number;
  applicantName?: string;
  beneficiaryName?: string;
  amount?: number;
  currency?: string;
  expiryDate?: string;
  slaDaysRemaining?: number;
}

export interface Kpis {
  pendingDrafts: number;
  expiringSoon: number;
  discrepantDocs: number;
}

export interface LcListResponse {
  lcList: ImportLc[];
  lcListCount: number;
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
    if (this.credentials) {
      headers['Authorization'] = `Basic ${this.credentials}`;
      console.log(`DEBUG: Using Authorization header: ${headers['Authorization']}`);
    }
    const res = await fetch(url, { ...init, headers });
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

  async getImportLc(instrumentId: string): Promise<ImportLc & any> {
    const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}`);
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

  async createLc(data: any): Promise<{ instrumentId: string; errors?: string[]; error?: string }> {
    const res = await this._fetch(`${API_BASE}/import-lc`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
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
};
