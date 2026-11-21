export type QuantityKind = "frequency" | "dataSize" | "dataRate" | "power" | "energy" | "time"

/** A measurement as the model stores it: a number in the base unit, or text naming its own unit. */
export type Quantity = number | string

const SECOND_MS = 1e3
const MINUTE_MS = 60_000
const HOUR_MS = 3.6e6
const DAY_MS = 86_400_000

const WATT_HOUR_J = 3600

/**
 * Every unit a quantity may be written in, largest first, with how many base units it is worth. The
 * only source of unit names, for both the type a field accepts and the ladder a value is shown on.
 */
const LADDER = {
    frequency: [
        ["GHz", 1e3],
        ["MHz", 1],
        ["kHz", 1e-3],
        ["Hz", 1e-6],
    ],
    dataSize: [
        ["TiB", 1024 ** 2],
        ["GiB", 1024],
        ["MiB", 1],
        ["KiB", 1 / 1024],
    ],
    dataRate: [
        ["Gibps", 1024 ** 2],
        ["Mibps", 1024],
        ["Kibps", 1],
    ],
    power: [
        ["kW", 1e3],
        ["W", 1],
    ],
    energy: [
        ["MWh", WATT_HOUR_J * 1e6],
        ["kWh", WATT_HOUR_J * 1e3],
        ["Wh", WATT_HOUR_J],
        ["kJ", 1e3],
        ["J", 1],
    ],
    time: [
        ["h", HOUR_MS],
        ["min", MINUTE_MS],
        ["s", SECOND_MS],
        ["ms", 1],
    ],
} as const satisfies Record<QuantityKind, ReadonlyArray<readonly [string, number]>>

/** A unit a quantity of kind [K] may be written in. */
export type Unit<K extends QuantityKind = QuantityKind> = (typeof LADDER)[K][number][0]

/** What a bare-number field is counted in; the unit never travels in the document. */
export const SCALAR_UNIT = {
    carbonIntensity: "gCO2/kWh",
} as const

/** Anything the editor prints beside a number. */
export type UnitLabel = Unit | (typeof SCALAR_UNIT)[keyof typeof SCALAR_UNIT]

/** The one unit a measurement field reads and writes, printed beside a plain number. */
export const DEFAULT_UNIT: { [K in QuantityKind]: Unit<K> } = {
    frequency: "GHz",
    dataSize: "GiB",
    dataRate: "Gibps",
    power: "W",
    energy: "kWh",
    time: "min",
}

/** The unit a bare number is counted in, which is what the model assumes when text names none. */
const BASE_UNIT: { [K in QuantityKind]: Unit<K> } = {
    frequency: "MHz",
    dataSize: "MiB",
    dataRate: "Kibps",
    power: "W",
    energy: "J",
    time: "ms",
}

export type QuantityValue = { status: "ok"; base: number } | { status: "unspecified" } | { status: "invalid" }

/**
 * Reads a stored measurement into its base unit, accepting every spelling the backend does: values
 * also come from the SDK's formatter ("10.000000 KWatts") and from imported documents.
 */
export function parseQuantity(kind: QuantityKind, wire: Quantity): QuantityValue {
    if (typeof wire === "number") {
        return Number.isFinite(wire) ? classify(wire) : { status: "invalid" }
    }
    if (kind === "time") {
        const iso = isoDuration(wire)
        if (iso !== undefined) return classify(iso)
    }
    const parts = NUMBER_AND_SUFFIX.exec(wire)
    if (!parts) return { status: "invalid" }
    const amount = Number(parts[1])
    const suffix = parts[2] ?? ""
    if (!Number.isFinite(amount)) return { status: "invalid" }
    if (suffix === "") return classify(amount)

    const factor = factorOf(kind, suffix)
    if (factor === undefined) return { status: "invalid" }
    return classify(amount * factor)
}

// The model reads any negative value as "never specified".
function classify(base: number): QuantityValue {
    return base < 0 ? { status: "unspecified" } : { status: "ok", base }
}

const NUMBER_AND_SUFFIX = /^\s*([\d.e-]+)\s*(.*?)\s*$/

// TimeDelta serializes through Duration.toString(), so a "5 min" saved from here returns as "PT5M".
const ISO_DURATION = /^P(?:([\d.]+)D)?(?:T(?:([\d.]+)H)?(?:([\d.]+)M)?(?:([\d.]+)S)?)?$/

