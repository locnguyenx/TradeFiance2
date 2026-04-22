'use client';

import React from 'react';
import { SystemAdminSettings } from '../../../components/SystemAdminSettings';

export default function AdminProductPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <SystemAdminSettings activePanel="product" />
    </div>
  );
}
