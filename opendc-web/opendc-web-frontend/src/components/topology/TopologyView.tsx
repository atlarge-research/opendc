"use client"

import { BackTo, MessagePage } from "@/components/layout/MessagePage"
import { TopologyBuilder } from "@/components/topology/TopologyBuilder"
import { PanelGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { idParam } from "@/components/util/params"
import { problemOf } from "@/lib/api/client"
import { useTopology } from "@/lib/api/topologies"
import type { Id } from "@/lib/api/types"
import { Box } from "@mantine/core"
import { useSearchParams } from "next/navigation"

export function TopologyView() {
    const params = useSearchParams()
    const topologyId = idParam(params, "id")

    // The topology's own id is enough to find it, and the project it belongs to comes back with it.
    if (topologyId.status !== "ok") {
        return (
            <MessagePage title="No topology here" message="This link is missing a topology id.">
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return <LoadedTopology templateId={topologyId.value} />
}

// Keyed on the template id so the builder, which owns the document once open, survives saves.
function LoadedTopology({ templateId }: { templateId: Id }) {
    const template = useTopology(templateId)

    // The editor is the whole page, so a topology that never loaded leaves nothing to annotate.
    if (template.isError && template.data === undefined) {
        return (
            <MessagePage title="Topology not found" message={problemOf(template.error).title}>
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return (
        <Box px="md" py="sm">
            <QueryState query={template} ghost={<PanelGhost height={420} />}>
                {(loaded) => <TopologyBuilder key={loaded.id} template={loaded} />}
            </QueryState>
        </Box>
    )
}
