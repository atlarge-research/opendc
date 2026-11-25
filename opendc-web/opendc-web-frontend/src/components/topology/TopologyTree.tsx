"use client"

import { TreeRow } from "@/components/topology/TreeRow"
import type { Selection } from "@/components/topology/selection"
import { ROOT_VALUE, type TreeTarget, buildTree, isActive, targetPath } from "@/components/topology/tree"
import type { DocumentIssue } from "@/lib/api/types"
import type { TopologySpec } from "@/lib/topology/spec"
import { issuesUnder } from "@/lib/topology/validation"
import { Group, ScrollArea, Tree, getTreeExpandedState, useTree } from "@mantine/core"
import { useMemo } from "react"

export function TopologyTree({
    topology,
    selection,
    issues,
    onSelect,
}: {
    topology: TopologySpec
    selection: Selection
    issues: DocumentIssue[]
    onSelect: (target: TreeTarget) => void
}) {
    const model = useMemo(() => buildTree(topology), [topology])
    const tree = useTree({
        initialExpandedState: getTreeExpandedState(model.data, [
            ROOT_VALUE,
            ...topology.datacenters.map((_, index) => `dc-${index}`),
        ]),
    })

    return (
        <ScrollArea h="100%" p="xs">
            <Tree
                data={model.data}
                tree={tree}
                levelOffset={16}
                renderNode={({ node, expanded, hasChildren, elementProps }) => {
                    const target = model.targets.get(node.value)
                    if (!target) return null
                    const path = targetPath(target)
                    return (
                        <Group
                            {...elementProps}
                            gap={6}
                            wrap="nowrap"
                            py={3}
                            onClick={(event) => {
                                elementProps.onClick(event)
                                onSelect(target)
                            }}
                        >
                            <TreeRow
                                target={target}
                                label={String(node.label)}
                                expanded={expanded}
                                hasChildren={hasChildren}
                                active={isActive(target, selection)}
                                broken={path === "" ? issues.length > 0 : issuesUnder(issues, path).length > 0}
                            />
                        </Group>
                    )
                }}
            />
        </ScrollArea>
    )
}
