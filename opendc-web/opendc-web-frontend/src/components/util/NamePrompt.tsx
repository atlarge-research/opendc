"use client"

import { Button, Group, Stack, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { useState } from "react"

export interface NamePromptOptions {
    title: string
    label: string
    initial?: string
    confirmLabel: string
    onSubmit: (name: string) => void
}

const MODAL_ID = "name-prompt"

export function openNamePrompt(options: NamePromptOptions): void {
    modals.open({
        modalId: MODAL_ID,
        title: options.title,
        children: <NamePromptForm options={options} onDone={() => modals.close(MODAL_ID)} />,
    })
}

function NamePromptForm({ options, onDone }: { options: NamePromptOptions; onDone: () => void }) {
    const [value, setValue] = useState(options.initial ?? "")
    const trimmed = value.trim()

    const submit = () => {
        if (trimmed === "") return
        options.onSubmit(trimmed)
        onDone()
    }

    return (
        <Stack>
            <TextInput
                label={options.label}
                value={value}
                data-autofocus
                onChange={(event) => setValue(event.currentTarget.value)}
                onKeyDown={(event) => {
                    if (event.key === "Enter") submit()
                }}
            />
            <Group justify="flex-end">
                <Button variant="default" onClick={onDone}>
                    Cancel
                </Button>
                <Button onClick={submit} disabled={trimmed === ""}>
                    {options.confirmLabel}
                </Button>
            </Group>
        </Stack>
    )
}
