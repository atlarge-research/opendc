"use client"

import { ProjectMembersModal } from "@/components/projects/ProjectMembersModal"
import { notifyProblem } from "@/components/util/feedback"
import { useRemoveMember } from "@/lib/api/members"
import { useDeleteProject } from "@/lib/api/projects"
import type { Project } from "@/lib/api/types"
import { useAuth } from "@/lib/auth/auth"
import { allows } from "@/lib/project/permissions"
import { ActionIcon, Menu, Text } from "@mantine/core"
import { useDisclosure } from "@mantine/hooks"
import { modals } from "@mantine/modals"
import { IconDoorExit, IconDots, IconTrash, IconUsers } from "@tabler/icons-react"

/** Members for everyone, deleting for owners, and leaving for everyone else. */
export function ProjectActions({ project }: Readonly<{ project: Project }>) {
    const remove = useDeleteProject()
    const leave = useRemoveMember(project.id)
    const auth = useAuth()
    const [membersOpened, members] = useDisclosure(false)
    const handle =
        auth.status === "signedIn" && auth.session.handle.type === "chosen" ? auth.session.handle.name : undefined

    const confirmDelete = () =>
        modals.openConfirmModal({
            title: `Delete ${project.name}?`,
            children: <Text size="sm">Its topologies and experiments go with it. This cannot be undone.</Text>,
            labels: { confirm: "Delete project", cancel: "Keep it" },
            confirmProps: { color: "red" },
            onConfirm: () => remove.mutate(project.id, { onError: notifyProblem }),
        })

    const confirmLeave = (self: string) =>
        modals.openConfirmModal({
            title: `Leave ${project.name}?`,
            children: <Text size="sm">You lose access to it until an owner adds you again.</Text>,
            labels: { confirm: "Leave project", cancel: "Stay" },
            confirmProps: { color: "red" },
            onConfirm: () => leave.mutate(self, { onError: notifyProblem }),
        })

    return (
        <>
            <Menu position="bottom-end">
                <Menu.Target>
                    <ActionIcon variant="subtle" color="gray" aria-label="Project actions">
                        <IconDots size={18} />
                    </ActionIcon>
                </Menu.Target>
                <Menu.Dropdown>
                    <Menu.Item leftSection={<IconUsers size={16} />} onClick={members.open}>
                        Members
                    </Menu.Item>
                    {allows(project.role, "manage") ? (
                        <Menu.Item color="red" leftSection={<IconTrash size={16} />} onClick={confirmDelete}>
                            Delete
                        </Menu.Item>
                    ) : (
                        handle !== undefined && (
                            <Menu.Item
                                color="red"
                                leftSection={<IconDoorExit size={16} />}
                                onClick={() => confirmLeave(handle)}
                            >
                                Leave
                            </Menu.Item>
                        )
                    )}
                </Menu.Dropdown>
            </Menu>
            <ProjectMembersModal project={project} opened={membersOpened} onClose={members.close} />
        </>
    )
}
