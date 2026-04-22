import React from 'react';
import { SystemAdminSettings } from '../../../components/SystemAdminSettings';

export default async function AdminLogsPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <SystemAdminSettings activePanel="audit" />
    </div>
  );
}
