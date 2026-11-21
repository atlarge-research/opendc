"use client"

import { openEditTrace } from "@/components/traces/EditTraceModal"
import { openShareTrace } from "@/components/traces/ShareTraceModal"
import { notifyProblem } from "@/components/util/feedback"
import { startDownload } from "@/lib/api/downloads"
import { traceContentLink, useDeleteTrace } from "@/lib/api/traces"
import type { Trace } from "@/lib/api/types"
import { ActionIcon, Menu } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconDots, IconDownload, IconPencil, IconShare, IconTrash } from "@tabler/icons-react"

export function TraceActions({ trace }: { trace: Trace }) {
    const remove = useDeleteTrace()
    const mine = trace.access === "owned"

    return (
        <Menu position="bottom-end" withinPortal>
            <Menu.Target>
                <ActionIcon variant="subtle" color="gray" aria-label={`Actions for ${trace.slug}`}>
                    <IconDots size={16} />
                </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
                <Menu.Item
                    leftSection={<IconDownload size={14} />}
                    onClick={() => startDownload(traceContentLink(trace.id)).catch(notifyProblem)}
                >
                    Download
                </Menu.Item>
                {mine && (
                    <>
                        <Menu.Item leftSection={<IconPencil size={14} />} onClick={() => openEditTrace(trace)}>
                            Edit
                        </Menu.Item>
                        <Menu.Item leftSection={<IconShare size={14} />} onClick={() => openShareTrace(trace)}>
                            Share
                        </Menu.Item>
                        <Menu.Item
                            color="red"
                            leftSection={<IconTrash size={14} />}
                            onClick={() =>
                                confirmDelete(trace, () => remove.mutate(trace.id, { onError: notifyProblem }))
                            }
                        >
                            Delete
                        </Menu.Item>
                    </>
                )}
            </Menu.Dropdown>
        </Menu>
    )
}

function confirmDelete(trace: Trace, onConfirm: () => void): void {
    modals.openConfirmModal({
        title: `Delete ${trace.slug}?`,
        children: "The files go with it. An experiment that already references this trace keeps working.",
        labels: { confirm: "Delete", cancel: "Keep" },
        confirmProps: { color: "red" },
        onConfirm,
    })
}
