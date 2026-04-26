import '@testing-library/jest-dom';
import fetch from 'cross-fetch';

global.fetch = fetch;

// Mock IntersectionObserver for InstrumentDetails component
class IntersectionObserverMock {
  root = null;
  rootMargin = "";
  thresholds = [];
  disconnect() {}
  observe() {}
  unobserve() {}
  takeRecords() { return []; }
}
global.IntersectionObserver = IntersectionObserverMock as any;

// Mock next/navigation for ImportLcDashboard and other components
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    prefetch: jest.fn(),
    back: jest.fn(),
  }),
  usePathname: () => '/',
  useSearchParams: () => new URLSearchParams(),
}));
