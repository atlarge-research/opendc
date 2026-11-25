"use client"

import { TokenRow } from "@/components/user/TokenRow"
import { TokenSecret } from "@/components/user/TokenSecret"
import { TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { notifyProblem } from "@/components/util/feedback"
import { useAccessTokens, useMintToken, useRevokeToken } from "@/lib/api/account"
import type { AccessToken } from "@/lib/api/types"
import { Button, Group, Modal, Stack, Table, Text, TextInput } from "@mantine/core"
import { useState } from "react"

/** Personal access tokens, which is how the CLI and scripts act as the person. */
export function AccessTokensModal({ opened, onClose }: Readonly<{ opened: boolean; onClose: () => void }>) {
    const tokens = useAccessTokens(opened)
    const mint = useMintToken()
    const revoke = useRevokeToken()
    const [name, setName] = useState("")

    const close = () => {
        mint.reset()
        setName("")
        onClose()
    }

    return (
        <Modal opened={opened} onClose={close} title="Access tokens" size="lg" centered>
            <Stack gap="md">
                {mint.data && <TokenSecret secret={mint.data.secret} />}
                <form
                    onSubmit={(event) => {
                        event.preventDefault()
                        mint.mutate(name, { onSuccess: () => setName(""), onError: notifyProblem })
                    }}
                >
                    <Group align="flex-end" gap="xs">
                        <TextInput
                            label="New token"
                            placeholder="What it is for, such as laptop or CI"
                            value={name}
                            onChange={(event) => setName(event.currentTarget.value)}
                            flex={1}
                        />
                        <Button type="submit" loading={mint.isPending} disabled={name.trim() === ""}>
                            Create
                        </Button>
                    </Group>
                </form>
                <QueryState query={tokens} ghost={<TableGhost columns={3} rows={2} />}>
                    {(loaded) => (
                        <TokenTable tokens={loaded} onRevoke={(id) => revoke.mutate(id, { onError: notifyProblem })} />
                    )}
                </QueryState>
            </Stack>
        </Modal>
    )
}

function TokenTable({ tokens, onRevoke }: Readonly<{ tokens: AccessToken[]; onRevoke: (id: string) => void }>) {
    if (tokens.length === 0) {
        return (
            <Text size="sm" c="dimmed">
                No tokens yet.
            </Text>
        )
    }

    return (
        <Table>
            <Table.Tbody>
                {tokens.map((token) => (
                    <TokenRow key={token.id} token={token} onRevoke={() => onRevoke(token.id)} />
                ))}
            </Table.Tbody>
        </Table>
    )
}
