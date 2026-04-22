import React from 'react';

// ABOUTME: ImportLcDashboard component provides a high-level overview of active Import LCs and key operational metrics.
// ABOUTME: Includes KPI cards for urgent tasks and a filtered transaction data table.

export const ImportLcDashboard: React.FC = () => {
    return (
        <div className="import-lc-dashboard">
            <div className="kpi-widgets">
                <div className="kpi-card">Drafts Awaiting My Submission: 5</div>
                <div className="kpi-card urgent">LCs Expiring within 7 Days: 2</div>
                <div className="kpi-card warning">Discrepant Presentations Awaiting Waiver: 1</div>
            </div>
            <div className="transaction-table-container">
                <h2>Active Transaction Data Table</h2>
                <div className="filters">
                    <select aria-label="Status Filter">
                        <option>Status: Draft, Issued, Docs</option>
                    </select>
                </div>
                <table>
                    <thead>
                        <tr><th>Ref No</th><th>Applicant</th><th>Amount</th><th>Status</th><th>SLA Timer</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>TF-IMP-001</td><td>Acme Corp</td><td>$500,000</td><td>Issued</td><td>N/A</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    );
};
