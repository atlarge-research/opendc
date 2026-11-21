"use client"

import type { LegendEntry } from "@/components/experiment/results/SeriesLegend"
import { downloadChartImage } from "@/components/experiment/results/chartImage"
import { notifyProblem } from "@/components/util/feedback"
import { ActionIcon, Group, Paper, Stack, Text, Title, Tooltip } from "@mantine/core"
import { IconInfoCircle, IconPhotoDown } from "@tabler/icons-react"
import { type ReactNode, useRef } from "react"

export function ChartCard({
    title,
    caption,
    hint,
    fileName,
    legend,
    children,
}: {
    title: string
    caption: string
    hint: string
    fileName: string
    legend: LegendEntry[]
    children: ReactNode
}) {
    const card = useRef<HTMLDivElement>(null)

    const save = () => {
        if (card.current === null) return
        downloadChartImage(card.current, { title, caption, legend, fileName }).catch(notifyProblem)
    }

    return (
        <Paper ref={card} withBorder radius="md" p="md">
            <Stack gap="sm">
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                    <Stack gap={2}>
                        <Group gap={6}>
                            <Title order={5} fw={500}>
                                {title}
                            </Title>
                            <Tooltip label={hint} withArrow multiline w={280}>
                                <ActionIcon variant="subtle" color="gray" size="xs" aria-label={`About ${title}`}>
                                    <IconInfoCircle size={14} />
                                </ActionIcon>
                            </Tooltip>
                        </Group>
                        <Text size="xs" c="dimmed">
                            {caption}
                        </Text>
                    </Stack>
                    <Tooltip label="Save as PNG" withArrow>
                        <ActionIcon
                            variant="subtle"
                            color="gray"
                            aria-label={`Save ${title} as an image`}
                            onClick={save}
                        >
                            <IconPhotoDown size={16} />
                        </ActionIcon>
                    </Tooltip>
                </Group>
                {children}
            </Stack>
        </Paper>
    )
}
