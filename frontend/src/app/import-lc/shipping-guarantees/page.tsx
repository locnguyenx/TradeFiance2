import React from 'react';
import { ShippingGuaranteeForm } from '../../../components/ShippingGuaranteeForm';

export default async function ShippingGuaranteesPage({ searchParams }: any) {
    const params = await searchParams;
    const id = params.id || 'IMP-2026-001';
    
    return (
        <div style={{ padding: '2rem' }}>
            <h1>Issue Shipping Guarantee</h1>
            <ShippingGuaranteeForm instrumentId={id} />
        </div>
    );
}
