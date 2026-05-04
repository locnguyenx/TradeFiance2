import React from 'react';
import { ShippingGuaranteeForm } from '../../../components/ShippingGuaranteeForm';

export default async function ShippingGuaranteesPage({ searchParams }: any) {
    const params = await searchParams;
    const id = params.id || '';
    
    return (
        <div style={{ padding: '2rem' }}>

            <ShippingGuaranteeForm instrumentId={id} />
        </div>
    );
}
