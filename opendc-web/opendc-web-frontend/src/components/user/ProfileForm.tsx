"use client"

import { HANDLE_PATTERN, HANDLE_RULE } from "@/lib/account/handle"
import { type ProfileChange, useUpdateProfile } from "@/lib/api/account"
import { ApiError } from "@/lib/api/client"
import { Button, Stack, TextInput } from "@mantine/core"
import { useState } from "react"

/** The handle other people see and the name shown with it, checked here and then by the server. */
export function ProfileForm({
    initial,
    submitLabel,
    onDone,
}: Readonly<{ initial: ProfileChange; submitLabel: string; onDone?: () => void }>) {
    const [handle, setHandle] = useState(initial.handle)
    const [displayName, setDisplayName] = useState(initial.displayName)
    const update = useUpdateProfile()
    const problem = update.error instanceof ApiError ? update.error.problem : undefined
    const issueAt = (path: string) => problem?.issues.find((issue) => issue.path === path)?.message
    const malformed = handle.length > 0 && !HANDLE_PATTERN.test(handle)

    return (
        <form
            onSubmit={(event) => {
                event.preventDefault()
                update.mutate({ handle, displayName }, { onSuccess: onDone })
            }}
        >
            <Stack gap="sm">
                <TextInput
                    label="Handle"
                    description="Prefixes the traces you share, and is how others add you to a project."
                    value={handle}
                    onChange={(event) => setHandle(event.currentTarget.value.toLowerCase())}
                    error={
                        malformed
                            ? HANDLE_RULE
                            : (issueAt("handle") ?? (problem?.status === 409 ? problem.title : undefined))
                    }
                    required
                />
                <TextInput
                    label="Display name"
                    value={displayName}
                    onChange={(event) => setDisplayName(event.currentTarget.value)}
                    error={issueAt("displayName")}
                    required
                />
                <Button type="submit" loading={update.isPending} disabled={malformed || displayName.trim() === ""}>
                    {submitLabel}
                </Button>
            </Stack>
        </form>
    )
}
