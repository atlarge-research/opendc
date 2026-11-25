"use client"

import { useComputedColorScheme, useMantineTheme } from "@mantine/core"

export interface CanvasPalette {
    surface: string
    grid: string
    cluster: string
    clusterSelected: string
    title: string
    divider: string
    border: string
    borderSelected: string
    accent: string
    invalidBorder: string
    label: string
    muted: string
    hover: string
    meterTrack: string
    energyFill: string
    meterOver: string
}

export function useCanvasPalette(): CanvasPalette {
    const theme = useMantineTheme()
    const scheme = useComputedColorScheme("light")
    const dark = scheme === "dark"
    const shade = (color: keyof typeof theme.colors, index: number) => theme.colors[color]?.[index] ?? "#888888"

    return {
        surface: dark ? shade("dark", 8) : shade("gray", 0),
        grid: dark ? shade("dark", 5) : shade("gray", 3),
        cluster: dark ? shade("dark", 6) : theme.white,
        clusterSelected: dark ? shade("dark", 5) : shade("opendc", 0),
        title: dark ? shade("gray", 0) : shade("dark", 9),
        divider: dark ? shade("dark", 4) : shade("gray", 2),
        border: dark ? shade("dark", 4) : shade("gray", 3),
        borderSelected: shade("opendc", 6),
        accent: shade("opendc", dark ? 4 : 7),
        invalidBorder: shade("red", 6),
        label: dark ? shade("gray", 2) : shade("dark", 6),
        muted: dark ? shade("dark", 1) : shade("gray", 6),
        hover: shade("opendc", 5),
        meterTrack: dark ? shade("dark", 4) : shade("gray", 2),
        energyFill: shade("yellow", dark ? 5 : 6),
        meterOver: shade("red", 6),
    }
}
