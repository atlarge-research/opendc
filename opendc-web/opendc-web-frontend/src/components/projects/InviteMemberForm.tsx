"use client"

import { ROLE_OPTIONS, ROLE_SELECT_WIDTH } from "@/components/projects/MemberRow"
import { notifyProblem } from "@/components/util/feedback"
import { useInviteMember } from "@/lib/api/members"
import type { Id, ProjectRole } from "@/lib/api/types"
import { Button, Group, Select, TextInput } from "@mantine/core"
import { useState } from "react"

export function InviteMemberForm({ projectId }: Readonly<{ projectId: Id }>) {
    const invite = useInviteMember(projectId)
    const [handle, setHandle] = useState("")
    const [role, setRole] = useState<ProjectRole>("viewer")

    return (
        <form
            onSubmit={(event) => {
                event.preventDefault()
                invite.mutate(
                    { handle: handle.trim(), role },
                    { onSuccess: () => setHandle(""), onError: notifyProblem },
                )
            }}
        >
            <Group align="flex-end" gap="xs" wrap="nowrap">
                <TextInput
                    label="Add someone"
                    placeholder="Their handle"
                    value={handle}
                    onChange={(event) => setHandle(event.currentTarget.value)}
                    flex={1}
                />
                <Select
                    w={ROLE_SELECT_WIDTH}
                    data={ROLE_OPTIONS}
                    value={role}
                    allowDeselect={false}
                    aria-label="Role"
                    onChange={(value) => value && setRole(value as ProjectRole)}
                />
                <Button type="submit" loading={invite.isPending} disabled={handle.trim() === ""}>
                    Add
                </Button>
            </Group>
        </form>
    )
}
