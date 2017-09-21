import React from "react";
import {convertSecondsToFormattedTime} from "../../../util/date-time";

const TaskComponent = ({task, flopsLeft}) => {
    let stateInfo;

    if (flopsLeft === task.totalFlopCount) {
        stateInfo = <p><span className="fa fa-hourglass-half"/>Waiting</p>;
    } else if (flopsLeft > 0) {
        stateInfo = (
            <p>
                <span className="fa fa-refresh"/>
                Running ({task.totalFlopCount - flopsLeft} / {task.totalFlopCount} FLOPS)
            </p>
        );
    } else {
        stateInfo = <p><span className="fa fa-check"/>Completed</p>;
    }

    return (
        <li className="list-group-item flex-column align-items-start">
            <div className="d-flex w-100 justify-content-between">
                <h5 className="mb-1">{task.totalFlopCount} FLOPS</h5>
                <small>Starts: {convertSecondsToFormattedTime(task.startTick)}</small>
            </div>
            {stateInfo}
        </li>
    );
};

export default TaskComponent;
