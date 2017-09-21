import React from "react";
import TaskContainer from "../../../containers/sidebars/simulation/TaskContainer";

const TraceComponent = ({jobs}) => (
    <div>
        <h3>Trace</h3>
        {jobs.map(job => (
            <ul className="list-group" key={job.id}>
                {job.taskIds.map(taskId => (
                    <TaskContainer taskId={taskId} key={taskId}/>
                ))}
            </ul>
        ))}
    </div>
);

export default TraceComponent;
