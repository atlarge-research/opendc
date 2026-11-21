import { idParam } from "@/components/util/params"
import { describe, expect, it } from "vitest"

const params = (query: string) => new URLSearchParams(query)

describe("idParam", () => {
    it("reads an id from a link", () => {
        expect(idParam(params("id=8f14e45f-ea0c-4b6c-9f0a-1d2c3b4a5e6f"), "id")).toEqual({
            status: "ok",
            value: "8f14e45f-ea0c-4b6c-9f0a-1d2c3b4a5e6f",
        })
    })

    it("treats an absent or blank parameter as missing", () => {
        expect(idParam(params(""), "id").status).toBe("missing")
        expect(idParam(params("id="), "id").status).toBe("missing")
        expect(idParam(params("id=%20%20"), "id").status).toBe("missing")
    })

    it("tolerates surrounding whitespace from hand-edited links", () => {
        expect(idParam(params("id=%20abc%20"), "id")).toEqual({ status: "ok", value: "abc" })
    })

    it("passes an id it cannot vouch for through to the server", () => {
        expect(idParam(params("id=not-a-uuid"), "id")).toEqual({ status: "ok", value: "not-a-uuid" })
    })
})
