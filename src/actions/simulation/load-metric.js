export const CHANGE_LOAD_METRIC = "CHANGE_LOAD_METRIC";

export function changeLoadMetric(metric) {
    return {
        type: CHANGE_LOAD_METRIC,
        metric
    };
}
