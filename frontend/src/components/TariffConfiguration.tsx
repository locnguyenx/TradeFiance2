import React from 'react';

// ABOUTME: TariffConfiguration component manages the fee and commission rules for Trade Finance products.
// ABOUTME: Supports base rule sets and tier/exception override pricing.

export const TariffConfiguration: React.FC = () => {
    return (
        <div className="tariff-config-layout">
            <aside className="tariff-nav">
                <ul>
                    <li>Issuance Commission</li>
                    <li>Amendment Fee</li>
                    <li>SWIFT Cable Charge</li>
                </ul>
            </aside>
            <main className="tariff-main">
                <h2>Issuance Commission</h2>
                <div className="base-rules">
                    <h3>Base Rule Set</h3>
                    <label>Default Rate (%): <input defaultValue="0.125" /></label>
                    <label>Minimum Charge (USD): <input defaultValue="50.00" /></label>
                </div>
                <div className="exceptions-grid">
                    <h3>Exception / Tier Pricing Grid</h3>
                    <table>
                        <thead><tr><th>Customer Tier</th><th>Override Rate</th></tr></thead>
                        <tbody><tr><td>VIP Corporate</td><td>0.100</td></tr></tbody>
                    </table>
                </div>
                <button>Publish New Tariff</button>
            </main>
        </div>
    );
};
