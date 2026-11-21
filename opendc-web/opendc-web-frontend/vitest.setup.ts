import "@testing-library/jest-dom/vitest"

const noop = () => {}

Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addEventListener: noop,
        removeEventListener: noop,
        addListener: noop,
        removeListener: noop,
        dispatchEvent: () => false,
    }),
})

class ResizeObserverMock {
    observe = noop
    unobserve = noop
    disconnect = noop
}

window.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver
window.scrollTo = noop as typeof window.scrollTo
