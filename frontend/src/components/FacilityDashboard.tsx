'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: FacilityDashboard implements exposure visualization for credit limits.
// ABOUTME: Provides real-time utilization tracking and risk warnings for v3.0.

interface Facility {
    facilityId: string;
    facilityName: string;
    limit: number;
    firm: number;
    contingent: number;
    reserved: number;
    available: number;
}

interface Transaction {
    instrumentId: string;
    transactionRef: string;
    businessStateId: string;
    effectiveOutstandingAmount: number;
    transactionDate: string;
}

interface ExposureData {
    totalLimit: number;
    totalExposure: number;
    totalFirm: number;
    totalContingent: number;
    totalReserved: number;
    utilizationPercent: number;
    facilityBreakdown: Facility[];
}

export const FacilityDashboard: React.FC = () => {
    const [data, setData] = useState<ExposureData>({
        totalLimit: 0,
        totalExposure: 0,
        totalFirm: 0,
        totalContingent: 0,
        totalReserved: 0,
        utilizationPercent: 0,
        facilityBreakdown: []
    });
    const [selectedFacilityId, setSelectedFacilityId] = useState<string>('');
    const [detail, setDetail] = useState<{ facility: Facility; transactions: Transaction[] } | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchOverview = async () => {
            try {
                const exposure = await tradeApi.getExposureData();
                if (exposure) {
                    setData({
                        totalLimit: exposure.totalLimit ?? 0,
                        totalExposure: exposure.totalExposure ?? 0,
                        totalFirm: exposure.totalFirm ?? 0,
                        totalContingent: exposure.totalContingent ?? 0,
                        totalReserved: exposure.totalReserved ?? 0,
                        utilizationPercent: exposure.utilizationPercent ?? 0,
                        facilityBreakdown: exposure.facilityBreakdown ?? []
                    });
                    if (exposure.facilityBreakdown?.length > 0) {
                        setSelectedFacilityId(exposure.facilityBreakdown[0].facilityId);
                    }
                }
            } catch (err) {
                setError('Failed to load overview data');
            } finally {
                setLoading(false);
            }
        };
        fetchOverview();
    }, []);

    useEffect(() => {
        if (!selectedFacilityId) return;
        const fetchDetail = async () => {
            try {
                const result = await tradeApi.getFacilityDetail(selectedFacilityId);
                if (result) setDetail(result);
            } catch (err) {
                console.error('Detail fetch failed', err);
            }
        };
        fetchDetail();
    }, [selectedFacilityId]);

    if (loading) return <div className="loading-state">Loading Facility Data...</div>;
    if (error) return <div className="error-state">{error}</div>;

    const selectedFacility = detail?.facility || data.facilityBreakdown.find(f => f.facilityId === selectedFacilityId) || {
        facilityId: '',
        facilityName: 'No Facility Selected',
        limit: 0,
        firm: 0,
        contingent: 0,
        reserved: 0,
        available: 0
    };
    const transactions = detail?.transactions || [];

    const isHighUtilization = selectedFacility.limit > 0 && 
        ((selectedFacility.limit - selectedFacility.available) / selectedFacility.limit) > 0.9;

    return (
        <div className="facility-dashboard">
            <header className="dashboard-header">
                <h2>Credit Facility Dashboard</h2>
                <div className="header-actions">
                    <select 
                        value={selectedFacilityId} 
                        onChange={(e) => setSelectedFacilityId(e.target.value)}
                        className="facility-selector"
                    >
                        {data.facilityBreakdown.map(f => (
                            <option key={f.facilityId} value={f.facilityId}>
                                {f.facilityName} ({f.facilityId})
                            </option>
                        ))}
                    </select>
                    {isHighUtilization && (
                        <div className="warning-badge">HIGH UTILIZATION</div>
                    )}
                </div>
            </header>

            {selectedFacility && (
                <>
                    <div className="exposure-widget">
                        <div className="widget-header">
                            <span className="widget-title">Facility Exposure Breakdown</span>
                            <span className="total-limit">Limit: <span className="value">${selectedFacility.limit.toLocaleString()}</span></span>
                        </div>
                        
                        <div className="segmented-bar-container">
                            <div className="segmented-bar">
                                <div className="segment firm" style={{ width: `${selectedFacility.limit > 0 ? (selectedFacility.firm / selectedFacility.limit) * 100 : 0}%` }}>
                                    <span className="segment-label">Firm</span>
                                </div>
                                <div className="segment contingent" style={{ width: `${selectedFacility.limit > 0 ? (selectedFacility.contingent / selectedFacility.limit) * 100 : 0}%` }}>
                                    <span className="segment-label">Contingent</span>
                                </div>
                                <div className="segment reserved" style={{ width: `${selectedFacility.limit > 0 ? (selectedFacility.reserved / selectedFacility.limit) * 100 : 0}%` }}>
                                    <span className="segment-label">Reserved</span>
                                </div>
                                <div className="segment available" style={{ width: `${selectedFacility.limit > 0 ? (selectedFacility.available / selectedFacility.limit) * 100 : 0}%` }}>
                                    <span className="segment-label">Available</span>
                                </div>
                            </div>
                        </div>

                        <div className="widget-legend">
                            <div className="legend-item"><span className="dot firm"></span> Firm: <span className="value">${selectedFacility.firm.toLocaleString()}</span></div>
                            <div className="legend-item"><span className="dot contingent"></span> Contingent: <span className="value">${selectedFacility.contingent.toLocaleString()}</span></div>
                            <div className="legend-item"><span className="dot reserved"></span> Reserved: <span className="value">${selectedFacility.reserved.toLocaleString()}</span></div>
                            <div className="legend-item"><span className="dot available"></span> Available: <span className="value">${selectedFacility.available.toLocaleString()}</span></div>
                        </div>
                    </div>

                    <section className="utilization-section">
                        <h3>Utilization Breakdown</h3>
                        <div className="table-container">
                            <table className="breakdown-table">
                                <thead>
                                    <tr>
                                        <th>Transaction Ref</th>
                                        <th>Date</th>
                                        <th>State</th>
                                        <th className="text-right">Amount</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {transactions.length > 0 ? transactions.map(t => (
                                        <tr key={t.instrumentId}>
                                            <td>
                                                <a href={`/instruments/${t.instrumentId}`} className="tx-link">
                                                    {t.transactionRef}
                                                </a>
                                            </td>
                                            <td>{new Date(t.transactionDate).toLocaleDateString()}</td>
                                            <td><span className={`status-tag ${t.businessStateId?.split('_')?.pop()?.toLowerCase() || 'draft'}`}>{t.businessStateId}</span></td>
                                            <td className="text-right">${t.effectiveOutstandingAmount.toLocaleString()}</td>
                                        </tr>
                                    )) : (
                                        <tr>
                                            <td colSpan={4} className="empty-row">No active transactions for this facility</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </section>
                </>
            )}

            <style jsx>{`
                .facility-dashboard {
                    padding: 2rem;
                    background: #f8fafc;
                    min-height: 100vh;
                }

                .dashboard-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 2.5rem;
                }
                .dashboard-header h2 { margin: 0; color: #1e293b; font-size: 1.5rem; font-weight: 700; }
                .header-actions { display: flex; align-items: center; gap: 1rem; }
                
                .facility-selector { 
                    padding: 0.625rem 1.25rem; 
                    border: 1px solid #e2e8f0; 
                    border-radius: 8px; 
                    font-size: 0.875rem; 
                    color: #1e293b;
                    background: white;
                    outline: none;
                    min-width: 300px;
                    box-shadow: 0 1px 2px rgba(0,0,0,0.05);
                }

                .warning-badge {
                    background: #fee2e2;
                    color: #991b1b;
                    padding: 0.5rem 1rem;
                    border-radius: 9999px;
                    font-size: 0.75rem;
                    font-weight: 700;
                    letter-spacing: 0.05em;
                }

                .exposure-widget {
                    background: white;
                    border: 1px solid #e2e8f0;
                    border-radius: 16px;
                    padding: 2rem;
                    margin-bottom: 3rem;
                    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
                }
                .widget-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
                .widget-title { font-size: 1.125rem; font-weight: 600; color: #1e293b; }
                .total-limit { font-size: 1rem; color: #64748b; font-weight: 600; }

                .segmented-bar-container { height: 40px; background: #f1f5f9; border-radius: 10px; overflow: hidden; margin-bottom: 2rem; }
                .segmented-bar { display: flex; height: 100%; width: 100%; }
                .segment { 
                    height: 100%; 
                    display: flex; 
                    align-items: center; 
                    justify-content: center; 
                    font-size: 0.7rem; 
                    font-weight: 800; 
                    text-transform: uppercase; 
                    color: white;
                    overflow: hidden;
                    white-space: nowrap;
                    transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
                }
                .segment.firm { background: #1e293b; }
                .segment.contingent { background: #3b82f6; }
                .segment.reserved { background: #94a3b8; }
                .segment.available { background: #f8fafc; color: #94a3b8; border-left: 1px dashed #cbd5e1; }

                .widget-legend { display: flex; gap: 3rem; flex-wrap: wrap; padding: 0 0.5rem; }
                .legend-item { display: flex; align-items: center; gap: 0.75rem; font-size: 0.875rem; color: #475569; font-weight: 600; }
                .dot { width: 10px; height: 10px; border-radius: 50%; }
                .dot.firm { background: #1e293b; }
                .dot.contingent { background: #3b82f6; }
                .dot.reserved { background: #94a3b8; }
                .dot.available { background: #cbd5e1; border: 1px solid #94a3b8; }

                .utilization-section h3 { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin-bottom: 1.5rem; padding-left: 0.5rem; }
                .table-container { background: white; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .breakdown-table { width: 100%; border-collapse: collapse; }
                .breakdown-table th { background: #f8fafc; text-align: left; padding: 1rem 1.5rem; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; color: #64748b; border-bottom: 1px solid #e2e8f0; letter-spacing: 0.05em; }
                .breakdown-table td { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; font-size: 0.875rem; vertical-align: middle; }
                .text-right { text-align: right; }
                .tx-link { color: #2563eb; text-decoration: none; font-weight: 600; }
                .tx-link:hover { text-decoration: underline; }
                .empty-row { text-align: center; color: #94a3b8; padding: 4rem !important; font-style: italic; }
                
                .status-tag { 
                    padding: 0.375rem 0.75rem; 
                    border-radius: 6px; 
                    font-size: 0.65rem; 
                    font-weight: 800; 
                    text-transform: uppercase; 
                    letter-spacing: 0.025em;
                    display: inline-block;
                }
                .status-tag.approved { background: #ecfdf5; color: #059669; border: 1px solid #b7f4d8; }
                .status-tag.pending { background: #fffbeb; color: #d97706; border: 1px solid #fde68a; }
                .status-tag.draft { background: #f1f5f9; color: #64748b; border: 1px solid #e2e8f0; }

                .loading-state, .error-state { 
                    padding: 5rem; 
                    text-align: center; 
                    color: #64748b; 
                    background: white; 
                    border-radius: 12px;
                    margin: 2rem;
                    border: 1px solid #e2e8f0;
                }
            `}</style>
        </div>
    );
};
