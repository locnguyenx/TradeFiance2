'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { DocumentExamination } from '@/components/DocumentExamination';

// ABOUTME: Document Examination Page linking the component to the application shell.

export default function LcPresentationPage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <main className="p-6">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900">Document Examination</h1>
                <p className="text-slate-500">Screening presentation for Instrument: {id}</p>
            </header>
            <DocumentExamination instrument={{ instrumentId: id }} />
        </main>
    );
}
