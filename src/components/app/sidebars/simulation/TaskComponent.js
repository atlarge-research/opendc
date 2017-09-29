import approx from "approximate-number";
import React from "react";
import {convertSecondsToFormattedTime} from "../../../../util/date-time";

const TaskComponent = ({task, flopsLeft}) => {
    let stateInfo;

    if (flopsLeft === task.totalFlopCount) {
        stateInfo = (
            <div>
                <span className="fa fa-hourglass-half mr-2"/>
                Waiting
            </div>
        );
    } else if (flopsLeft > 0) {
        stateInfo = (
            <div>
                <span className="fa fa-refresh mr-2"/>
                Running ({approx(task.totalFlopCount - flopsLeft)} / {approx(task.totalFlopCount)} FLOP)
            </div>
        );
    } else {
        stateInfo = (
            <div>
                <span className="fa fa-check mr-2"/>
                Completed
            </div>
        );
    }

    return (
        <li className="list-group-item flex-column align-items-start">
            <div className="d-flex w-100 justify-content-between">
                <h5 className="mb-1">{approx(task.totalFlopCount)} FLOP</h5>
                <small>Starts at {convertSecondsToFormattedTime(task.startTick)}</small>
            </div>
            {stateInfo}
        </li>
    );
};

export default TaskComponent;
