import '@testing-library/jest-dom';

// Mock window.scrollTo
Object.defineProperty(window, 'scrollTo', {
  value: () => {},
  writable: true,
});

// Mock HTMLElement.prototype.scrollBy
Object.defineProperty(HTMLElement.prototype, 'scrollBy', {
  value: () => {},
  writable: true,
});
