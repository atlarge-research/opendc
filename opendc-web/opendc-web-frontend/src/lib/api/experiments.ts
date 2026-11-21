import { apiRequest, apiText } from "@/lib/api/client"
import type {
    Experiment,
    ExperimentPreview,
    ExperimentStatus,
    ExperimentSummary,
    Id,
    Page,
    ScenarioDetail,
    ScenarioStatus,
} from "@/lib/api/types"
import { type ExperimentResults, isLiveResults } from "@/lib/experiment/results"
import type { ExperimentSpec } from "@/lib/experiment/spec"
import { isTerminalExperiment } from "@/lib/experiment/status"
import { type QueryClient, keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

const LIVE_POLL_MS = 2000

// An experiment is keyed by its own id alone; the project only scopes the list it appears in.
export const experimentKeys = {
    list: (projectId: Id) => ["projects", projectId, "experiments"] as const,
    page: (projectId: Id, page: number) => ["projects", projectId, "experiments", page] as const,
    detail: (experimentId: Id) => ["experiments", experimentId] as const,
    status: (experimentId: Id) => ["experiments", experimentId, "status"] as const,
    results: (experimentId: Id) => ["experiments", experimentId, "results"] as const,
}

export interface ExperimentChange {
    name: string
    spec: ExperimentSpec
}

export const EXPERIMENT_PAGE_SIZE = 20

/** One page of a project's experiments, newest first, counting pages from 1. */
export function useExperiments(projectId: Id, page: number) {
    const offset = (page - 1) * EXPERIMENT_PAGE_SIZE
    return useQuery({
        queryKey: experimentKeys.page(projectId, page),
        queryFn: () =>
            apiRequest<Page<ExperimentSummary>>(
                `api/v1/experiments?project=${projectId}&limit=${EXPERIMENT_PAGE_SIZE}&offset=${offset}`,
            ),
        placeholderData: keepPreviousData,
        refetchInterval: (query) => (query.state.data?.items.some(isLive) ? LIVE_POLL_MS : false),
        retry: false,
    })
}

export function useExperiment(experimentId: Id) {
    return useQuery({
        queryKey: experimentKeys.detail(experimentId),
        queryFn: () => apiRequest<Experiment>(`api/v1/experiments/${experimentId}`),
        refetchInterval: (query) => (query.state.data && isLive(query.state.data) ? LIVE_POLL_MS : false),
        retry: false,
    })
}

export function useExperimentStatus(experimentId: Id) {
    return useQuery({
        queryKey: experimentKeys.status(experimentId),
        queryFn: () => apiRequest<ExperimentStatus>(`api/v1/experiments/${experimentId}/status`),
        refetchInterval: (query) => (query.state.data && isLive(query.state.data) ? LIVE_POLL_MS : false),
        retry: false,
    })
}

function isLive(entry: { state: ExperimentSummary["state"] }): boolean {
    return entry.state !== "draft" && !isTerminalExperiment(entry.state)
}

/** One scenario's runs and attempts, polled while it is still going. */
export function useScenario(experimentId: Id, scenarioIndex: number | undefined) {
    return useQuery({
        queryKey: [...experimentKeys.detail(experimentId), "scenarios", scenarioIndex] as const,
        queryFn: () => apiRequest<ScenarioDetail>(`api/v1/experiments/${experimentId}/scenarios/${scenarioIndex}`),
        enabled: scenarioIndex !== undefined,
        refetchInterval: (query) => {
            const state = query.state.data?.status.state
            return state === "queued" || state === "running" ? LIVE_POLL_MS : false
        },
    })
}

/** What a scenario's runs wrote to the launcher log; there is none until an attempt has ended. */
export function useScenarioLog(experimentId: Id, scenarioIndex: number, attempts: number) {
    return useQuery({
        // Keyed by how many attempts have ended, so a newly collected log is fetched once it exists.
        queryKey: [...experimentKeys.detail(experimentId), "scenarios", scenarioIndex, "log", attempts] as const,
        queryFn: () => apiText(`api/v1/experiments/${experimentId}/scenarios/${scenarioIndex}/logs`),
        enabled: attempts > 0,
        retry: false,
    })
}

export function useExperimentResults(experimentId: Id) {
    return useQuery({
        queryKey: experimentKeys.results(experimentId),
        queryFn: () => apiRequest<ExperimentResults>(`api/v1/experiments/${experimentId}/results`),
        refetchInterval: (query) => (query.state.data && isLiveResults(query.state.data) ? LIVE_POLL_MS : false),
        retry: false,
    })
}

/** Where the server signs a link to everything an experiment's runs produced, as one zip. */
export function resultsArchiveLink(experimentId: Id): string {
    return `api/v1/experiments/${experimentId}/archive/link`
}

export function useCreateExperiment(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (change: ExperimentChange) =>
            apiRequest<Experiment>("api/v1/experiments", {
                method: "POST",
                body: { projectId, name: change.name, spec: change.spec },
            }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: experimentKeys.list(projectId) }),
    })
}

