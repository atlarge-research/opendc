"use client"

import { useEffect, useState } from "react"

export interface HardwareIcons {
    cpu?: HTMLImageElement
    gpu?: HTMLImageElement
    memory?: HTMLImageElement
    energy?: HTMLImageElement
    space?: HTMLImageElement
}

const SOURCES: Record<keyof HardwareIcons, string> = {
    cpu: "/img/topology/cpu-icon.png",
    gpu: "/img/topology/gpu-icon.png",
    memory: "/img/topology/memory-icon.png",
    energy: "/img/topology/rack-energy-icon.png",
    space: "/img/topology/rack-space-icon.png",
}

export function useHardwareIcons(): HardwareIcons {
    const [icons, setIcons] = useState<HardwareIcons>({})

    useEffect(() => {
        let live = true
        const elements = Object.entries(SOURCES).map(([name, src]) => {
            const element = new window.Image()
            element.src = src
            element.addEventListener("load", () => {
                if (live) setIcons((current) => ({ ...current, [name]: element }))
            })
            return element
        })
        return () => {
            live = false
            for (const element of elements) element.removeAttribute("src")
        }
    }, [])

    return icons
}
