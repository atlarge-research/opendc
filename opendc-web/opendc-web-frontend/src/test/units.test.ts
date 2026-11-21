import { DEFAULT_UNIT, type QuantityKind, amountIn, formatQuantity, formatWire, parseQuantity } from "@/lib/units"
import { describe, expect, it } from "vitest"

const KINDS: QuantityKind[] = ["frequency", "dataSize", "dataRate", "power", "energy", "time"]

function base(kind: QuantityKind, wire: string | number): number {
    const parsed = parseQuantity(kind, wire)
    if (parsed.status !== "ok") throw new Error(`expected ${wire} to parse, got ${parsed.status}`)
    return parsed.base
}

describe("durations", () => {
    it("reads the ISO-8601 the backend writes them back as", () => {
        expect(base("time", "PT5M")).toBe(300_000)
        expect(base("time", "PT1H")).toBe(3.6e6)
        expect(base("time", "PT1H30M")).toBe(5.4e6)
        expect(base("time", "PT0.5S")).toBe(500)
        expect(base("time", "P1DT2H")).toBe(93.6e6)
    })

    it("keeps minutes and milliseconds apart the way the backend's parser does", () => {
        expect(base("time", "5 m")).toBe(300_000)
        expect(base("time", "5 ms")).toBe(5)
        expect(base("time", "PT5M")).toBe(base("time", "5 min"))
    })

    it("reads a bare number as milliseconds, which is what TimeDelta assumes", () => {
        expect(base("time", 300_000)).toBe(300_000)
    })

    it("rejects text that names no duration rather than guessing at one", () => {
        expect(parseQuantity("time", "P").status).toBe("invalid")
        expect(parseQuantity("time", "PT").status).toBe("invalid")
        expect(parseQuantity("time", "5 fortnights").status).toBe("invalid")
        expect(parseQuantity("time", "soon").status).toBe("invalid")
    })

    it("counts a stored duration in the unit a field asks for, whatever it was written in", () => {
        expect(amountIn("time", base("time", "PT5M"), "min")).toBe(5)
        expect(amountIn("time", base("time", "90 s"), "min")).toBe(1.5)
        expect(amountIn("time", base("time", "PT2H"), "min")).toBe(120)
    })

    it("shows a stored duration in a unit a reader expects, ISO-8601 included", () => {
        expect(formatWire("time", "PT5M")).toBe("5 min")
        expect(formatWire("time", 3.6e6)).toBe("1 h")
        // Nothing readable in it, so it is shown as it was stored rather than as an invented number.
        expect(formatWire("time", "whenever")).toBe("whenever")
    })
})

describe("energy", () => {
    // A battery's capacity once meant kilowatt-hours as a bare number; the backend now counts it in joules.
    it("reads a bare number as joules and the joules the backend writes back", () => {
        expect(base("energy", 100)).toBe(100)
        expect(base("energy", "360000000 J")).toBe(3.6e8)
        expect(base("energy", "100 kWh")).toBe(3.6e8)
    })

    it("reads every watt-hour and joule spelling the backend does", () => {
        expect(base("energy", "1 MWh")).toBe(3.6e9)
        expect(base("energy", "2 Wh")).toBe(7200)
        expect(base("energy", "1 kW/h")).toBe(3.6e6)
        expect(base("energy", "3 MJ")).toBe(3e6)
        expect(base("energy", "1 GJ")).toBe(1e9)
        expect(base("energy", "5 kilojoules")).toBe(5000)
    })

    // The backend reads a lower-case m as milli, which it refuses, so this must not become mega.
    it("rejects milli rather than reading it as mega", () => {
        expect(parseQuantity("energy", "1 mWh").status).toBe("invalid")
        expect(parseQuantity("energy", "1 mJ").status).toBe("invalid")
        expect(parseQuantity("energy", "1 kW").status).toBe("invalid")
    })
})

