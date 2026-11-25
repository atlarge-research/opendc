import "@mantine/core/styles.css"
import "@mantine/notifications/styles.css"
import "@mantine/charts/styles.css"

import { brandColor } from "@/theme/theme"
import { ColorSchemeScript, mantineHtmlProps } from "@mantine/core"
import type { Metadata, Viewport } from "next"
import { Geist_Mono, Instrument_Sans } from "next/font/google"
import type { ReactNode } from "react"
import { Providers } from "./providers"

const openDcFont = Instrument_Sans({
    subsets: ["latin"],
    variable: "--font-opendc",
})

const openDcFontMono = Geist_Mono({
    subsets: ["latin"],
    variable: "--font-opendc-mono",
})

export const metadata: Metadata = {
    title: "OpenDC",
    description: "Collaborative Datacenter Simulation and Exploration for Everybody",
    icons: { icon: "/favicon.ico" },
}

export const viewport: Viewport = {
    themeColor: brandColor,
}

export default function RootLayout({ children }: { children: ReactNode }) {
    return (
        <html lang="en" className={`${openDcFont.variable} ${openDcFontMono.variable}`} {...mantineHtmlProps}>
            <head>
                <ColorSchemeScript defaultColorScheme="auto" />
            </head>
            <body>
                <Providers>{children}</Providers>
            </body>
        </html>
    )
}
