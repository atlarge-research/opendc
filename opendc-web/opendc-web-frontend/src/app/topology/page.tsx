"use client"

import { AppShell } from "@/components/layout/AppShell"
import { TopologyView } from "@/components/topology/TopologyView"
import { PageGhost } from "@/components/util/Ghost"
import { Suspense } from "react"

export default function TopologyPage() {
    return (
        <AppShell>
            <Suspense fallback={<PageGhost />}>
                <TopologyView />
            </Suspense>
        </AppShell>
    )
}
