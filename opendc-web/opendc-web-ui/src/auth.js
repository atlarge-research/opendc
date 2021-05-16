import { LOG_IN_SUCCEEDED, LOG_OUT } from './redux/actions/auth'
import { DELETE_CURRENT_USER_SUCCEEDED } from './redux/actions/users'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/router'
import { useSelector } from 'react-redux'

const getAuthObject = () => {
    const authItem = global.localStorage && localStorage.getItem('auth')
    if (!authItem || authItem === '{}') {
        return undefined
    }
    return JSON.parse(authItem)
}

export const userIsLoggedIn = () => {
    const authObj = getAuthObject()

    if (!authObj || !authObj.googleId) {
        return false
    }

    const currentTime = new Date().getTime()
    return parseInt(authObj.expiresAt, 10) - currentTime > 0
}

export const getAuthToken = () => {
    const authObj = getAuthObject()
    if (!authObj) {
        return undefined
    }

    return authObj.authToken
}

export const saveAuthLocalStorage = (payload) => {
    localStorage.setItem('auth', JSON.stringify(payload))
}

export const clearAuthLocalStorage = () => {
    localStorage.setItem('auth', '')
}

export const authRedirectMiddleware = (store) => (next) => (action) => {
    switch (action.type) {
        case LOG_IN_SUCCEEDED:
            saveAuthLocalStorage(action.payload)
            window.location.href = '/projects'
            break
        case LOG_OUT:
        case DELETE_CURRENT_USER_SUCCEEDED:
            clearAuthLocalStorage()
            window.location.href = '/'
            break
        default:
            next(action)
            return
    }

    next(action)
}

export function useIsLoggedIn() {
    const [isLoggedIn, setLoggedIn] = useState(false)

    useEffect(() => {
        setLoggedIn(userIsLoggedIn())
    }, [])

    return isLoggedIn
}

export function useRequireAuth() {
    const router = useRouter()
    useEffect(() => {
        if (!userIsLoggedIn()) {
            router.replace('/')
        }
    })
}

export function useUser() {
    return useSelector((state) => state.auth)
}
