import React from 'react';
import { CancellationRequest } from '../../../components/CancellationRequest';

export default async function CancellationsPage({ searchParams }: any) {
    const params = await searchParams;
    const id = params.id || 'IMP-2026-001';
    
    return (
        <div style={{ padding: '2rem' }}>
            <h1>Request LC Cancellation</h1>
            <CancellationRequest instrumentId={id} />
        </div>
    );
}
