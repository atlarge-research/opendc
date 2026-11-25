"use client"

import { problemOf } from "@/lib/api/client"
import { Anchor, Group, Text } from "@mantine/core"
import type { UseQueryResult } from "@tanstack/react-query"
import type { ReactNode } from "react"

/**
 * Renders one query's loading and failure states so callers handle only loaded data. Wrap just the
 * part that needs the data, not the controls around it. [ghost] holds the layout while loading, and
 * data already on screen survives a failed refetch with a hint above it.
 */
export function QueryState<T>({
    query,
    ghost,
    children,
}: {
    query: UseQueryResult<T>
    ghost: ReactNode
    children: (data: T) => ReactNode
}) {
    if (query.data !== undefined) {
        return (
            <>
                {query.isError && <Failure query={query} label="Could not refresh" />}
                {children(query.data)}
            </>
        )
    }
    if (query.isPending) return ghost

    return <Failure query={query} label={problemOf(query.error).title} />
}

function Failure({ query, label }: { query: UseQueryResult<unknown>; label: string }) {
    return (
        <Group gap="xs" py="xs">
            <Text size="sm" c="red.6">
                {label}
            </Text>
            <Anchor size="sm" component="button" type="button" onClick={() => query.refetch()}>
                {query.isFetching ? "Retrying" : "Try again"}
            </Anchor>
        </Group>
    )
}
