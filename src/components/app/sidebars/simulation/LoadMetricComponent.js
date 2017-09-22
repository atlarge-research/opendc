import React from "react";
import {SIM_HIGH_COLOR, SIM_LOW_COLOR, SIM_MID_HIGH_COLOR, SIM_MID_LOW_COLOR} from "../../../../util/colors";
import {LOAD_NAME_MAP} from "../../../../util/simulation-load";

const LoadMetricComponent = ({loadMetric}) => (
    <div>
        <div>Colors represent <strong>{LOAD_NAME_MAP[loadMetric]}</strong></div>
        <div className="btn-group mb-2" style={{display: "flex"}}>
            <span
                className="btn btn-secondary"
                style={{backgroundColor: SIM_LOW_COLOR, flex: 1}}
                title="0-25%"
            />
            <span
                className="btn btn-secondary"
                style={{backgroundColor: SIM_MID_LOW_COLOR, flex: 1}}
                title="25-50%"
            />
            <span
                className="btn btn-secondary"
                style={{backgroundColor: SIM_MID_HIGH_COLOR, flex: 1}}
                title="50-75%"
            />
            <span
                className="btn btn-secondary"
                style={{backgroundColor: SIM_HIGH_COLOR, flex: 1}}
                title="75-100%"
            />
        </div>
    </div>
);

export default LoadMetricComponent;
