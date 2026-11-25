"use client"

import { InviteMemberForm } from "@/components/projects/InviteMemberForm"
import { MemberRow } from "@/components/projects/MemberRow"
import { QueryState } from "@/components/util/QueryState"
import { notifyProblem } from "@/components/util/feedback"
import { useChangeRole, useMembers, useRemoveMember } from "@/lib/api/members"
import type { Project } from "@/lib/api/types"
import { allows } from "@/lib/project/permissions"
import { Divider, Modal, Skeleton, Stack } from "@mantine/core"

/** Who works in a project. Everyone may look; owners add people, change roles and remove them. */
export function ProjectMembersModal({
    project,
    opened,
    onClose,
}: Readonly<{ project: Project; opened: boolean; onClose: () => void }>) {
    const members = useMembers(project.id, opened)
    const changeRole = useChangeRole(project.id)
    const remove = useRemoveMember(project.id)
    const manages = allows(project.role, "manage")

    return (
        <Modal opened={opened} onClose={onClose} title={`Members of ${project.name}`} centered>
            <Stack gap="md">
                <QueryState query={members} ghost={<Skeleton h={80} radius="md" />}>
                    {(list) => (
                        <Stack gap="sm">
                            {list.map((member) => (
                                <MemberRow
                                    key={member.handle}
                                    member={member}
                                    manageable={manages}
                                    onRoleChange={(role) =>
                                        changeRole.mutate({ handle: member.handle, role }, { onError: notifyProblem })
                                    }
                                    onRemove={() => remove.mutate(member.handle, { onError: notifyProblem })}
                                />
                            ))}
                        </Stack>
                    )}
                </QueryState>
                {manages && (
                    <>
                        <Divider />
                        <InviteMemberForm projectId={project.id} />
                    </>
                )}
            </Stack>
        </Modal>
    )
}
