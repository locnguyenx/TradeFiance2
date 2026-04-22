import React from 'react';

// ABOUTME: LimitsDashboard component provides a real-time view of credit facility availability and utilization.
// ABOUTME: Maps directly to the Credit Facility & Limit Dashboard requirement (REQ-UI-CMN-04).

export const LimitsDashboard: React.FC = () => {
    return (
        <div className="limits-dashboard">
            <header className="limits-header">
                <h2>Credit Facility & Limit Dashboard</h2>
            </header>
            <div className="limit-summary-grid">
                <div className="kpi-card">
                    <h3>Total Credit Facility Limit</h3>
                    <p className="amount">$10,000,000</p>
                </div>
                <div className="kpi-card highlight">
                    <h3>Available Balance</h3>
                    <p className="amount">$6,500,000</p>
                </div>
                <div className="kpi-card warning">
                    <h3>Utilized Amount</h3>
                    <p className="amount">$3,500,000</p>
                </div>
            </div>
            <div className="facility-details">
                <h3>Facility ID: FAC-IMP-100</h3>
                <p>Status: Active</p>
                <progress value="35" max="100"></progress>
                <p>Utilization: 35%</p>
            </div>
        </div>
    );
};
