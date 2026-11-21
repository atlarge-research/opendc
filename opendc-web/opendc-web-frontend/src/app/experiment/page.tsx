"use client"

import { ExperimentView } from "@/components/experiment/ExperimentView"
import { AppShell } from "@/components/layout/AppShell"
import { PageGhost } from "@/components/util/Ghost"
import { Suspense } from "react"

export default function ExperimentPage() {
    return (
        <AppShell>
            <Suspense fallback={<PageGhost />}>
                <ExperimentView />
            </Suspense>
        </AppShell>
    )
}
