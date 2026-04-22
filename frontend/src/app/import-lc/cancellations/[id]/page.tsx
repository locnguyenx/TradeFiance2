'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { CancellationRequest } from '@/components/CancellationRequest';
import { GlobalShell } from '@/components/GlobalShell';

// ABOUTME: LC Cancellation Page linking the request component to the application shell.

export default function CancellationPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <GlobalShell>
            <main className="p-6">
                <header className="mb-8">
                    <h1 className="text-2xl font-bold text-slate-900">LC Cancellation</h1>
                    <p className="text-slate-500">Initiating early closure for Instrument: {id}</p>
                </header>
                <CancellationRequest instrumentId={id} />
            </main>
        </GlobalShell>
    );
}
