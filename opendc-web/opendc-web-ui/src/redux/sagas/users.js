import { call, put } from 'redux-saga/effects'
import { logInSucceeded } from '../actions/auth'
import { addToStore } from '../actions/objects'
import { fetchAuthorizationsOfCurrentUserSucceeded } from '../actions/users'
import { performTokenSignIn } from '../../api/token-signin'
import { addUser } from '../../api/users'
import { saveAuthLocalStorage } from '../../auth'
import { fetchAndStoreProject, fetchAndStoreUser } from './objects'

export function* onFetchLoggedInUser(action) {
    try {
        const tokenResponse = yield call(performTokenSignIn, action.payload.authToken)

        let userId = tokenResponse.userId

        if (tokenResponse.isNewUser) {
            saveAuthLocalStorage({ authToken: action.payload.authToken })
            const newUser = yield call(addUser, action.payload)
            userId = newUser._id
        }

        yield put(logInSucceeded(Object.assign({ userId }, action.payload)))
    } catch (error) {
        console.error(error)
    }
}

export function* onFetchAuthorizationsOfCurrentUser(action) {
    try {
        const user = yield call(fetchAndStoreUser, action.userId)

        for (const authorization of user.authorizations) {
            authorization.userId = action.userId
            yield put(addToStore('authorization', authorization))
            yield fetchAndStoreProject(authorization.projectId)
        }

        const authorizationIds = user.authorizations.map((authorization) => [action.userId, authorization.projectId])

        yield put(fetchAuthorizationsOfCurrentUserSucceeded(authorizationIds))
    } catch (error) {
        console.error(error)
    }
}
