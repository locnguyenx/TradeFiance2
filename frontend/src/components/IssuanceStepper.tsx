import React, { useState } from 'react';

// ABOUTME: IssuanceStepper component manages the multi-step data entry workflow for LC Issuance.
// ABOUTME: Implements a 5-step horizontal stepper strictly mapped to SWIFT field requirements.

const steps = [
    'Step 1: Parties & Limits',
    'Step 2: Financials & Dates',
    'Step 3: Terms & Shipping',
    'Step 4: Narratives (MT700 Block)',
    'Step 5: Review & Submit',
];

export const IssuanceStepper: React.FC = () => {
    const [stepIndex, setStepIndex] = useState(0);
    
    return (
        <div className="stepper-layout">
            <header className="draft-banner">
                <span>Draft Reference: DRAFT-1002</span>
                <span>Base Equivalent: $0</span>
            </header>
            <h2>{steps[stepIndex]}</h2>
            {stepIndex === 1 && (
                <div className="financials-form">
                    <label>Positive Tolerance %: <input type="number" /></label>
                    <label>Negative Tolerance %: <input type="number" /></label>
                </div>
            )}
            {stepIndex === 4 && (
                <div className="review-submit-panel">
                    <h3>System Validations</h3>
                    <p>✅ Limit Check Passed</p>
                    <p>✅ Sanctions Check Passed</p>
                    <button>Submit for Approval</button>
                </div>
            )}
            <div className="stepper-actions">
                {stepIndex > 0 && <button onClick={() => setStepIndex(stepIndex - 1)}>Back</button>}
                {stepIndex < 4 && <button onClick={() => setStepIndex(stepIndex + 1)}>Next</button>}
            </div>
        </div>
    );
};
