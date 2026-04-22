import React from 'react';
import { PresentationLodgement } from '../../../components/PresentationLodgement';

export default async function PresentationsPage({ searchParams }: any) {
    const params = await searchParams;
    const id = params.id || 'IMP-2026-001';
    
    return (
        <div style={{ padding: '2rem' }}>
            <h1>Log LC Presentation</h1>
            <PresentationLodgement instrumentId={id} />
        </div>
    );
}
