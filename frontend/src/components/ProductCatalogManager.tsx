'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeProductCatalog } from '../api/types';

// ABOUTME: ProductCatalogManager implements REQ-UI-CMN-06.
// ABOUTME: Master-Detail layout for managing Trade Finance product configurations.

export const ProductCatalogManager: React.FC = () => {
    const [products, setProducts] = useState<TradeProductCatalog[]>([]);
    const [selectedProduct, setSelectedProduct] = useState<TradeProductCatalog | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        loadProducts();
    }, []);

    const loadProducts = async () => {
        try {
            const data = await tradeApi.getProductCatalog();
            const productList = data?.productList || [];
            setProducts(productList);
            if (productList.length > 0 && !selectedProduct) {
                setSelectedProduct(productList[0]);
            }
        } catch (err) {
            setError('Failed to load product catalog');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectProduct = (product: TradeProductCatalog) => {
        setSelectedProduct({ ...product });
    };

    const handleFieldChange = (field: keyof TradeProductCatalog, value: any) => {
        if (!selectedProduct) return;
        setSelectedProduct({
            ...selectedProduct,
            [field]: value
        });
    };

    const handleSave = async (isPublish: boolean) => {
        if (!selectedProduct) return;
        setSaving(true);
        try {
            await tradeApi.updateProductCatalog(selectedProduct.productId, selectedProduct);
            await loadProducts();
        } catch (err) {
            setError('Failed to save configuration');
        } finally {
            setSaving(false);
        }
    };

    if (loading && (products?.length || 0) === 0) return <div className="admin-loading">Loading configuration...</div>;

    return (
        <div className="product-manager-layout">
            <aside className="product-list-pane">
                <header className="pane-header">
                    <h3>Products</h3>
                </header>
                <div className="product-items">
                    {(products || []).map((p, index) => (
                        <div 
                            key={p.productId || `product-${index}`} 
                            className={`product-item ${selectedProduct?.productId === p.productId ? 'active' : ''}`}
                            onClick={() => handleSelectProduct(p)}
                        >
                            <span className="product-name">{p.productName}</span>
                            <span className="product-id">{p.productId}</span>
                        </div>
                    ))}
                </div>
            </aside>

            <main className="product-detail-pane">
                {selectedProduct ? (
                    <>
                        <header className="pane-header detail-header">
                            <h2>{selectedProduct.productName} Configuration</h2>
                            <div className="action-bar">
                                <button 
                                    className="secondary-btn" 
                                    onClick={() => handleSave(false)}
                                    disabled={saving}
                                >
                                    {saving ? 'Saving...' : 'Save Draft'}
                                </button>
                                <button 
                                    className="primary-btn" 
                                    onClick={() => handleSave(true)}
                                    disabled={saving}
                                >
                                    Publish New Product
                                </button>
                            </div>
                        </header>

                        <div className="config-form">
                            <section className="form-section">
                                <h3>Basic Rules</h3>
                                <div className="field-group">
                                    <div className="field">
                                        <label htmlFor="documentExamSlaDays">Document Exam SLA Days</label>
                                        <input 
                                            id="documentExamSlaDays"
                                            type="number" 
                                            value={selectedProduct.documentExamSlaDays}
                                            onChange={(e) => handleFieldChange('documentExamSlaDays', parseInt(e.target.value))}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="mandatoryMarginPercent">Mandatory Margin %</label>
                                        <input 
                                            id="mandatoryMarginPercent"
                                            type="number" 
                                            value={selectedProduct.mandatoryMarginPercent}
                                            onChange={(e) => handleFieldChange('mandatoryMarginPercent', parseFloat(e.target.value))}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="maxToleranceLimit">Max Tolerance Limit %</label>
                                        <input 
                                            id="maxToleranceLimit"
                                            type="number" 
                                            value={selectedProduct.maxToleranceLimit}
                                            onChange={(e) => handleFieldChange('maxToleranceLimit', parseFloat(e.target.value))}
                                        />
                                    </div>
                                </div>
                            </section>

                            <section className="form-section">
                                <h3>Product Toggles</h3>
                                <div className="toggle-grid">
                                    <div className="toggle-field">
                                        <input 
                                            type="checkbox" 
                                            id="allowRevolving"
                                            checked={selectedProduct.allowRevolving === 'Y'}
                                            onChange={(e) => handleFieldChange('allowRevolving', e.target.checked ? 'Y' : 'N')}
                                        />
                                        <label htmlFor="allowRevolving">Allow Revolving LCs</label>
                                    </div>
                                    <div className="toggle-field">
                                        <input 
                                            type="checkbox" 
                                            id="allowAdvancePayment"
                                            checked={selectedProduct.allowAdvancePayment === 'Y'}
                                            onChange={(e) => handleFieldChange('allowAdvancePayment', e.target.checked ? 'Y' : 'N')}
                                        />
                                        <label htmlFor="allowAdvancePayment">Allow Advance Payment</label>
                                    </div>
                                    <div className="toggle-field">
                                        <input 
                                            type="checkbox" 
                                            id="isStandby"
                                            checked={selectedProduct.isStandby === 'Y'}
                                            onChange={(e) => handleFieldChange('isStandby', e.target.checked ? 'Y' : 'N')}
                                        />
                                        <label htmlFor="isStandby">Is Standby LC</label>
                                    </div>
                                    <div className="toggle-field">
                                        <input 
                                            type="checkbox" 
                                            id="isTransferable"
                                            checked={selectedProduct.isTransferable === 'Y'}
                                            onChange={(e) => handleFieldChange('isTransferable', e.target.checked ? 'Y' : 'N')}
                                        />
                                        <label htmlFor="isTransferable">Is Transferable LC</label>
                                    </div>
                                </div>
                            </section>
                        </div>
                    </>
                ) : (
                    <div className="empty-selection">Select a product to configure</div>
                )}
            </main>

            <style jsx>{`
                .product-manager-layout { display: grid; grid-template-columns: 320px 1fr; background: white; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; min-height: 700px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                .pane-header { padding: 1.5rem; border-bottom: 1px solid #f1f5f9; background: #f8fafc; }
                .pane-header h3, .pane-header h2 { margin: 0; color: #1e293b; font-size: 1.125rem; }
                .product-list-pane { border-right: 1px solid #f1f5f9; background: #f8fafc; }
                .product-items { padding: 1rem; display: flex; flex-direction: column; gap: 0.5rem; }
                .product-item { padding: 1rem; border-radius: 8px; cursor: pointer; display: flex; flex-direction: column; gap: 0.25rem; transition: all 0.2s; border: 1px solid transparent; }
                .product-item:hover { background: #f1f5f9; }
                .product-item.active { background: white; border-color: #2563eb; box-shadow: 0 1px 3px rgba(37, 99, 235, 0.1); }
                .product-name { font-weight: 600; color: #1e293b; }
                .product-id { font-size: 0.75rem; color: #64748b; }
                .product-detail-pane { background: white; display: flex; flex-direction: column; }
                .detail-header { display: flex; justify-content: space-between; align-items: center; background: white; }
                .action-bar { display: flex; gap: 1rem; }
                .config-form { padding: 2rem; display: flex; flex-direction: column; gap: 2.5rem; overflow-y: auto; }
                .form-section h3 { margin: 0 0 1.25rem 0; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; border-bottom: 1px solid #f1f5f9; padding-bottom: 0.5rem; }
                .field-group { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 2rem; }
                .field { display: flex; flex-direction: column; gap: 0.5rem; }
                .field label { font-size: 0.875rem; font-weight: 600; color: #475569; }
                .field input { padding: 0.625rem; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.875rem; outline: none; transition: border-color 0.2s; }
                .field input:focus { border-color: #2563eb; }
                .toggle-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; }
                .toggle-field { display: flex; align-items: center; gap: 1rem; padding: 1rem; background: #f8fafc; border-radius: 8px; border: 1px solid #f1f5f9; }
                .toggle-field label { font-weight: 500; color: #334155; cursor: pointer; }
                .toggle-field input[type="checkbox"] { width: 1.25rem; height: 1.25rem; cursor: pointer; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.625rem 1.25rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .primary-btn:hover { background: #1d4ed8; }
                .primary-btn:disabled { background: #94a3b8; cursor: not-allowed; }
                .secondary-btn { background: white; color: #475569; border: 1px solid #e2e8f0; padding: 0.625rem 1.25rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
                .secondary-btn:hover { background: #f8fafc; border-color: #cbd5e1; }
                .empty-selection { display: flex; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-style: italic; }
                .admin-loading { padding: 2rem; text-align: center; color: #64748b; }
            `}</style>
        </div>
    );
};
