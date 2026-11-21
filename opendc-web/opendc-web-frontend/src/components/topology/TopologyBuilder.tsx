"use client"

import { BuilderPanes } from "@/components/topology/BuilderPanes"
import { BuilderToolbar } from "@/components/topology/BuilderToolbar"
import { DataCenterTabs } from "@/components/topology/DataCenterTabs"
import { useResizableWidth } from "@/components/topology/ResizeHandle"
import { TopologyTree } from "@/components/topology/TopologyTree"
import { builderActions } from "@/components/topology/builderActions"
import type { ZoomCommand } from "@/components/topology/canvas/FloorStage"
import { TopologyInspector } from "@/components/topology/inspector/TopologyInspector"
import {
    WHOLE_TOPOLOGY,
    extendToCluster,
    selectCluster,
    selectDataCenter,
    selectHost,
    selectedClusters,
    toggleHost,
} from "@/components/topology/selection"
import { selectionFor } from "@/components/topology/tree"
import { useTopologyEditor } from "@/components/topology/useTopologyEditor"
import { EntityBreadcrumbs } from "@/components/util/EntityBreadcrumbs"
import { openNamePrompt } from "@/components/util/NamePrompt"
import { downloadJson } from "@/components/util/download"
import { useProject } from "@/lib/api/projects"
import type { Id, TopologyTemplate } from "@/lib/api/types"
import { usePermission } from "@/lib/project/permissions"
import { dataCenterHeadroom, isOverBudget, supplyOf } from "@/lib/topology/capacity"
import { placeCluster } from "@/lib/topology/edits"
import { dataCenterName } from "@/lib/topology/spec"
import { brokenClusters, brokenDataCenters } from "@/lib/topology/validation"
import { ActionIcon, Box, Fieldset, Skeleton, Stack } from "@mantine/core"
import { useDisclosure, useHotkeys, useMediaQuery } from "@mantine/hooks"
import { IconPencil } from "@tabler/icons-react"
import dynamic from "next/dynamic"
import { useState } from "react"

const INSPECTOR_WIDTH = 360
const INSPECTOR_MIN_WIDTH = 300
const INSPECTOR_MAX_WIDTH = 720
const COMPACT_QUERY = "(max-width: 75em)"

// Loaded on demand because konva is large; the skeleton keeps the floor's area meanwhile.
const FloorStage = dynamic(() => import("@/components/topology/canvas/FloorStage").then((m) => m.FloorStage), {
    ssr: false,
    loading: () => <Skeleton h="100%" radius="md" />,
})

