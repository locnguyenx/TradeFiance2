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

// Fail tests on console.error to ensure "clean at capture" and "tests catch errors"
const originalConsoleError = console.error;
console.error = (message: any, ...args: any[]) => {
  // If you expect a console.error in a test, use jest.spyOn(console, 'error').mockImplementation(() => {})
  originalConsoleError(message, ...args);
  throw new Error(`Test failed due to unexpected console.error: ${message} ${args.join(' ')}`);
};
