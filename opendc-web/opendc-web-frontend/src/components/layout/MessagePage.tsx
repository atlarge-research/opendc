"use client"

import { Button, Center, Space, Stack, Text, Title } from "@mantine/core"
import { IconArrowLeft } from "@tabler/icons-react"
import Link from "next/link"
import type { ReactNode } from "react"

/** A whole-page dead end with a way out, for when a page cannot render at all. */
export function MessagePage({
    title,
    message,
    children,
}: Readonly<{ title: string; message: string; children?: ReactNode }>) {
    return (
        <Center mih="60vh">
            <Stack align="center" gap="md" maw={440} ta="center">
                <Title order={1}>{title}</Title>
                <Text c="dimmed">{message}</Text>
                <Space h="md" />
                {children}
            </Stack>
        </Center>
    )
}

/** The way out of a [MessagePage]: somewhere that is known to exist. */
export function BackTo({ href, label }: Readonly<{ href: string; label: string }>) {
    return (
        <Button component={Link} href={href} leftSection={<IconArrowLeft size={16} />}>
            {label}
        </Button>
    )
}
