'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { CancellationRequest } from '@/components/CancellationRequest';

// ABOUTME: LC Cancellation Page linking the request component to the application shell.

export default function LcCancellationPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <main className="p-6">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900">LC Cancellation</h1>
                <p className="text-slate-500">Initiating early closure for Instrument: {id}</p>
            </header>
            <CancellationRequest instrumentId={id} />
        </main>
    );
}
