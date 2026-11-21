"use client"

import {
    type History,
    canRedo as canRedoHistory,
    canUndo as canUndoHistory,
    initialHistory,
    record,
    redo as redoHistory,
    undo as undoHistory,
} from "@/components/topology/history"
import { type BuilderView, INITIAL_VIEW, type Selection, clampToTopology, focus } from "@/components/topology/selection"
import { notifyProblem } from "@/components/util/feedback"
import { useSaveTopology } from "@/lib/api/topologies"
import type { DocumentIssue, TopologyTemplate } from "@/lib/api/types"
import type { TopologyPlan } from "@/lib/topology/edits"
import { floorPlanOf } from "@/lib/topology/layout"
import { validateTopology } from "@/lib/topology/validation"
import { useCallback, useEffect, useMemo, useState } from "react"

const AUTOSAVE_DELAY_MS = 1200

export type SaveState = "saved" | "pending" | "saving"

export interface TopologyEditor {
    name: string
    plan: TopologyPlan
    view: BuilderView
    issues: DocumentIssue[]
    saveState: SaveState
    canUndo: boolean
    canRedo: boolean
    select: (selection: Selection) => void
    show: (view: BuilderView) => void
    rename: (name: string) => void
    apply: (change: (plan: TopologyPlan) => TopologyPlan) => void
    undo: () => void
    redo: () => void
    saveNow: () => void
}

/** The editor's state. One that is not [editable], a viewer's, ignores every edit and saves nothing. */
export function useTopologyEditor(template: TopologyTemplate, editable: boolean): TopologyEditor {
    const save = useSaveTopology(template.projectId)
    const [history, setHistory] = useState<History<TopologyPlan>>(() =>
        initialHistory({ topology: template.topology, layout: floorPlanOf(template.layout, template.topology) }),
    )
    const [name, setName] = useState(template.name)
    const [saved, setSaved] = useState<TopologyPlan>(() => history.present)
    const [view, setView] = useState<BuilderView>(INITIAL_VIEW)

    const plan = history.present
    const issues = useMemo(() => validateTopology(plan.topology), [plan.topology])
    const dirty = plan !== saved || name !== template.name

    const flush = useCallback(() => {
        if (!dirty || save.isPending) return
        setSaved(plan)
        save.mutate(
            { templateId: template.id, name, topology: plan.topology, layout: plan.layout },
            { onError: notifyProblem },
        )
    }, [dirty, name, plan, save, template.id])

    useEffect(() => {
        if (!dirty) return
        const timer = setTimeout(flush, AUTOSAVE_DELAY_MS)
        return () => clearTimeout(timer)
    }, [dirty, flush])

    const select = useCallback((selection: Selection) => setView((current) => focus(current, selection)), [])

    const apply = useCallback(
        (change: (plan: TopologyPlan) => TopologyPlan) => {
            if (editable) setHistory((current) => record(current, change(current.present)))
        },
        [editable],
    )

    const step = (move: (history: History<TopologyPlan>) => History<TopologyPlan>) => {
        const next = move(history)
        setHistory(next)
        setView((current) => clampToTopology(current, next.present.topology))
    }

    return {
        name,
        plan,
        view,
        issues,
        saveState: save.isPending ? "saving" : dirty ? "pending" : "saved",
        canUndo: canUndoHistory(history),
        canRedo: canRedoHistory(history),
        select,
        show: setView,
        rename: (next) => editable && setName(next),
        apply,
        undo: () => step(undoHistory),
        redo: () => step(redoHistory),
        saveNow: flush,
    }
}
