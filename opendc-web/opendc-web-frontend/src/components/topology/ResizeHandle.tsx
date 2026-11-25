"use client"

import { Box } from "@mantine/core"
import { useCallback, useEffect, useRef, useState } from "react"

const KEYBOARD_STEP = 24

export interface Resizer {
    width: number
    min: number
    max: number
    start: () => void
    nudge: (delta: number) => void
}

export function useResizableWidth(initial: number, min: number, max: number): Resizer {
    const [width, setWidth] = useState(initial)
    const dragging = useRef(false)

    const start = useCallback(() => {
        dragging.current = true
        document.body.style.cursor = "col-resize"
        document.body.style.userSelect = "none"
    }, [])

    const nudge = useCallback(
        (delta: number) => setWidth((current) => Math.min(max, Math.max(min, current + delta))),
        [min, max],
    )

    useEffect(() => {
        const move = (event: PointerEvent) => {
            if (!dragging.current) return
            setWidth(Math.min(max, Math.max(min, window.innerWidth - event.clientX)))
        }
        const stop = () => {
            if (!dragging.current) return
            dragging.current = false
            document.body.style.cursor = ""
            document.body.style.userSelect = ""
        }
        window.addEventListener("pointermove", move)
        window.addEventListener("pointerup", stop)
        return () => {
            window.removeEventListener("pointermove", move)
            window.removeEventListener("pointerup", stop)
        }
    }, [min, max])

    return { width, min, max, start, nudge }
}

export function ResizeHandle({ resizer, label }: { resizer: Resizer; label: string }) {
    const [active, setActive] = useState(false)

    return (
        <Box
            // biome-ignore lint/a11y/useSemanticElements: an <hr> is a thematic break; this is the
            // ARIA window-splitter pattern, which must stay focusable and carry aria-value*.
            role="separator"
            aria-label={label}
            aria-orientation="vertical"
            aria-valuenow={Math.round(resizer.width)}
            aria-valuemin={resizer.min}
            aria-valuemax={resizer.max}
            tabIndex={0}
            onPointerDown={(event) => {
                event.preventDefault()
                resizer.start()
            }}
            onKeyDown={(event) => {
                if (event.key === "ArrowLeft") resizer.nudge(KEYBOARD_STEP)
                if (event.key === "ArrowRight") resizer.nudge(-KEYBOARD_STEP)
            }}
            onMouseEnter={() => setActive(true)}
            onMouseLeave={() => setActive(false)}
            onFocus={() => setActive(true)}
            onBlur={() => setActive(false)}
            w="var(--mantine-spacing-xs)"
            style={{
                cursor: "col-resize",
                flexShrink: 0,
                alignSelf: "stretch",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
            }}
        >
            <Box
                w={3}
                h={32}
                bg={active ? "var(--mantine-color-opendc-6)" : "var(--mantine-color-default-border)"}
                style={{ borderRadius: 3, transition: "background-color 120ms ease" }}
            />
        </Box>
    )
}
