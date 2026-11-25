"use client"

import type { SaveState } from "@/components/topology/useTopologyEditor"
import { ActionIcon, Group, Loader, Text, Tooltip } from "@mantine/core"
import {
    IconAdjustments,
    IconArrowBackUp,
    IconArrowForwardUp,
    IconCheck,
    IconDownload,
    IconList,
    IconMaximize,
    IconZoomIn,
    IconZoomOut,
} from "@tabler/icons-react"

export function BuilderToolbar({
    saveState,
    canUndo,
    canRedo,
    onUndo,
    onRedo,
    onZoom,
    onExport,
    onOpenTree,
    onOpenInspector,
}: {
    saveState: SaveState
    canUndo: boolean
    canRedo: boolean
    onUndo: () => void
    onRedo: () => void
    onZoom: (action: "in" | "out" | "fit") => void
    onExport: () => void
    onOpenTree?: () => void
    onOpenInspector?: () => void
}) {
    return (
        <Group justify="space-between" gap={4} px="sm" py={6} wrap="nowrap">
            {onOpenTree ? (
                <Tooltip label="Clusters">
                    <ActionIcon variant="subtle" color="gray" onClick={onOpenTree} aria-label="Open cluster list">
                        <IconList size={18} />
                    </ActionIcon>
                </Tooltip>
            ) : (
                <span />
            )}
            <Group gap={4} wrap="nowrap">
                <SaveIndicator state={saveState} />
                <Tooltip label="Undo (mod+Z)">
                    <ActionIcon variant="subtle" color="gray" disabled={!canUndo} onClick={onUndo} aria-label="Undo">
                        <IconArrowBackUp size={18} />
                    </ActionIcon>
                </Tooltip>
                <Tooltip label="Redo (mod+shift+Z)">
                    <ActionIcon variant="subtle" color="gray" disabled={!canRedo} onClick={onRedo} aria-label="Redo">
                        <IconArrowForwardUp size={18} />
                    </ActionIcon>
                </Tooltip>
                <Tooltip label="Zoom out">
                    <ActionIcon variant="subtle" color="gray" onClick={() => onZoom("out")} aria-label="Zoom out">
                        <IconZoomOut size={18} />
                    </ActionIcon>
                </Tooltip>
                <Tooltip label="Zoom in">
                    <ActionIcon variant="subtle" color="gray" onClick={() => onZoom("in")} aria-label="Zoom in">
                        <IconZoomIn size={18} />
                    </ActionIcon>
                </Tooltip>
                <Tooltip label="Fit to content">
                    <ActionIcon variant="subtle" color="gray" onClick={() => onZoom("fit")} aria-label="Fit to content">
                        <IconMaximize size={18} />
                    </ActionIcon>
                </Tooltip>
                <Tooltip label="Download JSON">
                    <ActionIcon variant="subtle" color="gray" onClick={onExport} aria-label="Download topology JSON">
                        <IconDownload size={18} />
                    </ActionIcon>
                </Tooltip>
                {onOpenInspector && (
                    <Tooltip label="Editor">
                        <ActionIcon variant="subtle" color="gray" onClick={onOpenInspector} aria-label="Open editor">
                            <IconAdjustments size={18} />
                        </ActionIcon>
                    </Tooltip>
                )}
            </Group>
        </Group>
    )
}

function SaveIndicator({ state }: { state: SaveState }) {
    if (state === "saving") return <Loader size={14} mr="xs" />
    if (state === "pending")
        return (
            <Text size="xs" c="dimmed" mr="xs">
                Unsaved
            </Text>
        )
    return (
        <Tooltip label="All changes saved">
            <IconCheck size={16} color="var(--mantine-color-green-6)" style={{ marginRight: 8 }} />
        </Tooltip>
    )
}
