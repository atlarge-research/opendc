"use client"

import { ProfileForm } from "@/components/user/ProfileForm"
import { suggestHandle } from "@/lib/account/handle"
import { useSessionControls } from "@/lib/auth/session"
import { Center, Paper, Stack, Text, Title } from "@mantine/core"

/** A first sign-in must choose a handle before anything else, since everything it shares carries it. */
export function OnboardingPage() {
    const controls = useSessionControls()
    const hint = controls.type === "auth0" ? controls.hint : { nickname: "", name: "" }

    return (
        <Center mih="70vh">
            <Paper withBorder radius="md" p="xl" w={420} maw="100%">
                <Stack gap="md">
                    <Title order={3}>Welcome to OpenDC</Title>
                    <Text size="sm" c="dimmed">
                        Choose the handle others will know you by. You can change it later, as long as no trace you have
                        shared carries it.
                    </Text>
                    <ProfileForm
                        initial={{
                            handle: suggestHandle(hint.nickname || hint.name || "user"),
                            displayName: hint.name,
                        }}
                        submitLabel="Continue"
                    />
                </Stack>
            </Paper>
        </Center>
    )
}
