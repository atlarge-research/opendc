"use client"

import { AccessTokensModal } from "@/components/user/AccessTokensModal"
import { BillingModal } from "@/components/user/BillingModal"
import { DeactivateAccountModal } from "@/components/user/DeactivateAccountModal"
import { ProfileModal } from "@/components/user/ProfileModal"
import { type AuthSession, useBilling } from "@/lib/auth/auth"
import { useSessionControls } from "@/lib/auth/session"
import { Divider, NavLink } from "@mantine/core"
import { useDisclosure } from "@mantine/hooks"
import {
    IconCreditCard,
    IconDatabase,
    IconId,
    IconKey,
    IconLogout,
    IconSettings,
    IconUserOff,
} from "@tabler/icons-react"
import Link from "next/link"

/** Signing out, tokens, billing and deactivation exist only where people sign in, not when anonymous. */
export function AccountActions({ session, closeDrawer }: Readonly<{ session: AuthSession; closeDrawer: () => void }>) {
    const controls = useSessionControls()
    const [billingOpened, billing] = useDisclosure(false)
    const [deactivateOpened, deactivation] = useDisclosure(false)
    const [profileOpened, profile] = useDisclosure(false)
    const [tokensOpened, tokens] = useDisclosure(false)
    const billingQuery = useBilling(billingOpened)

    const opening = (open: () => void) => () => {
        closeDrawer()
        open()
    }

    return (
        <>
            <NavLink
                component={Link}
                href="/traces"
                label="Traces"
                leftSection={<IconDatabase size={16} />}
                onClick={closeDrawer}
            />
            <NavLink
                component="button"
                label="Profile"
                leftSection={<IconId size={16} />}
                onClick={opening(profile.open)}
            />
            {controls.type === "auth0" && (
                <>
                    <NavLink
                        component="button"
                        label="Access tokens"
                        leftSection={<IconKey size={16} />}
                        onClick={opening(tokens.open)}
                    />
                    <NavLink
                        component="button"
                        label="Billing details"
                        leftSection={<IconCreditCard size={16} />}
                        onClick={opening(billing.open)}
                    />
                    <NavLink
                        component="button"
                        label="Log out"
                        leftSection={<IconLogout size={16} />}
                        onClick={controls.signOut}
                    />
                </>
            )}
            <Divider my="xs" />
            {session.account.isAdmin && (
                <NavLink
                    component={Link}
                    href="/admin"
                    c="red"
                    label="Admin panel"
                    leftSection={<IconSettings size={16} />}
                    onClick={closeDrawer}
                />
            )}
            {controls.type === "auth0" && (
                <>
                    <NavLink
                        component="button"
                        c="red"
                        label="Deactivate account"
                        leftSection={<IconUserOff size={16} />}
                        onClick={opening(deactivation.open)}
                    />
                    <DeactivateAccountModal
                        userName={session.userName}
                        opened={deactivateOpened}
                        onClose={deactivation.close}
                        onDeactivated={controls.signOut}
                    />
                    <AccessTokensModal opened={tokensOpened} onClose={tokens.close} />
                    <BillingModal
                        plan={session.account.plan}
                        billing={billingQuery.data}
                        opened={billingOpened}
                        onClose={billing.close}
                    />
                </>
            )}
            <ProfileModal session={session} opened={profileOpened} onClose={profile.close} />
        </>
    )
}
