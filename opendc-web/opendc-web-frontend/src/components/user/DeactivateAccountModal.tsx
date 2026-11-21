"use client"

import { useDeactivateAccount } from "@/lib/api/account"
import { ApiError } from "@/lib/api/client"
import { Alert, Button, Group, List, Modal, Stack, Text, TextInput } from "@mantine/core"
import { IconAlertTriangle } from "@tabler/icons-react"
import { useState } from "react"

/** The server refuses a sole owner of a shared project, listing what to hand over first. */
export function DeactivateAccountModal({
    userName,
    opened,
    onClose,
    onDeactivated,
}: Readonly<{ userName: string; opened: boolean; onClose: () => void; onDeactivated: () => void }>) {
    const [confirmation, setConfirmation] = useState("")
    const deactivate = useDeactivateAccount()
    const refusal = deactivate.error instanceof ApiError ? deactivate.error.problem : undefined

    function close() {
        setConfirmation("")
        deactivate.reset()
        onClose()
    }

    return (
        <Modal opened={opened} onClose={close} title="Deactivate account" size="md" centered>
            <Stack gap="md">
                <Alert color="red" variant="light" icon={<IconAlertTriangle size="md" />}>
                    You lose access to OpenDC for good: your access tokens stop working and you leave every project
                    others work in. Nothing is deleted, and projects only you work in stay as they are.
                </Alert>
                {refusal && (
                    <Alert color="red" title={refusal.title}>
                        <List size="sm">
                            {refusal.issues.map((issue) => (
                                <List.Item key={issue.message}>{issue.message}</List.Item>
                            ))}
                        </List>
                    </Alert>
                )}
                <TextInput
                    label={
                        <Text size="xs">
                            Type <i>{userName}</i> to confirm
                        </Text>
                    }
                    value={confirmation}
                    onChange={(event) => setConfirmation(event.currentTarget.value)}
                />
                <Group justify="flex-end" gap="sm">
                    <Button variant="default" onClick={close}>
                        Cancel
                    </Button>
                    <Button
                        color="red"
                        disabled={confirmation !== userName}
                        loading={deactivate.isPending}
                        onClick={() => deactivate.mutate(undefined, { onSuccess: onDeactivated })}
                    >
                        Deactivate
                    </Button>
                </Group>
            </Stack>
        </Modal>
    )
}
