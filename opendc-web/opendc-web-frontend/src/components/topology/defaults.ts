import {
    type BatteryPolicy,
    type BatterySpec,
    type ClusterSpec,
    type DataCenterSpec,
    type HostSpec,
    type PowerModelType,
    type PowerSpec,
    type TopologySpec,
    idlePower,
    peakPower,
} from "@/lib/topology/spec"

/** What a fitted model is calibrated to before anybody fits it: the plain curve. */
const STARTER_CALIBRATION_FACTOR = 1

/** Where draw turns close to linear, from the middle of the range the model's authors give. */
const STARTER_ASYMPTOTIC_UTILIZATION = 0.3

export const STARTER_HOST: HostSpec = {
    name: "Host",
    count: 8,
    cpu: { coreCount: 16, coreSpeed: "2.6 GHz", count: 2 },
    memory: { size: "64 GiB" },
    cpuPowerModel: { type: "linear", maxPower: "300 Watts", idlePower: "90 Watts" },
}

export function newCluster(name: string, host: HostSpec = STARTER_HOST): ClusterSpec {
    return { name, hosts: [host] }
}

/** A data center with the simulator's own default supply, which is unlimited. */
export function newDataCenter(name: string): DataCenterSpec {
    return { name, clusters: [] }
}

export function newTopology(): TopologySpec {
    return { datacenters: [{ ...newDataCenter("DC 1"), clusters: [newCluster("Cluster 1")] }] }
}

/** The first of "base 1", "base 2", ... that is not taken, so names stay unique after deletions. */
export function nextName(base: string, taken: string[]): string {
    const used = new Set(taken)
    let index = 1
    while (used.has(`${base} ${index}`)) index++
    return `${base} ${index}`
}

/** [model] as a [type] model, keeping its peak and idle draw; a new type's own fields start at a default. */
export function starterPowerModel(type: PowerModelType, model: PowerSpec): PowerSpec {
    const maxPower = peakPower(model)
    switch (type) {
        case "constant":
            return { type, power: maxPower }
        case "linear":
        case "square":
        case "cubic":
        case "sqrt":
            return { type, maxPower, idlePower: idlePower(model) }
        case "mse":
            return { type, maxPower, idlePower: idlePower(model), calibrationFactor: STARTER_CALIBRATION_FACTOR }
        case "asymptotic":
            return { type, maxPower, idlePower: idlePower(model), asymUtil: STARTER_ASYMPTOTIC_UTILIZATION }
    }
}

export function starterBattery(): BatterySpec {
    return {
        name: "Battery",
        capacity: "100 kWh",
        chargingSpeed: "10000 W",
        initialCharge: "0 kWh",
        policy: starterBatteryPolicy("single"),
    }
}

export const BATTERY_POLICY_TYPES = ["single", "double", "runningMean", "runningMeanPlus"] as const

export function isBatteryPolicyType(type: string): type is BatteryPolicy["type"] {
    return (BATTERY_POLICY_TYPES as readonly string[]).includes(type)
}

export function starterBatteryPolicy(type: BatteryPolicy["type"]): BatteryPolicy {
    switch (type) {
        case "single":
            return { type, carbonThreshold: 150 }
        case "double":
            return { type, lowerThreshold: 100, upperThreshold: 200 }
        case "runningMean":
        case "runningMeanPlus":
            return { type, startingThreshold: 150, windowSize: 24 }
    }
}
