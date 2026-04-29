'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { ProductCatalogManager } from './ProductCatalogManager';
import { TariffManager } from './TariffManager';
import { UserAuthorityManager } from './UserAuthorityManager';
import { GlobalTransactionLog } from './GlobalTransactionLog';

// ABOUTME: SystemAdminSettings component provides configuration panels for user authorities, system audits, and product matrices.
// ABOUTME: Centralizes key governance settings for the Trade Finance platform.

interface AdminSettingsProps {
    activePanel?: 'authority' | 'audit' | 'product' | 'tariff' | 'user';
}

export const SystemAdminSettings: React.FC<AdminSettingsProps> = ({ activePanel }) => {
    const showAll = !activePanel;

    const handleConfigSave = async (key: string, value: string) => {
        await tradeApi.updateProductConfig(key, value);
    };

    return (
        <div className="system-admin-container">
            {(showAll || activePanel === 'authority') && (
                <section className="admin-panel premium-card">
                    <header className="panel-header">
                        <h2>User Authority Management</h2>
                        <button className="primary-btn">Add New Tier</button>
                    </header>
                    <div className="table-wrapper">
                        <table className="admin-table">
                            <thead>
                                <tr>
                                    <th>Authority Tier</th>
                                    <th>Description</th>
                                    <th>Limit Threshold</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Tier 1 - Maker</td>
                                    <td>Standard operational data entry</td>
                                    <td>N/A</td>
                                    <td><button className="edit-btn">Edit</button></td>
                                </tr>
                                <tr>
                                    <td>Tier 2 - Checker</td>
                                    <td>Basic authorization authorization</td>
                                    <td>USD 500,000</td>
                                    <td><button className="edit-btn">Edit</button></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            {(showAll || activePanel === 'audit') && (
                <section className="admin-panel premium-card" style={{ padding: '2rem' }}>
                    <GlobalTransactionLog />
                </section>
            )}

            {(showAll || activePanel === 'product') && (
                <section className="admin-panel premium-card">
                    <ProductCatalogManager />
                </section>
            )}

            {(showAll || activePanel === 'tariff') && (
                <section className="admin-panel premium-card">
                    <TariffManager />
                </section>
            )}

            {(showAll || activePanel === 'user') && (
                <section className="admin-panel premium-card">
                    <UserAuthorityManager />
                </section>
            )}

            <style jsx>{`
                .system-admin-container { display: flex; flex-direction: column; gap: 2rem; padding: 1rem; }
                .admin-panel { background: white; overflow: hidden; }
                .panel-header { padding: 1.5rem; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
                .panel-header h2 { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0; }
                
                .table-wrapper { padding: 0 1.5rem 1.5rem 1.5rem; }
                .admin-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
                .admin-table th { text-align: left; padding: 1rem; color: #64748b; font-weight: 600; border-bottom: 2px solid #f1f5f9; }
                .admin-table td { padding: 1rem; border-bottom: 1px solid #f1f5f9; color: #334155; }
                
                .font-mono { font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; color: #6366f1; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .primary-btn:hover { background: #1d4ed8; }
                
                .edit-btn { background: none; border: 1px solid #e2e8f0; color: #64748b; padding: 0.25rem 0.5rem; border-radius: 4px; cursor: pointer; font-size: 0.75rem; transition: all 0.2s; }
                .edit-btn:hover { border-color: #2563eb; color: #2563eb; }
                
                .config-form { padding: 2rem; }
                .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; margin-bottom: 2rem; }
                .form-field label { display: flex; align-items: center; gap: 0.75rem; font-weight: 600; color: #334155; cursor: pointer; }
                .form-field input[type="checkbox"] { width: 1.25rem; height: 1.25rem; }
                .form-actions { display: flex; justify-content: flex-end; }
                
                .admin-input { padding: 0.5rem 1rem; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.875rem; outline: none; width: 300px; }
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            `}</style>
        </div>
    );
};
