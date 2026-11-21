const DEPTH = 50

export interface History<T> {
    past: T[]
    present: T
    future: T[]
}

export function initialHistory<T>(present: T): History<T> {
    return { past: [], present, future: [] }
}

export function record<T>(history: History<T>, present: T): History<T> {
    if (present === history.present) return history
    return { past: [...history.past, history.present].slice(-DEPTH), present, future: [] }
}

export function undo<T>(history: History<T>): History<T> {
    const previous = history.past.at(-1)
    if (previous === undefined) return history
    return {
        past: history.past.slice(0, -1),
        present: previous,
        future: [history.present, ...history.future].slice(0, DEPTH),
    }
}

export function redo<T>(history: History<T>): History<T> {
    const [next, ...rest] = history.future
    if (next === undefined) return history
    return { past: [...history.past, history.present].slice(-DEPTH), present: next, future: rest }
}

export function canUndo<T>(history: History<T>): boolean {
    return history.past.length > 0
}

export function canRedo<T>(history: History<T>): boolean {
    return history.future.length > 0
}
