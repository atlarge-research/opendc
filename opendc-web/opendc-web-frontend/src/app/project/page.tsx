"use client"

import { AppShell } from "@/components/layout/AppShell"
import { ProjectView } from "@/components/project/ProjectView"
import { PageGhost } from "@/components/util/Ghost"
import { Suspense } from "react"

export default function ProjectPage() {
    return (
        <AppShell>
            <Suspense fallback={<PageGhost />}>
                <ProjectView />
            </Suspense>
        </AppShell>
    )
}
