"use client"

import { budgetColor, budgetPercent, formatBudgetUsage, formatResetsAt } from "@/components/user/accountFormat"
import type { BudgetWindow } from "@/lib/api/types"
import { Group, Progress, Stack, Text } from "@mantine/core"

export function SimulationBudget({ budgets }: Readonly<{ budgets: BudgetWindow[] }>) {
    return (
        <Stack gap="md">
            <Text size="xs" fw={700} tt="uppercase" c="dimmed">
                Simulation budget
            </Text>
            {budgets.length === 0 ? (
                <Text size="sm" c="dimmed">
                    This account has unlimited simulation budget. Enjoy!
                </Text>
            ) : (
                budgets.map((budget) => <BudgetBar key={budget.period} budget={budget} />)
            )}
        </Stack>
    )
}

function BudgetBar({ budget }: Readonly<{ budget: BudgetWindow }>) {
    const percent = budgetPercent(budget)
    return (
        <Stack gap={6}>
            <Group justify="space-between" gap="xs" wrap="nowrap">
                <Text size="sm" fw={500} tt="capitalize">
                    {budget.period}
                </Text>
                <Text size="xs" c="dimmed">
                    {formatBudgetUsage(budget)}
                </Text>
            </Group>
            <Progress
                value={percent}
                color={budgetColor(percent)}
                size="sm"
                radius="xl"
                aria-label={`${budget.period} simulation budget`}
            />
            <Text size="xs" c="dimmed">
                {formatResetsAt(budget)}
            </Text>
        </Stack>
    )
}
