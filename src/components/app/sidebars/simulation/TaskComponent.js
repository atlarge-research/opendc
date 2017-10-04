import approx from "approximate-number";
import classNames from "classnames";
import React from "react";
import { convertSecondsToFormattedTime } from "../../../../util/date-time";

const TaskComponent = ({ task, flopsLeft }) => {
  let icon;
  let progressBarContent;
  let percent;
  let infoTitle;

  if (flopsLeft === task.totalFlopCount) {
    icon = "hourglass-half";
    progressBarContent = "";
    percent = 0;
    infoTitle = "Not submitted yet";
  } else if (flopsLeft > 0) {
    icon = "refresh";
    progressBarContent = approx(task.totalFlopCount - flopsLeft) + " FLOP";
    percent = 100 * (task.totalFlopCount - flopsLeft) / task.totalFlopCount;
    infoTitle =
      progressBarContent + " (" + Math.round(percent * 10) / 10 + "%)";
  } else {
    icon = "check";
    progressBarContent = "Completed";
    percent = 100;
    infoTitle = "Completed";
  }

  return (
    <li className="list-group-item flex-column align-items-start">
      <div className="d-flex w-100 justify-content-between">
        <h5 className="mb-1">{approx(task.totalFlopCount)} FLOP</h5>
        <small>Starts at {convertSecondsToFormattedTime(task.startTick)}</small>
      </div>
      <div title={infoTitle} style={{ display: "flex" }}>
        <span
          className={classNames("fa", "fa-" + icon)}
          style={{ width: "20px" }}
        />
        <div className="progress" style={{ flexGrow: 1 }}>
          <div
            className="progress-bar"
            role="progressbar"
            aria-valuenow={percent}
            aria-valuemin="0"
            aria-valuemax="100"
            style={{ width: percent + "%" }}
          >
            {progressBarContent}
          </div>
        </div>
      </div>
    </li>
  );
};

export default TaskComponent;
