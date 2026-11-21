import { apiRequest, apiText } from "@/lib/api/client"
import type { CarriedOutcome, ExitReason, Handle, Page, PlanTier } from "@/lib/api/types"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

const ENDED_STATES = ["succeeded", "failed", "cancelled"] as const

type EndedState = (typeof ENDED_STATES)[number]

export const EXECUTION_STATES = ["queued", "submitted", "running", ...ENDED_STATES] as const

export type ExecutionState = (typeof EXECUTION_STATES)[number]

export type ExecutionPhase =
    | { type: "queued" }
    | { type: "submitted"; submittedAt: string }
    | { type: "running"; startedAt: string; elapsedSeconds: number; straggler: boolean }
    | { type: "ended"; state: EndedState; settledAt: string; reason: ExitReason; message: string }

export interface AdminExecution {
    id: string
    experimentId: string
    experimentName: string
    projectName: string
    owner: string
    dispatcher: string
    attempt: number
    cores: number
    memoryRequestMb: number
    timeLimitSeconds: number
    unitCount: number
    scenarios: number[]
    estimatedMakespanSeconds: number
    createdAt: string
    phase: ExecutionPhase
}

export interface CarriedUnit {
    scenarioIndex: number
    seed: number
    estimatedSeconds: number
    estimatedPeakMemoryMb: number
    outcome: CarriedOutcome
}

export interface AdminExecutionDetail {
    execution: AdminExecution
    units: CarriedUnit[]
    retryableUnits: number
}

export interface PlatformCapacity {
    dispatcher: string
    totalCores: number
    allocatedCores: number
    totalMemoryMb: number
    allocatedMemoryMb: number
    slotCores: number
    slotMemoryMb: number
    slotTimeCap: { type: "unlimited" } | { type: "limited"; seconds: number }
}

export interface AdminAccount {
    /** Who the identity provider says this is, which is what `OPENDC_AUTH_ADMINS` lists. */
    subject: string
    handle: Handle
    displayName: string
    plan: PlanTier
    isAdmin: boolean
    status: { type: "active" } | { type: "deactivated"; at: string }
    createdAt: string
    projectCount: number
}

/** Which executions to list: the live ones, every one, or those in one state. */
export type StateFilter = "live" | "all" | ExecutionState

export function statesOf(filter: StateFilter): readonly ExecutionState[] {
    if (filter === "live") return []
    if (filter === "all") return EXECUTION_STATES
    return [filter]
}

export const adminKeys = {
    executions: (filter: StateFilter, page: number) => ["admin", "executions", filter, page] as const,
    execution: (id: string) => ["admin", "execution", id] as const,
    log: (id: string) => ["admin", "execution", id, "log"] as const,
    capacity: ["admin", "capacity"] as const,
    users: (q: string, page: number) => ["admin", "users", q, page] as const,
}

export const ADMIN_PAGE_SIZE = 50

const LIVE_EXECUTIONS_POLL_MS = 5000

const CAPACITY_POLL_MS = 10_000

export function useAdminExecutions(filter: StateFilter, page: number) {
    const params = new URLSearchParams(statesOf(filter).map((state) => ["state", state]))
    params.set("limit", String(ADMIN_PAGE_SIZE))
    params.set("offset", String((page - 1) * ADMIN_PAGE_SIZE))
    return useQuery({
        queryKey: adminKeys.executions(filter, page),
        queryFn: () => apiRequest<Page<AdminExecution>>(`api/v1/admin/executions?${params}`),
        refetchInterval: filter === "live" ? LIVE_EXECUTIONS_POLL_MS : false,
    })
}

export function useAdminExecution(id: string | undefined) {
    return useQuery({
        queryKey: adminKeys.execution(id ?? ""),
        queryFn: () => apiRequest<AdminExecutionDetail>(`api/v1/admin/executions/${id}`),
        enabled: id !== undefined,
    })
}

/** What the launcher wrote, which exists only once the execution has ended. */
export function useExecutionLog(id: string, enabled: boolean) {
    return useQuery({
        queryKey: adminKeys.log(id),
        queryFn: () => apiText(`api/v1/admin/executions/${id}/logs`),
        enabled,
        retry: false,
    })
}

export function useRetryExecution() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (id: string) =>
            apiRequest<AdminExecution[]>(`api/v1/admin/executions/${id}/retry`, { method: "POST" }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin"] }),
    })
}

export function useAdminCapacity() {
    return useQuery({
        queryKey: adminKeys.capacity,
        queryFn: () => apiRequest<PlatformCapacity>("api/v1/admin/capacity"),
        refetchInterval: CAPACITY_POLL_MS,
    })
}

export function useAdminAccounts(q: string, page: number) {
    const params = new URLSearchParams({
        q,
        limit: String(ADMIN_PAGE_SIZE),
        offset: String((page - 1) * ADMIN_PAGE_SIZE),
    })
    return useQuery({
        queryKey: adminKeys.users(q, page),
        queryFn: () => apiRequest<Page<AdminAccount>>(`api/v1/admin/users?${params}`),
    })
}
