import { getAll, getById } from './util'

export function getAllCoolingItems() {
    return getAll('/specifications/cooling-items')
}

export function getCoolingItem(id) {
    return getById('/specifications/cooling-items/{id}', { id })
}

export function getAllCPUs() {
    return getAll('/specifications/cpus')
}

export function getCPU(id) {
    return getById('/specifications/cpus/{id}', { id })
}

export function getAllFailureModels() {
    return getAll('/specifications/failure-models')
}

export function getFailureModel(id) {
    return getById('/specifications/failure-models/{id}', { id })
}

export function getAllGPUs() {
    return getAll('/specifications/gpus')
}

export function getGPU(id) {
    return getById('/specifications/gpus/{id}', { id })
}

export function getAllMemories() {
    return getAll('/specifications/memories')
}

export function getMemory(id) {
    return getById('/specifications/memories/{id}', { id })
}

export function getAllPSUs() {
    return getAll('/specifications/psus')
}

export function getPSU(id) {
    return getById('/specifications/psus/{id}', { id })
}

export function getAllStorages() {
    return getAll('/specifications/storages')
}

export function getStorage(id) {
    return getById('/specifications/storages/{id}', { id })
}
