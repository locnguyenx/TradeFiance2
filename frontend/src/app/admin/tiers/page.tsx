'use client';

import { UserAuthorityManager } from '../../../components/UserAuthorityManager';

export default function TiersPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">User Authority Management</h1>
      <UserAuthorityManager />
    </div>
  );
}
