import React from 'react';

// ABOUTME: DocumentExamination component provides a split-screen interface for Import LC document checking.
// ABOUTME: Left pane displays digital documents, Right pane provides a discrepancy reporting checklist (REQ-UI-IMP-04).

export const DocumentExamination: React.FC = () => {
    return (
        <div className="document-exam-layout">
            <aside className="document-viewer">
                <h2>Digital Document Viewer</h2>
                <div className="pdf-stub"> [PDF Render: Commercial Invoice - INV-2026-001] </div>
            </aside>
            <main className="discrepancy-pane">
                <h2>Discrepancy Reporting Checklist</h2>
                <ul>
                    <li><input type="checkbox" /> MT700 Logic Check: Late Shipment</li>
                    <li><input type="checkbox" /> Document mismatch: Port of Loading</li>
                </ul>
                <div className="checker-actions">
                    <button>Raise Discrepancy</button>
                    <button className="primary">Approve Clean Docs</button>
                </div>
            </main>
        </div>
    );
};
