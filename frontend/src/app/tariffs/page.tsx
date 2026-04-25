'use client';

import { TariffManager } from '../../components/TariffManager';

export default function TariffsPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Tariff & Fee Configuration</h1>
      <TariffManager />
    </div>
  );
}
