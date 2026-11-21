"use client"

import { Card, Container, Group, Paper, SimpleGrid, Skeleton, Stack, Table } from "@mantine/core"

// Loading outlines that mirror the layout they stand in for, so nothing shifts when the data lands.

function keys(count: number, prefix: string): string[] {
    return Array.from({ length: count }, (_, index) => `${prefix}-${index}`)
}

export function TableGhost({ columns, rows = 3 }: Readonly<{ columns: number; rows?: number }>) {
    return (
        <Table verticalSpacing="sm">
            <Table.Tbody>
                {keys(rows, "row").map((row) => (
                    <Table.Tr key={row}>
                        {keys(columns, row).map((cell, column) => (
                            <Table.Td key={cell}>
                                <Skeleton height={12} radius="sm" width={column === 0 ? "60%" : "40%"} />
                            </Table.Td>
                        ))}
                    </Table.Tr>
                ))}
            </Table.Tbody>
        </Table>
    )
}

export function CardGridGhost({ cards = 3 }: Readonly<{ cards?: number }>) {
    return (
        <>
            <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
                {keys(cards, "card").map((card) => (
                    <Card key={card} withBorder radius="md" padding="md" h="100%">
                        <Stack gap="sm">
                            <Skeleton height={16} radius="sm" width="55%" />
                            <Skeleton height={10} radius="sm" width="35%" />
                            <Group gap="xs" mt="xs">
                                <Skeleton height={18} radius="xl" width={64} />
                                <Skeleton height={18} radius="xl" width={48} />
                            </Group>
                        </Stack>
                    </Card>
                ))}
            </SimpleGrid>
        </>
    )
}

/** A single line of text, for headers and breadcrumbs. */
export function LineGhost({ width = "40%" }: Readonly<{ width?: string | number }>) {
    return <Skeleton height={14} radius="sm" width={width} />
}

/** A whole page whose kind is not known yet, such as a route that has not read its parameters. */
export function PageGhost() {
    return (
        <Container size="lg" py="md">
            <Stack gap="md">
                <LineGhost width={220} />
                <PanelGhost height={220} />
            </Stack>
        </Container>
    )
}

/** A bordered block, for a panel whose contents are still unknown. */
export function PanelGhost({ height = 180 }: Readonly<{ height?: number }>) {
    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                <Skeleton height={14} radius="sm" width="30%" />
                <Skeleton height={height} radius="sm" />
            </Stack>
        </Paper>
    )
}