function isoDuration(wire: string): number | undefined {
    const parts = ISO_DURATION.exec(wire.trim())
    if (parts === null) return undefined
    const [, days, hours, minutes, seconds] = parts
    // "P" and "PT" carry no amount at all, which is a malformed duration rather than a zero one.
    if (days === undefined && hours === undefined && minutes === undefined && seconds === undefined) return undefined
    return (
        Number(days ?? 0) * DAY_MS +
        Number(hours ?? 0) * HOUR_MS +
        Number(minutes ?? 0) * MINUTE_MS +
        Number(seconds ?? 0) * SECOND_MS
    )
}

/**
 * Every duration spelling the backend's parser matches, in milliseconds. Written out rather than
 * built from prefixes because "m" is minutes but "ms" is milliseconds.
 */
const TIME_UNITS = new Map<string, number>([
    ["ms", 1],
    ["milli", 1],
    ["millis", 1],
    ["millisecond", 1],
    ["milliseconds", 1],
    ["s", SECOND_MS],
    ["sec", SECOND_MS],
    ["secs", SECOND_MS],
    ["second", SECOND_MS],
    ["seconds", SECOND_MS],
    ["m", MINUTE_MS],
    ["min", MINUTE_MS],
    ["mins", MINUTE_MS],
    ["minute", MINUTE_MS],
    ["minutes", MINUTE_MS],
    ["h", HOUR_MS],
    ["hour", HOUR_MS],
    ["hours", HOUR_MS],
])

const PER_SECOND = /^(.*?)\s*(?:p|per|\/)\s*(?:s|sec|Sec|second|Second)\s*$/

const BIT_UNITS = ["b", "bit", "bits", "Bit", "Bits"]
const BYTE_UNITS = ["B", "byte", "bytes", "Byte", "Bytes"]

// Longest first, so "Kibi" is tried before "Ki" before "K". Every prefix is matched in either
// case; only the unit itself is case-sensitive, where "B" is bytes and "b" is bits.
const SIZE_PREFIX_SCALES = [
    ["Kibi", 1024],
    ["Mebi", 1024 ** 2],
    ["Gibi", 1024 ** 3],
    ["Tebi", 1024 ** 4],
    ["Kilo", 1e3],
    ["Mega", 1e6],
    ["Giga", 1e9],
    ["Tera", 1e12],
    ["Ki", 1024],
    ["Mi", 1024 ** 2],
    ["Gi", 1024 ** 3],
    ["Ti", 1024 ** 4],
    ["K", 1e3],
    ["M", 1e6],
    ["G", 1e9],
    ["T", 1e12],
] as const

const SIZE_PREFIXES: ReadonlyArray<readonly [string, number]> = [
    ...SIZE_PREFIX_SCALES.flatMap(([name, scale]) => [[name, scale] as const, [name.toLowerCase(), scale] as const]),
    ["", 1],
]

// Hertz and watts are matched case-insensitively, so these need only one spelling each.
const HERTZ_PREFIXES: ReadonlyArray<readonly [string, number]> = [
    ["kilo", 1e3],
    ["mega", 1e6],
    ["giga", 1e9],
    ["k", 1e3],
    ["m", 1e6],
    ["g", 1e9],
    ["", 1],
]

const WATT_PREFIXES: ReadonlyArray<readonly [string, number]> = [
    ["kilo", 1e3],
    ["k", 1e3],
    ["", 1],
]

// Case-sensitive, longest first: a lone "m" is milli, which the backend refuses for energy, not mega.
const ENERGY_PREFIXES: ReadonlyArray<readonly [string, number]> = [
    ["Giga", 1e9],
    ["giga", 1e9],
    ["Mega", 1e6],
    ["mega", 1e6],
    ["Kilo", 1e3],
    ["kilo", 1e3],
    ["G", 1e9],
    ["g", 1e9],
    ["M", 1e6],
    ["K", 1e3],
    ["k", 1e3],
    ["", 1],
]

const JOULE = /^(?:j|joules?)$/i
const WATT_HOUR = /^(?:w|watts?)\s*(?:-|p|per|\/)?\s*(?:h|hours?)$/i

