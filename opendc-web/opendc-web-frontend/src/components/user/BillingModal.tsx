"use client"

import { formatAmount, formatBillingDate } from "@/components/user/accountFormat"
import { TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { notifyComingSoon } from "@/components/util/feedback"
import { useBilling } from "@/lib/api/account"
import type { Billing, Invoice, PlanTier } from "@/lib/api/types"
import { Badge, Button, Group, Modal, Paper, Stack, Table, Text } from "@mantine/core"

export function BillingModal({
    plan,
    opened,
    onClose,
}: Readonly<{ plan: PlanTier; opened: boolean; onClose: () => void }>) {
    const billing = useBilling(opened)

    return (
        <Modal opened={opened} onClose={onClose} title="Billing details" size="md" centered>
            <QueryState query={billing} ghost={<TableGhost columns={3} />}>
                {(loaded) => <BillingDetails plan={plan} billing={loaded} />}
            </QueryState>
        </Modal>
    )
}

function BillingDetails({ plan, billing }: Readonly<{ plan: PlanTier; billing: Billing }>) {
    return (
        <Stack gap="lg">
            <Paper withBorder radius="md" p="md">
                <Stack gap={2}>
                    <Text fw={600} tt="capitalize">
                        {plan} plan
                    </Text>
                    <Text size="sm" c="dimmed">
                        {billing.renewsAt === undefined
                            ? "This plan does not renew"
                            : `Your subscription auto-renews on ${formatBillingDate(billing.renewsAt)}`}
                    </Text>
                </Stack>
            </Paper>
            {billing.paymentMethod !== undefined && (
                <Group justify="space-between">
                    <Text size="sm" c="dimmed">
                        Payment method
                    </Text>
                    <Text size="sm">{billing.paymentMethod}</Text>
                </Group>
            )}
            <InvoiceTable invoices={billing.invoices} />
            <Group justify="flex-end" gap="sm">
                <Button variant="default" onClick={() => notifyComingSoon("Managing your payment method")}>
                    Payment method
                </Button>
                <Button onClick={() => notifyComingSoon("Changing your plan")}>Change plan</Button>
            </Group>
        </Stack>
    )
}

function InvoiceTable({ invoices }: Readonly<{ invoices: Invoice[] }>) {
    if (invoices.length === 0) {
        return (
            <Text size="sm" c="dimmed">
                No invoices yet.
            </Text>
        )
    }

    return (
        <Stack gap="xs">
            <Text size="xs" fw={700} tt="uppercase" c="dimmed">
                Recent invoices
            </Text>
            <Table verticalSpacing="xs" horizontalSpacing={0}>
                <Table.Tbody>
                    {invoices.map((invoice) => (
                        <Table.Tr key={invoice.id}>
                            <Table.Td>{formatBillingDate(invoice.issuedAt)}</Table.Td>
                            <Table.Td>{formatAmount(invoice.amountEur)}</Table.Td>
                            <Table.Td ta="right">
                                <Badge variant="light" color={invoice.paid ? "green" : "yellow"} size="sm">
                                    {invoice.paid ? "Paid" : "Due"}
                                </Badge>
                            </Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>
        </Stack>
    )
}
