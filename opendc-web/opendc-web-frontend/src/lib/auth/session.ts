import { createContext, useContext } from "react"

/** What the person can do about their session, which depends on how the deployment signs people in. */
export type SessionControls =
    | { type: "anonymous" }
    | { type: "auth0"; signIn: () => void; signOut: () => void; hint: { nickname: string; name: string } }

const SessionContext = createContext<SessionControls>({ type: "anonymous" })

export const SessionProvider = SessionContext.Provider

export function useSessionControls(): SessionControls {
    return useContext(SessionContext)
}
