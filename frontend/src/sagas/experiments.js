import { call, put, select } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { deleteExperiment, getExperiment } from '../api/routes/experiments'
import { addExperiment, getProject } from '../api/routes/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject, fetchTopologyOfExperiment } from './topology'

export function* onOpenExperimentSucceeded(action) {
    try {
        const project = yield call(getProject, action.projectId)
        yield put(addToStore('project', project))

        const experiment = yield call(getExperiment, action.experimentId)
        yield put(addToStore('experiment', experiment))

        yield fetchExperimentSpecifications()

        yield fetchTopologyOfExperiment(experiment)
    } catch (error) {
        console.error(error)
    }
}

export function* onFetchExperimentsOfProject() {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)
        const currentProject = yield select((state) => state.object.project[currentProjectId])

        yield fetchExperimentSpecifications()

        for (let i in currentProject.experimentIds) {
            const experiment = yield call(getExperiment, currentProject.experimentIds[i])
            yield put(addToStore('experiment', experiment))
        }
    } catch (error) {
        console.error(error)
    }
}

function* fetchExperimentSpecifications() {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)
        yield fetchAndStoreAllTopologiesOfProject(currentProjectId)
        yield fetchAndStoreAllTraces()
        yield fetchAndStoreAllSchedulers()
    } catch (error) {
        console.error(error)
    }
}

export function* onAddExperiment(action) {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)

        const experiment = yield call(
            addExperiment,
            currentProjectId,
            Object.assign({}, action.experiment, {
                id: '-1',
                projectId: currentProjectId,
            }),
        )
        yield put(addToStore('experiment', experiment))

        const experimentIds = yield select((state) => state.objects.project[currentProjectId].experimentIds)
        yield put(
            addPropToStoreObject('project', currentProjectId, {
                experimentIds: experimentIds.concat([experiment._id]),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteExperiment(action) {
    try {
        yield call(deleteExperiment, action.id)

        const currentProjectId = yield select((state) => state.currentProjectId)
        const experimentIds = yield select((state) => state.objects.project[currentProjectId].experimentIds)

        yield put(
            addPropToStoreObject('project', currentProjectId, {
                experimentIds: experimentIds.filter((id) => id !== action.id),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}
