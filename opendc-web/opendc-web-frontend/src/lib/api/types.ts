import type { ExperimentSpec } from "@/lib/experiment/spec"
import type { ExperimentState, ProgressReport, ScenarioExecutionState } from "@/lib/experiment/status"
import type { HostSpec, TopologySpec } from "@/lib/topology/spec"

export interface DocumentIssue {
    path: string
    message: string
}

export interface ApiProblem {
    status: number
    title: string
    detail?: string
    issues: DocumentIssue[]
}

export type ProjectRole = "owner" | "editor" | "viewer"

// Identifiers are opaque strings issued by the server (uuids). Nothing on the client parses,
// orders or does arithmetic on them; they are only ever passed back.
export type Id = string

export interface Project {
    id: Id
    name: string
    role: ProjectRole
    createdAt: string
    updatedAt: string
}

export type ExitReason =
    | "ok"
    | "simulationError"
    | "invalidSpec"
    | "oom"
    | "timeout"
    | "walltime"
    | "cancelled"
    | "rejected"
    | "unknown"

export interface ScenarioExit {
    exitCode: number
    reason: ExitReason
    message?: string
}

export interface CostEstimate {
    scenarioCount: number
    estimatedSimulationSeconds: number
    estimatedBudgetSeconds: number
}

export interface ExperimentPreview {
    scenarioCount: number
    estimate: CostEstimate
    issues: DocumentIssue[]
}

export interface Experiment {
    id: Id
    projectId: Id
    name: string
    state: ExperimentState
    spec: ExperimentSpec
    specHash: string
    estimate: CostEstimate
    createdAt: string
    updatedAt: string
    submittedAt?: string
}

export interface ExperimentSummary {
    id: Id
    name: string
    state: ExperimentState
    scenarioCount: number
    progress: ProgressReport
    createdAt: string
    submittedAt?: string
}

export interface ScenarioStatus {
    scenarioIndex: number
    state: ScenarioExecutionState
    completedTasks: number
    totalTasks: number
    attempt: number
    exitInfo?: ScenarioExit
}

/** One window of a longer list, and how long the whole list is. */
export interface Page<T> {
    items: T[]
    total: number
}

/** How one execution's attempt at a unit went. */
export type CarriedOutcome =
    | { type: "carried" }
    | { type: "succeeded" }
    | { type: "failed"; reason: ExitReason; message: string }
    | { type: "cancelled" }

export interface RunAttempt {
    executionId: Id
    attempt: number
    outcome: CarriedOutcome
}

export interface OutputFile {
    name: string
    sizeBytes: number
}

/** One seed of a scenario: every attempt at it, and the files it left once it landed. */
export interface ScenarioRun {
    seed: number
    state: ScenarioExecutionState
    attempts: RunAttempt[]
    files: OutputFile[]
}

export interface ScenarioDetail {
    status: ScenarioStatus
    runs: ScenarioRun[]
}

export interface ExperimentStatus {
    id: Id
    name: string
    state: ExperimentState
    completedTasks: number
    totalTasks: number
    scenarioCount: number
    scenarios: ScenarioStatus[]
}

export interface TopologyTemplate {
    id: Id
    projectId: Id
    name: string
    topology: TopologySpec
    topologyHash: string
    /** Opaque to the server and validated by the editor, which lays out afresh what it cannot read. */
    layout?: unknown
    createdAt: string
    updatedAt: string
}

export type TraceKind = "workload" | "carbon" | "failure"

/**
 * How you came by a trace, which is what decides whether you may change it: the deployment's own
 * are nobody's to edit, and one shared with you belongs to somebody else.
 */
export type TraceAccess = "builtin" | "owned" | "shared"

/** One table of a trace. Rows here means rows of the parquet file, of which there are many. */
export interface TraceTable {
    name: string
    sizeBytes: number
    rowCount?: number
}

export interface Trace {
    id: Id
    slug: string
    kind: TraceKind
    access: TraceAccess
    description?: string
    sizeBytes: number
    tables: TraceTable[]
    createdAt: string
}

export type ImportProgress =
    | { type: "running"; since: string }
    | { type: "succeeded"; at: string }
    | { type: "failed"; at: string; reason: string }

/** A trace the server is fetching from URLs, which joins the library once every table has landed. */
export interface TraceImport {
    id: Id
    traceId: Id
    slug: string
    kind: TraceKind
    progress: ImportProgress
}

/** Which tables a kind of trace is made of, so an upload form need not carry its own copy. */
export interface TraceKindTables {
    kind: TraceKind
    tables: string[]
}

/** One stretch of a table's file, and the target that takes exactly that stretch. */
export interface UploadPart {
    url: string
    offset: number
    length: number
}

/**
 * Where one table's bytes go. Direct means object storage takes them without touching the API, and
 * more than one part means they may go at once.
 */
export interface UploadSlot {
    table: string
    parts: UploadPart[]
    direct: boolean
}

export interface RegisteredTrace {
    trace: Trace
    uploads: UploadSlot[]
}

export interface TraceShare {
    handle: string
    displayName: string
}

export type CatalogName = "schedulers" | "failure-prefabs" | "power-models" | "battery-policies" | "export-columns"

export interface CatalogEntry {
    id: string
    label: string
    group: string
    description?: string
}

export interface HostTemplate {
    id: string
    label: string
    group: string
    host: HostSpec
}

export type PlanTier = "free" | "education" | "enterprise"

export type BudgetPeriod = "session" | "week"

// Unlimited is a deliberate grant, held by accounts raised by hand, rather than the absence of a
// limit.
export type SimulationCap = { type: "limited"; seconds: number } | { type: "unlimited" }

export interface BudgetWindow {
    period: BudgetPeriod
    usedSeconds: number
    reservedSeconds: number
    cap: SimulationCap
    resetsAt: string
}

export interface Invoice {
    id: string
    issuedAt: string
    amountEur: number
    paid: boolean
}

// Renewal and payment method only exist once a billing provider is wired up; until then the server
// sends neither rather than sending placeholders.
export interface Billing {
    renewsAt?: string
    paymentMethod?: string
    invoices: Invoice[]
}

/** The name other people see, or that there is none yet: a first sign-in has to choose one. */
export type Handle = { type: "provisional" } | { type: "chosen"; name: string }

/** How this deployment signs people in, which the app learns at runtime rather than at build. */
export type AuthSettings = { type: "auth0"; domain: string; clientId: string; audience: string } | { type: "anonymous" }

export interface DeploymentConfig {
    auth: AuthSettings
}

export interface UserProfile {
    displayName: string
    handle: Handle
    plan: PlanTier
    isAdmin: boolean
    projectCount: number
    budgets: BudgetWindow[]
}

export type TokenUse = { type: "unused" } | { type: "used"; at: string }

export interface AccessToken {
    id: string
    name: string
    prefix: string
    createdAt: string
    lastUse: TokenUse
}

/** A token as it is minted, the only time its secret is ever shown. */
export interface MintedToken {
    token: AccessToken
    secret: string
}

/** A short-lived link a browser follows to download something without an access token. */
export interface DownloadLink {
    url: string
    expiresAt: string
}

export type Account = Pick<UserProfile, "plan" | "projectCount" | "budgets" | "isAdmin">
