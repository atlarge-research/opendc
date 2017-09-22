export const dummyMiddleware = store => next => action => {
    next(action);
};
