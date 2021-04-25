export const getState = (dispatch) =>
    new Promise((resolve) => {
        dispatch((dispatch, getState) => {
            resolve(getState())
        })
    })
