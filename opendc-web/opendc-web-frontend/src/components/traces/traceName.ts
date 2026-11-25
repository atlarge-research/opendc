// The server's rule for a trace name, applied after the same trim and lower-casing: "MyTrace" is
// accepted, "My Trace" is refused for its space.
const MAX_NAME_LENGTH = 100

const USABLE_NAME = new RegExp(`^[a-z0-9][a-z0-9._-]{0,${MAX_NAME_LENGTH - 1}}$`)

const NAME_CHARACTERS = "letters, digits, dots, dashes and underscores, starting with a letter or a digit"

export const TRACE_NAME_HELP = `Lower-case ${NAME_CHARACTERS}. Your handle stays in front, so only this part is yours to choose.`

/** What is wrong with [name], or nothing when the server will take it. */
export function traceNameProblem(name: string): string | undefined {
    const cleaned = name.trim().toLowerCase()
    if (cleaned === "") return "A name is needed."
    if (cleaned.length > MAX_NAME_LENGTH) return "That is too long."
    if (!USABLE_NAME.test(cleaned)) return `Use lower-case ${NAME_CHARACTERS}.`
    return undefined
}