export function TopologyBuilder({ template }: { template: TopologyTemplate }) {
    const editable = usePermission(template.projectId, "edit")
    const editor = useTopologyEditor(template, editable)
    const [zoom, setZoom] = useState<ZoomCommand>({ action: "fit", nonce: 0 })
    const inspector = useResizableWidth(INSPECTOR_WIDTH, INSPECTOR_MIN_WIDTH, INSPECTOR_MAX_WIDTH)
    const compact = useMediaQuery(COMPACT_QUERY, false, { getInitialValueInEffect: true })
    const [treeOpened, tree] = useDisclosure(false)
    const [inspectorOpened, inspectorDrawer] = useDisclosure(false)
    const { plan, view, issues } = editor
    const { selection, floor: shown } = view
    const actions = builderActions(editor)
    const dataCenters = plan.topology.datacenters
    const dataCenter = dataCenters[shown]
    const floor = plan.layout.floors[shown]

    useHotkeys([
        ["mod+z", editor.undo],
        ["mod+shift+Z", editor.redo],
        ["mod+s", editor.saveNow],
        ["mod+d", () => editable && actions.duplicateSelected()],
        ["Delete", () => editable && actions.deleteSelected()],
        ["Backspace", () => editable && actions.deleteSelected()],
        ["Escape", () => editor.select(WHOLE_TOPOLOGY)],
    ])

    return (
        <Stack gap="xs">
            <TopologyBreadcrumbs
                projectId={template.projectId}
                name={editor.name}
                {...(editable
                    ? {
                          onRename: () =>
                              openNamePrompt({
                                  title: "Rename topology",
                                  label: "Topology name",
                                  initial: editor.name,
                                  confirmLabel: "Rename",
                                  onSubmit: editor.rename,
                              }),
                      }
                    : {})}
            />
            <BuilderPanes
                compact={compact}
                resizer={inspector}
                drawers={{
                    treeOpened,
                    inspectorOpened,
                    closeTree: tree.close,
                    closeInspector: inspectorDrawer.close,
                }}
                tree={
                    <TopologyTree
                        topology={plan.topology}
                        selection={selection}
                        issues={issues}
                        onSelect={(target) => editor.select(selectionFor(target, selection))}
                    />
                }
                canvas={
                    <>
                        <BuilderToolbar
                            saveState={editor.saveState}
                            canUndo={editor.canUndo}
                            canRedo={editor.canRedo}
                            onUndo={editor.undo}
                            onRedo={editor.redo}
                            onZoom={(action) => setZoom((current) => ({ action, nonce: current.nonce + 1 }))}
                            onExport={() => downloadJson(editor.name, plan.topology)}
                            {...(compact ? { onOpenTree: tree.open, onOpenInspector: inspectorDrawer.open } : {})}
                        />
                        <DataCenterTabs
                            names={dataCenters.map(dataCenterName)}
                            broken={brokenDataCenters(issues, dataCenters.length)}
                            active={shown}
                            onSelect={(index) => editor.select(selectDataCenter(index))}
                            {...(editable ? { onAdd: actions.addDataCenter } : {})}
                        />
                        <Box flex={1} mih={0}>
                            {dataCenter && floor && (
                                <FloorStage
                                    key={shown}
                                    floor={floor}
                                    clusters={dataCenter.clusters}
                                    supply={supplyOf(dataCenter)}
                                    overBudget={isOverBudget(dataCenterHeadroom(dataCenter))}
                                    selected={selectedClusters(selection, shown)}
                                    broken={brokenClusters(issues, shown, dataCenter.clusters.length)}
                                    zoom={zoom}
                                    onCreate={(cell) => editable && actions.createAt(cell)}
                                    onSelect={(cluster, additive) => {
                                        const at = { dataCenter: shown, cluster }
                                        editor.select(additive ? extendToCluster(selection, at) : selectCluster(at))
                                    }}
                                    onClearSelection={() => editor.select(selectDataCenter(shown))}
                                    onPlace={(cluster, cell) =>
                                        editor.apply((current) =>
                                            placeCluster(current, { dataCenter: shown, cluster }, cell),
                                        )
                                    }
                                    onOpen={(cluster) => {
                                        editor.select(selectHost({ dataCenter: shown, cluster, host: 0 }))
                                        if (compact) inspectorDrawer.open()
                                    }}
                                />
                            )}
                        </Box>
                    </>
                }
                inspector={
                    <Fieldset variant="unstyled" disabled={!editable} h="100%">
                        <TopologyInspector
                            plan={plan}
                            selection={selection}
                            issues={issues}
                            apply={editor.apply}
                            onSelectHost={(at) => editor.select(toggleHost(selection, at))}
                            onMoveClusters={actions.moveClusters}
                            onDuplicateDataCenter={actions.duplicateDataCenter}
                            onRemoveDataCenter={actions.removeDataCenter}
                        />
                    </Fieldset>
                }
            />
        </Stack>
    )
}

function TopologyBreadcrumbs({
    projectId,
    name,
    onRename,
}: {
    projectId: Id
    name: string
    onRename?: () => void
}) {
    const project = useProject(projectId)

    return (
        <EntityBreadcrumbs
            crumbs={[
                { kind: "projects", label: "Projects", href: "/" },
                { kind: "project", label: project.data?.name ?? "Project", href: `/project?id=${projectId}` },
                { kind: "topology", label: name },
            ]}
            trailing={
                onRename && (
                    <ActionIcon variant="subtle" color="gray" size="sm" aria-label="Rename topology" onClick={onRename}>
                        <IconPencil size={14} />
                    </ActionIcon>
                )
            }
        />
    )
}
