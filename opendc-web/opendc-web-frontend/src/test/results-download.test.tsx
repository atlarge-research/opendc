import { ResultsPanel } from "@/components/experiment/results/ResultsPanel"
import { downloadChartImage } from "@/components/experiment/results/chartImage"
import { resultsArchiveLink } from "@/lib/api/experiments"
import type { Experiment } from "@/lib/api/types"
import type { ExperimentResults } from "@/lib/experiment/results"
import type { ExperimentState } from "@/lib/experiment/status"
import { theme } from "@/theme/theme"
import { MantineProvider } from "@mantine/core"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"

const EXPERIMENT_ID = "8f2b6b3e-58cd-4c04-9a8e-1b8f4b1a2c33"

const RESULTS: ExperimentResults = {
    experimentId: EXPERIMENT_ID,
    exportIntervalMs: 300_000,
    bucketMs: 300_000,
    complete: true,
    scenarios: [
        {
            scenarioIndex: 0,
            seeds: 1,
            complete: true,
            series: [{ metric: "host.cpu_utilization", points: [{ t: 0, value: 0.4 }], spread: 0 }],
        },
    ],
}

function experiment(state: ExperimentState): Experiment {
    return {
        id: EXPERIMENT_ID,
        projectId: "3f5b1f4a-7c8e-4c3d-9d5a-2f9a1c7b6d21",
        name: "Nightly sweep",
        state,
        spec: {
            topologies: [
                {
                    datacenters: [
                        {
                            clusters: [
                                {
                                    name: "a",
                                    hosts: [{ cpu: { coreCount: 8, coreSpeed: "3 GHz" }, memory: { size: "64 GiB" } }],
                                },
                            ],
                        },
                    ],
                },
            ],
            workloads: [{ type: "trace", source: { type: "named", name: "bitbrains-small" } }],
        },
        specHash: "hash",
        estimate: { scenarioCount: 1, estimatedSimulationSeconds: 1, estimatedBudgetSeconds: 1 },
        createdAt: "2026-08-01T00:00:00Z",
        updatedAt: "2026-08-01T00:00:00Z",
        submittedAt: "2026-08-01T00:00:00Z",
    }
}

const SIGNED = { url: "api/v1/downloads/ticket.signature", expiresAt: "2026-08-01T00:05:00Z" }

function json(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })
}

function show(state: ExperimentState, results: ExperimentResults, archive: Response = json(SIGNED)) {
    const assign = vi.fn()
    vi.stubGlobal("location", { ...window.location, assign })
    const fetch = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) =>
        String(input).endsWith("/archive/link") ? archive : json(results),
    )
    vi.stubGlobal("fetch", fetch)
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MantineProvider theme={theme} defaultColorScheme="light">
                <ResultsPanel experiment={experiment(state)} />
            </MantineProvider>
        </QueryClientProvider>,
    )
    return { assign, fetch }
}

afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
})

/**
 * The parquet an experiment produces is the result; the chart is a reduction of it. Taking the files
 * away has to be reachable from the results view, and has to be followed as a link so the archive
 * streams to disk rather than being assembled in the tab. A plain link cannot carry an access token,
 * so the server signs one first.
 */
describe("downloading an experiment's results", () => {
    it("follows a link the server signs, so a large archive never passes through memory", async () => {
        const { assign, fetch } = show("succeeded", RESULTS)

        fireEvent.click(await screen.findByRole("button", { name: "Download all results" }))

        await waitFor(() => expect(assign).toHaveBeenCalledWith(`/${SIGNED.url}`))
        const [url, init] = fetch.mock.calls.find(([input]) => String(input).endsWith("/archive/link")) ?? []
        expect(String(url)).toBe(`/${resultsArchiveLink(EXPERIMENT_ID)}`)
        expect(init).toMatchObject({ method: "POST" })
    })

    it("goes nowhere when the server has nothing to download", async () => {
        const { assign, fetch } = show("succeeded", RESULTS, json({ status: 404, title: "Results not found" }, 404))

        fireEvent.click(await screen.findByRole("button", { name: "Download all results" }))

        await waitFor(() =>
            expect(fetch.mock.calls.some(([input]) => String(input).endsWith("/archive/link"))).toBe(true),
        )
        expect(assign).not.toHaveBeenCalled()
    })

    it("offers the archive before any samples have arrived, since the files land run by run", async () => {
        show("running", { ...RESULTS, complete: false, scenarios: [] })

        expect(await screen.findByRole("button", { name: "Download all results" })).toBeInTheDocument()
        expect(screen.queryByRole("button", { name: "Export CSV" })).not.toBeInTheDocument()
    })

    it("says nothing about downloading an experiment that has not started, which has written nothing", async () => {
        show("queued", { ...RESULTS, complete: false, scenarios: [] })

        await screen.findByText(/Waiting for the first samples/)
        expect(screen.queryByRole("button", { name: "Download all results" })).not.toBeInTheDocument()
    })

    it("does not offer results for a draft, which has never run", () => {
        show("draft", RESULTS)

        expect(screen.queryByRole("button", { name: "Download all results" })).not.toBeInTheDocument()
    })
})

describe("saving a chart as an image", () => {
    it("says there is nothing to save before the chart has been drawn", async () => {
        const image = { title: "CPU utilization", caption: "", legend: [], fileName: "chart.png" }

        await expect(downloadChartImage(document.createElement("div"), image)).rejects.toThrow(
            "There is no chart to save yet",
        )
    })
})
