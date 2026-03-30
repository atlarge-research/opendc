import { fork } from 'redux-saga/effects'
import { watchServer, updateServer } from './topology'

export default function* rootSaga() {
    yield fork(watchServer)
    yield fork(updateServer)
}
