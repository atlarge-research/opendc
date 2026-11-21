const MIN_LENGTH = 3

const MAX_LENGTH = 32

/** The server's rule for handles: a letter first, no dash last. */
export const HANDLE_PATTERN = new RegExp(`^[a-z][a-z0-9-]{${MIN_LENGTH - 2},${MAX_LENGTH - 2}}[a-z0-9]$`)

export const HANDLE_RULE = `${MIN_LENGTH} to ${MAX_LENGTH} lowercase letters, digits or dashes, starting with a letter`

/** A handle offered to someone choosing one; reserved names are left to the server. */
export function suggestHandle(nickname: string): string {
    let handle = nickname
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
    if (!/^[a-z]/.test(handle)) handle = `u-${handle}`
    handle = handle.slice(0, MAX_LENGTH).replace(/-+$/, "")
    while (handle.length < MIN_LENGTH) handle = `${handle}0`
    return handle
}
