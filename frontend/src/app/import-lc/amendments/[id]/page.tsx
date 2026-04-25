'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { AmendmentStepper } from '@/components/AmendmentStepper';

// ABOUTME: LC Amendment Page linking the stepper to the application shell.

export default function LcAmendmentPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <main className="p-6">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900">Issue LC Amendment</h1>
                <p className="text-slate-500">Processing modifications for Instrument: {id}</p>
            </header>
            <AmendmentStepper lcId={id} />
        </main>
    );
}
