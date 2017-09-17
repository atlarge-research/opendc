export const SET_PLAYING = "SET_PLAYING";

export function playSimulation() {
    return {
        type: SET_PLAYING,
        playing: true
    }
}

export function pauseSimulation() {
    return {
        type: SET_PLAYING,
        playing: false
    }
}
