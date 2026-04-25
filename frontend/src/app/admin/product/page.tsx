'use client';

import { ProductCatalogManager } from '../../../components/ProductCatalogManager';

export default function ProductPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Product Configuration Matrix</h1>
      <ProductCatalogManager />
    </div>
  );
}
