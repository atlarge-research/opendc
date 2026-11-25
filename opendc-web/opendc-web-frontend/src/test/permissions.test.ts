import type { ProjectRole } from "@/lib/api/types"
import { type ProjectPermission, allows } from "@/lib/project/permissions"
import { describe, expect, it } from "vitest"

/** The app hides what the server would refuse, so its matrix has to be the server's. */
describe("what a project role allows", () => {
    const cases: [ProjectRole, ProjectPermission, boolean][] = [
        ["owner", "read", true],
        ["owner", "edit", true],
        ["owner", "manage", true],
        ["editor", "read", true],
        ["editor", "edit", true],
        ["editor", "manage", false],
        ["viewer", "read", true],
        ["viewer", "edit", false],
        ["viewer", "manage", false],
    ]

    it.each(cases)("%s may %s: %s", (role, permission, expected) => {
        expect(allows(role, permission)).toBe(expected)
    })
})
