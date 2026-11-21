"use client"

import { logoGradient } from "@/theme/theme"
import { ActionIcon, Anchor, Box, Group, Image, Text } from "@mantine/core"
import { IconBrandGithub, IconHelp } from "@tabler/icons-react"
import Link from "next/link"
import { UserMenu } from "../user/UserMenu"

const githubUrl = "https://github.com/atlarge-research/opendc"
const helpUrl = "https://opendc.org/"

export function AppHeader() {
    return (
        <Box
            h="100%"
            px="md"
            bg="dark.7"
            c="white"
            style={{
                borderBottom: "3px solid",
                borderImageSource: logoGradient,
                borderImageSlice: 1,
            }}
        >
            <Group h="100%" justify="space-between">
                <Group gap="xl">
                    <Anchor component={Link} href="/" underline="never" c="white">
                        <Group gap="xs">
                            <Image src="/img/logo.svg" alt="OpenDC" w={24} />
                            <Text fw={600} size="md">
                                OpenDC
                            </Text>
                        </Group>
                    </Anchor>
                </Group>
                <Group gap="sm">
                    <ActionIcon
                        component="a"
                        href={githubUrl}
                        target="_blank"
                        variant="subtle"
                        c="white"
                        aria-label="OpenDC on GitHub"
                    >
                        <IconBrandGithub size={20} />
                    </ActionIcon>
                    <ActionIcon
                        component="a"
                        href={helpUrl}
                        target="_blank"
                        variant="subtle"
                        c="white"
                        aria-label="Help"
                    >
                        <IconHelp size={20} />
                    </ActionIcon>
                    <UserMenu />
                </Group>
            </Group>
        </Box>
    )
}
