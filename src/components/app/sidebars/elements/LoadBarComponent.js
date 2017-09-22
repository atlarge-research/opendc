import classNames from "classnames";
import React from "react";

const LoadBarComponent = ({percent, disabled}) => (
    <div className="mt-1">
        <strong>Current load</strong>
        <div className={classNames("progress", {disabled})}>
            <div
                className="progress-bar"
                role="progressbar"
                aria-valuenow={percent}
                aria-valuemin="0"
                aria-valuemax="100"
                style={{width: percent + "%"}}
            >
                {percent}%
            </div>
        </div>
    </div>
);

export default LoadBarComponent;
