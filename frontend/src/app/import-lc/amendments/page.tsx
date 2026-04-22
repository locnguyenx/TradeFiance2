import React from 'react';
import { AmendmentStepper } from '../../../components/AmendmentStepper';

export default async function AmendmentPage({ searchParams }: any) {
  const params = await searchParams;
  const id = params.id || 'NEW';
  
  return (
    <div style={{ padding: '2rem' }}>
      <h1>Issue LC Amendment</h1>
      <AmendmentStepper lcId={id} />
    </div>
  );
}
