'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: FacilityDashboard implements exposure visualization for credit limits.
// ABOUTME: Provides real-time utilization tracking and risk warnings for v3.0.

interface Facility {
    facilityId: string;
    facilityName: string;
    limit: number;
    exposure: number;
}

interface ExposureData {
    totalLimit: number;
    totalExposure: number;
    utilizationPercent: number;
    facilityBreakdown: Facility[];
}

export const FacilityDashboard: React.FC = () => {
    const [data, setData] = useState<ExposureData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadExposure = async () => {
            try {
                const exposure = await tradeApi.getExposureData();
                setData(exposure);
            } catch (err) {
                setError('Failed to load exposure data');
            } finally {
                setLoading(false);
            }
        };
        loadExposure();
    }, []);

    if (loading) return <div className="loading-state">Calculating exposure...</div>;
    if (!data) return <div className="error-state">Error: {error}</div>;

    const isHighUtilization = data.utilizationPercent >= 90;

    return (
        <div className="facility-dashboard">
            <header className="dashboard-header">
                <h2>Exposure & Credit Facilities</h2>
                {isHighUtilization && (
                    <div className="warning-badge">HIGH UTILIZATION</div>
                )}
            </header>

            <div className="summary-cards">
                <div className="summary-card">
                    <span className="card-label">Total Outstanding Exposure</span>
                    <span className="card-value">${data.totalExposure.toLocaleString()}</span>
                </div>
                <div className="summary-card">
                    <span className="card-label">Total Approved Limits</span>
                    <span className="card-value">${data.totalLimit.toLocaleString()}</span>
                </div>
                <div className="summary-card">
                    <span className="card-label">Overall Utilization</span>
                    <div className="utilization-group">
                        <span className="card-value">{data.utilizationPercent}%</span>
                        <div className="progress-bar-bg">
                            <div 
                                className="progress-bar-fill" 
                                style={{ 
                                    width: `${data.utilizationPercent}%`,
                                    background: isHighUtilization ? '#ef4444' : '#2563eb'
                                }} 
                            />
                        </div>
                    </div>
                </div>
            </div>

            <section className="facility-list">
                <h3>Facility Breakdown</h3>
                <div className="facility-grid">
                    {data.facilityBreakdown.map(f => {
                        const util = (f.exposure / f.limit) * 100;
                        return (
                            <div key={f.facilityId} className="facility-box">
                                <div className="facility-info">
                                    <span className="facility-name">{f.facilityName}</span>
                                    <span className="facility-id">{f.facilityId}</span>
                                </div>
                                <div className="facility-metrics">
                                    <div className="metric">
                                        <span className="m-label">Limit</span>
                                        <span className="m-value">${f.limit.toLocaleString()}</span>
                                    </div>
                                    <div className="metric">
                                        <span className="m-label">Exposure</span>
                                        <span className="m-value">${f.exposure.toLocaleString()}</span>
                                    </div>
                                </div>
                                <div className="facility-progress">
                                    <div className="progress-bar-bg small">
                                        <div 
                                            className="progress-bar-fill" 
                                            style={{ width: `${util}%`, background: util > 90 ? '#ef4444' : '#64748b' }} 
                                        />
                                    </div>
                                    <span className="util-text">{util.toFixed(1)}%</span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </section>

            <style jsx>{`
                .facility-dashboard {
                    padding: 1.5rem;
                    display: flex;
                    flex-direction: column;
                    gap: 2rem;
                    background: #f8fafc;
                    border-radius: 12px;
                }

                .dashboard-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .dashboard-header h2 { margin: 0; color: #1e293b; font-size: 1.25rem; }

                .warning-badge {
                    background: #fee2e2;
                    color: #991b1b;
                    padding: 0.375rem 0.75rem;
                    border-radius: 9999px;
                    font-size: 0.75rem;
                    font-weight: 700;
                    letter-spacing: 0.05em;
                }

                .summary-cards {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: 1.5rem;
                }

                .summary-card {
                    background: white;
                    padding: 1.5rem;
                    border-radius: 12px;
                    border: 1px solid #e2e8f0;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
                }

                .card-label { font-size: 0.875rem; color: #64748b; font-weight: 500; }
                .card-value { font-size: 1.5rem; font-weight: 800; color: #1e293b; }

                .utilization-group { display: flex; flex-direction: column; gap: 0.5rem; }

                .progress-bar-bg {
                    height: 8px;
                    background: #f1f5f9;
                    border-radius: 4px;
                    overflow: hidden;
                }

                .progress-bar-fill {
                    height: 100%;
                    transition: width 0.5s ease-out;
                }

                .facility-list h3 { font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; margin-bottom: 1.25rem; }

                .facility-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }

                .facility-box {
                    background: white;
                    padding: 1.25rem;
                    border-radius: 10px;
                    border: 1px solid #e2e8f0;
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                }

                .facility-info { display: flex; flex-direction: column; }
                .facility-name { font-weight: 600; color: #334155; }
                .facility-id { font-size: 0.75rem; color: #94a3b8; }

                .facility-metrics { display: flex; justify-content: space-between; border-top: 1px solid #f1f5f9; padding-top: 1rem; }
                .metric { display: flex; flex-direction: column; gap: 0.25rem; }
                .m-label { font-size: 0.75rem; color: #64748b; }
                .m-value { font-weight: 700; color: #1e293b; font-size: 0.875rem; }

                .facility-progress { display: flex; align-items: center; gap: 1rem; }
                .progress-bar-bg.small { flex: 1; height: 6px; }
                .util-text { font-size: 0.75rem; font-weight: 700; color: #64748b; min-width: 40px; }

                .loading-state, .error-state { padding: 3rem; text-align: center; color: #64748b; }
            `}</style>
        </div>
    );
};
