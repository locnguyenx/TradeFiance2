import React from 'react';
import { SystemAdminSettings } from '../../../components/SystemAdminSettings';

export default async function AdminTiersPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <SystemAdminSettings activePanel="authority" />
    </div>
  );
}
