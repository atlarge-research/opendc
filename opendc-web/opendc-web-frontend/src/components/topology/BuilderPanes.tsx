"use client"

import { ResizeHandle, type Resizer } from "@/components/topology/ResizeHandle"
import { Drawer, Flex, Paper } from "@mantine/core"
import type { ReactNode } from "react"

export interface BuilderDrawers {
    treeOpened: boolean
    inspectorOpened: boolean
    closeTree: () => void
    closeInspector: () => void
}

export function BuilderPanes({
    compact,
    tree,
    canvas,
    inspector,
    resizer,
    drawers,
}: {
    compact: boolean
    tree: ReactNode
    canvas: ReactNode
    inspector: ReactNode
    resizer: Resizer
    drawers: BuilderDrawers
}) {
    if (compact) {
        return (
            <>
                <Paper withBorder radius="md" h="65vh" style={{ display: "flex", flexDirection: "column" }}>
                    {canvas}
                </Paper>
                <Drawer
                    opened={drawers.treeOpened}
                    onClose={drawers.closeTree}
                    position="left"
                    title="Topology"
                    size="80%"
                >
                    {tree}
                </Drawer>
                <Drawer
                    opened={drawers.inspectorOpened}
                    onClose={drawers.closeInspector}
                    position="right"
                    title="Editor"
                    size="90%"
                >
                    {inspector}
                </Drawer>
            </>
        )
    }

    return (
        <Flex h="calc(100vh - 165px)" gap={0} align="stretch">
            <Paper withBorder radius="md" w={230} mr="xs" style={{ flexShrink: 0, overflow: "hidden" }}>
                {tree}
            </Paper>

            <Paper withBorder radius="md" flex={1} style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                {canvas}
            </Paper>

            <ResizeHandle resizer={resizer} label="Resize the inspector" />

            <Paper withBorder radius="md" w={resizer.width} style={{ flexShrink: 0, overflow: "hidden" }}>
                {inspector}
            </Paper>
        </Flex>
    )
}
