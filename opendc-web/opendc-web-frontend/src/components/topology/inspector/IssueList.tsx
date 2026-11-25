"use client"

import type { DocumentIssue } from "@/lib/api/types"
import { Alert, Stack, Text } from "@mantine/core"
import { IconAlertTriangle } from "@tabler/icons-react"

const SHOWN = 8

/** The problems under one part of the topology, with paths written relative to it. */
export function IssueList({ issues, prefix = "" }: { issues: DocumentIssue[]; prefix?: string }) {
    if (issues.length === 0) return null
    const hidden = issues.length - SHOWN

    return (
        <Alert
            color="red"
            icon={<IconAlertTriangle size={16} />}
            p="xs"
            radius="sm"
            title={issues.length === 1 ? "1 problem" : `${issues.length} problems`}
        >
            <Stack gap={2}>
                {issues.slice(0, SHOWN).map((issue) => (
                    <Text key={`${issue.path}-${issue.message}`} size="xs">
                        {issue.path.startsWith(prefix) ? issue.path.slice(prefix.length) : issue.path} {issue.message}
                    </Text>
                ))}
                {hidden > 0 && (
                    <Text size="xs" c="dimmed">
                        and {hidden} more
                    </Text>
                )}
            </Stack>
        </Alert>
    )
}
