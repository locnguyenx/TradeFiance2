import React from 'react';
import { SettlementInitiation } from '../../../components/SettlementInitiation';

export default async function SettlementPage({ searchParams }: any) {
    const params = await searchParams;
    const id = params.id || 'IMP-2026-001';
    
    return (
        <div style={{ padding: '2rem' }}>
            <h1>Initiate LC Settlement</h1>
            <SettlementInitiation instrumentId={id} />
        </div>
    );
}
