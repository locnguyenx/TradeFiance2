'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { ShippingGuaranteeForm } from '@/components/ShippingGuaranteeForm';
import { GlobalShell } from '@/components/GlobalShell';

// ABOUTME: Shipping Guarantee Page linking the form component to the application shell.

export default function ShippingGuaranteePage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <GlobalShell>
            <main className="p-6">
                <header className="mb-8">
                    <h1 className="text-2xl font-bold text-slate-900">Request Shipping Guarantee</h1>
                    <p className="text-slate-500">Generating indemnity for Instrument: {id}</p>
                </header>
                <ShippingGuaranteeForm instrumentId={id} />
            </main>
        </GlobalShell>
    );
}
