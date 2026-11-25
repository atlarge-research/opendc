import { type MantineColorsTuple, createTheme } from "@mantine/core"

export const brandColor = "#00a6d6"

const opendc: MantineColorsTuple = [
    "#e0f7ff",
    "#b8ecfb",
    "#8fdff5",
    "#63d2ef",
    "#43c7ea",
    "#2fbfe8",
    brandColor,
    "#0093bd",
    "#007ea3",
    "#006788",
]

export const logoGradient = "linear-gradient(90deg, #48a1cd, #d1af2e, #df6f20)"

export const theme = createTheme({
    fontFamily: "var(--font-opendc), sans-serif",
    fontFamilyMonospace: "var(--font-opendc-mono), monospace",
    headings: { fontFamily: "var(--font-opendc), sans-serif" },
    primaryColor: "opendc",
    primaryShade: { light: 6, dark: 7 },
    colors: { opendc },
})
