'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { SettlementInitiation } from '@/components/SettlementInitiation';

// ABOUTME: Settlement Initiation Page linking the component to the application shell.

export default function LcSettlementPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <main className="p-6">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900">Initiate Settlement</h1>
                <p className="text-slate-500">Processing final payment for Instrument: {id}</p>
            </header>
            <SettlementInitiation instrumentId={id} />
        </main>
    );
}