/** Base units per one of [suffix], or undefined when the backend would reject it too. */
function factorOf(kind: QuantityKind, suffix: string): number | undefined {
    switch (kind) {
        case "frequency":
            return scaledUnit(suffix.toLowerCase(), ["hz", "hertz"], HERTZ_PREFIXES, 1e-6)
        case "power":
            return scaledUnit(suffix.toLowerCase(), ["w", "watts"], WATT_PREFIXES, 1)
        case "energy":
            return energyFactor(suffix)
        case "dataSize":
            return dataFactor(suffix, 1 / 1024 ** 2)
        case "dataRate": {
            const rate = PER_SECOND.exec(suffix)
            return rate === null ? undefined : dataFactor(rate[1] ?? "", 8 / 1024)
        }
        case "time":
            return TIME_UNITS.get(suffix.toLowerCase())
    }
}

function scaledUnit(
    suffix: string,
    units: string[],
    prefixes: ReadonlyArray<readonly [string, number]>,
    unitInBase: number,
): number | undefined {
    for (const unit of units) {
        if (!suffix.endsWith(unit)) continue
        const prefix = suffix.slice(0, suffix.length - unit.length)
        const scale = prefixes.find(([name]) => name === prefix)
        if (scale) return scale[1] * unitInBase
    }
    return undefined
}

function energyFactor(suffix: string): number | undefined {
    for (const [prefix, scale] of ENERGY_PREFIXES) {
        if (!suffix.startsWith(prefix)) continue
        const unit = suffix.slice(prefix.length).trim()
        if (JOULE.test(unit)) return scale
        if (WATT_HOUR.test(unit)) return scale * WATT_HOUR_J
    }
    return undefined
}

function dataFactor(suffix: string, baseUnitsPerByte: number): number | undefined {
    for (const [prefix, scale] of SIZE_PREFIXES) {
        if (!suffix.startsWith(prefix)) continue
        const unit = suffix.slice(prefix.length)
        if (BYTE_UNITS.includes(unit)) return scale * baseUnitsPerByte
        if (BIT_UNITS.includes(unit)) return (scale / 8) * baseUnitsPerByte
    }
    return undefined
}

/** Renders [base] on the largest unit that keeps the number short, for read-only display. */
export function formatQuantity(kind: QuantityKind, base: number): string {
    if (!Number.isFinite(base)) return `0 ${BASE_UNIT[kind]}`
    const unit = naturalUnit(kind, base)
    return `${round(base / unitScale(kind, unit))} ${unit}`
}

/**
 * Renders a measurement as the model stores it, which is text in whichever unit its author wrote.
 * Anything unreadable is shown as it was stored rather than as an invented number.
 */
export function formatWire(kind: QuantityKind, wire: Quantity): string {
    const parsed = parseQuantity(kind, wire)
    return parsed.status === "ok" ? formatQuantity(kind, parsed.base) : String(wire)
}

/** Counts [base] in [unit], for a field the reader is about to edit. */
export function amountIn<K extends QuantityKind>(kind: K, base: number, unit: Unit<K>): number {
    return trim(base / unitScale(kind, unit))
}

function naturalUnit<K extends QuantityKind>(kind: K, base: number): Unit<K> {
    const ladder = ladderOf(kind)
    const step = ladder.find(([, scale]) => Math.abs(base) >= scale)
    if (step !== undefined) return step[0]
    // Smaller than every step, zero included: show it on the smallest unit.
    const smallest = ladder[ladder.length - 1]
    return smallest === undefined ? BASE_UNIT[kind] : smallest[0]
}

function unitScale<K extends QuantityKind>(kind: K, unit: Unit<K>): number {
    return ladderOf(kind).find(([name]) => name === unit)?.[1] ?? 1
}

function ladderOf<K extends QuantityKind>(kind: K): ReadonlyArray<readonly [Unit<K>, number]> {
    return LADDER[kind]
}

// Two decimals is all a read-only summary needs.
function round(value: number): string {
    return String(Math.round(value * 100) / 100)
}

// Dividing by a scale turns an exact 2600 MHz into 2.5999999999999996 GHz. Twelve significant
// digits is far more than any field here carries, so this drops the artefact and nothing else.
function trim(value: number): number {
    return Number(value.toPrecision(12))
}