describe("parseQuantity", () => {
    it("reads a bare number in the implicit base unit the SDK assumes", () => {
        expect(base("frequency", 3200)).toBe(3200)
        expect(base("dataSize", 128)).toBe(128)
        expect(base("power", 400)).toBe(400)
        expect(base("dataRate", 1600)).toBe(1600)
    })

    it("treats data-size case as significant, because the backend does", () => {
        expect(base("dataSize", "1 GiB")).toBe(1024)
        expect(base("dataSize", "1 gib")).toBe(1024 / 8)
        expect(base("dataSize", "1 GB")).toBe(1e9 / 1024 ** 2)
        expect(base("dataSize", "1 Gb")).toBe(1e9 / 8 / 1024 ** 2)
    })

    it("ignores case for frequency and power, because the backend does", () => {
        expect(base("frequency", "3.2 ghz")).toBe(base("frequency", "3.2 GHz"))
        expect(base("frequency", "2megahertz")).toBe(2)
        expect(base("power", "50 KWATTS")).toBe(base("power", "50 kW"))
    })

    it("rejects units the backend does not accept, rather than guessing", () => {
        expect(parseQuantity("power", "5 MW").status).toBe("invalid")
        expect(parseQuantity("frequency", "1 THz").status).toBe("invalid")
        expect(parseQuantity("power", "1 watt").status).toBe("invalid")
        expect(parseQuantity("dataSize", "1 PiB").status).toBe("invalid")
    })

    it("rejects malformed input", () => {
        expect(parseQuantity("frequency", "1.2.3 GHz").status).toBe("invalid")
        expect(parseQuantity("frequency", "fast").status).toBe("invalid")
        expect(parseQuantity("frequency", "").status).toBe("invalid")
        expect(parseQuantity("dataSize", "GiB").status).toBe("invalid")
        expect(parseQuantity("power", Number.NaN).status).toBe("invalid")
    })

    it("matches the backend's numeric grammar, which has no exponent sign", () => {
        expect(base("frequency", "1e3 MHz")).toBe(1000)
        expect(parseQuantity("frequency", "1e+3 MHz").status).toBe("invalid")
        expect(parseQuantity("frequency", "1E3 MHz").status).toBe("invalid")
    })

    it("reports the -1 sentinel as unspecified instead of a real measurement", () => {
        expect(parseQuantity("frequency", -1).status).toBe("unspecified")
        expect(parseQuantity("dataSize", -1).status).toBe("unspecified")
        expect(parseQuantity("dataRate", -1).status).toBe("unspecified")
        expect(parseQuantity("dataSize", "-1 GiB").status).toBe("unspecified")
    })

    it("tolerates the whitespace the backend tolerates", () => {
        expect(base("frequency", "  10    GHz   ")).toBe(10_000)
        expect(base("dataSize", "128GiB")).toBe(131_072)
    })

    it("understands every per-second spelling for data rates", () => {
        expect(base("dataRate", "1 Kibps")).toBe(1)
        expect(base("dataRate", "1 Kib/s")).toBe(1)
        expect(base("dataRate", "1 Kib per second")).toBe(1)
        expect(base("dataRate", "1600 GBps")).toBe((1600 * 1e9 * 8) / 1024)
    })

    it("rejects a data size where a data rate is required", () => {
        expect(parseQuantity("dataRate", "1 GiB").status).toBe("invalid")
    })
})

describe("formatQuantity", () => {
    it("picks the largest unit that keeps the number readable", () => {
        expect(formatQuantity("frequency", 3200)).toBe("3.2 GHz")
        expect(formatQuantity("frequency", 800)).toBe("800 MHz")
        expect(formatQuantity("dataSize", 131_072)).toBe("128 GiB")
        expect(formatQuantity("dataSize", 512)).toBe("512 MiB")
        expect(formatQuantity("power", 50_000)).toBe("50 kW")
        expect(formatQuantity("power", 400)).toBe("400 W")
    })

    it("falls back to the smallest unit for zero rather than dividing by a large step", () => {
        expect(formatQuantity("power", 0)).toBe("0 W")
        expect(formatQuantity("dataSize", 0)).toBe("0 KiB")
    })

    it("round-trips back through the parser", () => {
        for (const [kind, value] of [
            ["frequency", 3200],
            ["dataSize", 131_072],
            ["power", 50_000],
            ["dataRate", 1024],
        ] as Array<[QuantityKind, number]>) {
            expect(base(kind, formatQuantity(kind, value))).toBe(value)
        }
    })
})

describe("amountIn", () => {
    it("counts a value in the unit its field is written in", () => {
        expect(amountIn("frequency", 2600, "GHz")).toBe(2.6)
        expect(amountIn("frequency", 2600, "MHz")).toBe(2600)
        expect(amountIn("dataSize", 131_072, "GiB")).toBe(128)
        expect(amountIn("power", 10_000, "kW")).toBe(10)
    })

    // 2600 / 1000 is 2.5999999999999996 in floating point, and the SDK's own "%f" formatter writes
    // "10.000000 KWatts". Neither may reach the box the reader types into.
    it("shows a clean number for values the backend wrote", () => {
        expect(amountIn("power", base("power", "10.000000 KWatts"), "kW")).toBe(10)
        expect(amountIn("frequency", base("frequency", "2.6 GHz"), "GHz")).toBe(2.6)
        expect(amountIn("dataSize", base("dataSize", "1.5 GiB"), "GiB")).toBe(1.5)
    })

    // Anything written and read back in the same unit must survive the trip, or editing one field
    // would quietly rewrite a neighbouring value every time the inspector opened.
    it("round-trips whatever the editor writes", () => {
        for (const kind of KINDS) {
            const unit = DEFAULT_UNIT[kind]
            expect(amountIn(kind, base(kind, `12.5 ${unit}`), unit), `${unit} for ${kind}`).toBe(12.5)
        }
    })
})
