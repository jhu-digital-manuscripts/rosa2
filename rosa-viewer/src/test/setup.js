/**
 * Vitest test setup file.
 */

import { vi, beforeAll, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';

// happy-dom's NodeIterator does not follow the tree when the node it returned
// last is removed, which DOMPurify does when it strips an element. Use a live
// iterator that continues from where the removed node was, as browsers do.
{
  const SHOW = { 1: 0x1, 3: 0x4, 4: 0x8, 7: 0x40, 8: 0x80 };
  const following = (node, root) => {
    if (node.firstChild) return node.firstChild;
    for (let n = node; n && n !== root; n = n.parentNode) {
      if (n.nextSibling) return n.nextSibling;
    }
    return null;
  };
  function createNodeIterator(root, whatToShow = 0xffffffff) {
    let ref = null;
    let refParent = null;
    let refPrev = null;
    const shown = (n) => (whatToShow & (SHOW[n.nodeType] || 0)) !== 0;
    return {
      root,
      nextNode() {
        let next;
        if (ref === null) next = root;
        else if (ref.parentNode === refParent || ref === root) next = following(ref, root);
        else next = refPrev ? refPrev.nextSibling : refParent?.firstChild; // ref was removed
        while (next && !shown(next)) next = following(next, root);
        if (!next) return null;
        ref = next;
        refParent = next.parentNode;
        refPrev = next.previousSibling;
        return next;
      },
    };
  }
  // Install on every prototype that defines it (HTMLDocument overrides Document)
  for (let p = Object.getPrototypeOf(document); p; p = Object.getPrototypeOf(p)) {
    if (Object.prototype.hasOwnProperty.call(p, 'createNodeIterator')) {
      Object.defineProperty(p, 'createNodeIterator', {
        value: createNodeIterator,
        configurable: true,
        writable: true,
      });
    }
  }
}

// Mock fetch globally
global.fetch = vi.fn();

// happy-dom defines the real nodeName and textContent getters on subclasses
// (Element, Text, ...) and leaves Node.prototype's returning '' or null.
// DOMPurify, used to sanitize annotation HTML, reads them through
// Node.prototype as browsers allow, so make the base getters dispatch to the
// most specific one, as they behave in browsers.
for (const name of ['nodeName', 'textContent']) {
  const base = Object.getOwnPropertyDescriptor(Node.prototype, name);
  if (!base?.get) continue;
  Object.defineProperty(Node.prototype, name, {
    ...base,
    get() {
      for (
        let p = Object.getPrototypeOf(this);
        p && p !== Node.prototype;
        p = Object.getPrototypeOf(p)
      ) {
        const own = Object.getOwnPropertyDescriptor(p, name);
        if (own?.get) return own.get.call(this);
      }
      return base.get.call(this);
    },
  });
}

// Reset mocks after each test
afterEach(() => {
  vi.clearAllMocks();
});

// Set up test environment
beforeAll(() => {
  // Mock window.matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
});
