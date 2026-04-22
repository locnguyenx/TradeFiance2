'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { PresentationLodgement } from '@/components/PresentationLodgement';
import { GlobalShell } from '@/components/GlobalShell';

// ABOUTME: Document Presentation Page linking the lodgement component to the application shell.

export default function PresentationPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <GlobalShell>
            <main className="p-6">
                <header className="mb-8">
                    <h1 className="text-2xl font-bold text-slate-900">Log Document Presentation</h1>
                    <p className="text-slate-500">Recording arrival of documents for Instrument: {id}</p>
                </header>
                <PresentationLodgement instrumentId={id} />
            </main>
        </GlobalShell>
    );
}
