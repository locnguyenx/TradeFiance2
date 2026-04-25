'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { ShippingGuaranteeForm } from '@/components/ShippingGuaranteeForm';

// ABOUTME: Shipping Guarantee Page linking the form to the application shell.

export default function LcShippingGuaranteePage() {
    const params = useParams();
    const id = params.id as string;

    return (
        <main className="p-6">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900">Issue Shipping Guarantee</h1>
                <p className="text-slate-500">Requesting port release for Instrument: {id}</p>
            </header>
            <ShippingGuaranteeForm instrumentId={id} />
        </main>
    );
}
