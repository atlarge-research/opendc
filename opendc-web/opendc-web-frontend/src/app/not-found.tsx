"use client"

import { AppShell } from "@/components/layout/AppShell"
import { BackTo, MessagePage } from "@/components/layout/MessagePage"

export default function NotFound() {
    return (
        <AppShell>
            <MessagePage title="404" message="This page does not exist.">
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        </AppShell>
    )
}