export function useSaveExperimentDraft(experimentId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (change: ExperimentChange) =>
            apiRequest<Experiment>(`api/v1/experiments/${experimentId}`, { method: "PUT", body: change }),
        onSuccess: (experiment) => {
            queryClient.setQueryData(experimentKeys.detail(experimentId), experiment)
            queryClient.invalidateQueries({ queryKey: experimentKeys.list(experiment.projectId) })
        },
    })
}

export function usePreviewExperiment() {
    return useMutation({
        mutationFn: (spec: ExperimentSpec) =>
            apiRequest<ExperimentPreview>("api/v1/experiments/preview", { method: "POST", body: { spec } }),
    })
}

export function useSubmitExperiment(experimentId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: () => apiRequest<Experiment>(`api/v1/experiments/${experimentId}/submit`, { method: "POST" }),
        onSuccess: (experiment) => applyExperiment(queryClient, experiment),
    })
}

export function useCancelExperiment(experimentId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: () => apiRequest<Experiment>(`api/v1/experiments/${experimentId}/cancel`, { method: "POST" }),
        onSuccess: (experiment) => applyExperiment(queryClient, experiment),
    })
}

export function useRetryScenario(experimentId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (scenarioIndex: number) =>
            apiRequest<ScenarioStatus>(`api/v1/experiments/${experimentId}/scenarios/${scenarioIndex}/retry`, {
                method: "POST",
            }),
        // The detail key is a prefix of the status and results keys, so one invalidation reaches
        // the panel that has to start polling again.
        onSuccess: () => queryClient.invalidateQueries({ queryKey: experimentKeys.detail(experimentId) }),
    })
}

/**
 * Takes the experiment a write returned. Status and results are invalidated because their cached
 * copies still say draft, and a draft does not poll.
 */
function applyExperiment(queryClient: QueryClient, experiment: Experiment): void {
    queryClient.setQueryData(experimentKeys.detail(experiment.id), experiment)
    queryClient.invalidateQueries({ queryKey: experimentKeys.status(experiment.id) })
    queryClient.invalidateQueries({ queryKey: experimentKeys.results(experiment.id) })
    queryClient.invalidateQueries({ queryKey: experimentKeys.list(experiment.projectId) })
}

export function useCloneExperiment(experimentId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: () => apiRequest<Experiment>(`api/v1/experiments/${experimentId}/clone`, { method: "POST" }),
        onSuccess: (draft) => queryClient.invalidateQueries({ queryKey: experimentKeys.list(draft.projectId) }),
    })
}

export function useDeleteExperiment(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (experimentId: Id) => apiRequest<void>(`api/v1/experiments/${experimentId}`, { method: "DELETE" }),
        // Removed rather than invalidated, or the refetch's 404 reaches the page before the redirect.
        // The detail key is a prefix of the status and results keys, so this covers all three.
        onSuccess: (_result, experimentId) => {
            queryClient.removeQueries({ queryKey: experimentKeys.detail(experimentId) })
            queryClient.invalidateQueries({ queryKey: experimentKeys.list(projectId) })
        },
    })
}
