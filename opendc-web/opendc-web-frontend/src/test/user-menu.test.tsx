import { UserMenu } from "@/components/user/UserMenu"
import { theme } from "@/theme/theme"
import { MantineProvider } from "@mantine/core"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { cleanup, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const noop = () => {}

// The menu renders only once both /config and /me have answered.
function mockSession() {
    const profile = {
        displayName: "Developer",
        handle: { type: "chosen", name: "local" },
        plan: "free",
        isAdmin: true,
        projectCount: 0,
        budgets: [
            {
                period: "session",
                usedSeconds: 0,
                reservedSeconds: 0,
                cap: { type: "unlimited" },
                resetsAt: "2026-07-30T00:00:00Z",
            },
        ],
    }
    vi.stubGlobal(
        "fetch",
        vi.fn(async (input: RequestInfo | URL) => {
            const url = String(input)
            const body = url.endsWith("/config") ? { auth: { type: "anonymous" } } : profile
            return new Response(JSON.stringify(body), {
                status: 200,
                headers: { "Content-Type": "application/json" },
            })
        }),
    )
}

function mockSystemPrefersDark(prefersDark: boolean) {
    window.matchMedia = ((query: string) => ({
        matches: prefersDark && query.includes("dark"),
        media: query,
        onchange: null,
        addEventListener: noop,
        removeEventListener: noop,
        addListener: noop,
        removeListener: noop,
        dispatchEvent: () => false,
    })) as unknown as typeof window.matchMedia
}

// jsdom has no layout engine, so Mantine's drawer transition leaves the panel at
// display:none and its contents stay out of the accessibility tree.
function findToggle(target: "light" | "dark") {
    return screen.findByRole("button", { name: `Switch to ${target} mode`, hidden: true })
}

async function openUserDrawer() {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MantineProvider theme={theme} defaultColorScheme="auto">
                <UserMenu />
            </MantineProvider>
        </QueryClientProvider>,
    )
    const user = userEvent.setup()
    await user.click(await screen.findByRole("button", { name: "Open account menu" }))
    return user
}

beforeEach(mockSession)

afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    window.localStorage?.clear()
    delete document.documentElement.dataset.mantineColorScheme
})

describe("user drawer color scheme toggle", () => {
    it("starts from the system preference when it is dark", async () => {
        mockSystemPrefersDark(true)
        await openUserDrawer()
        expect(await findToggle("light")).toBeInTheDocument()
    })

    it("starts from the system preference when it is light", async () => {
        mockSystemPrefersDark(false)
        await openUserDrawer()
        expect(await findToggle("dark")).toBeInTheDocument()
    })

    it("flips the color scheme when the toggle is clicked", async () => {
        mockSystemPrefersDark(false)
        const user = await openUserDrawer()
        await user.click(await findToggle("dark"))
        expect(document.documentElement.dataset.mantineColorScheme).toBe("dark")
        expect(await findToggle("light")).toBeInTheDocument()
    })
})
