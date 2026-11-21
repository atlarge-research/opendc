"use client"

import { ProfileForm } from "@/components/user/ProfileForm"
import type { AuthSession } from "@/lib/auth/auth"
import { Modal } from "@mantine/core"

export function ProfileModal({
    session,
    opened,
    onClose,
}: Readonly<{ session: AuthSession; opened: boolean; onClose: () => void }>) {
    const handle = session.handle.type === "chosen" ? session.handle.name : ""

    return (
        <Modal opened={opened} onClose={onClose} title="Profile" centered>
            {opened && (
                <ProfileForm initial={{ handle, displayName: session.userName }} submitLabel="Save" onDone={onClose} />
            )}
        </Modal>
    )
}
