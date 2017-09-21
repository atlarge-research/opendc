import React from "react";
import TaskContainer from "../../../containers/sidebars/simulation/TaskContainer";

const TraceComponent = ({jobs}) => (
    <div>
        <h3>Trace</h3>
        {jobs.map(job => (
            <div key={job.id}>
                <h4>Job: {job.name}</h4>
                <ul className="list-group">
                    {job.taskIds.map(taskId => (
                        <TaskContainer taskId={taskId} key={taskId}/>
                    ))}
                </ul>
            </div>
        ))}
    </div>
);

export default TraceComponent;
