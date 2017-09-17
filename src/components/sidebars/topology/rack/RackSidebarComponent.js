import React from "react";
import LoadBarContainer from "../../../../containers/sidebars/elements/LoadBarContainer";
import LoadChartContainer from "../../../../containers/sidebars/elements/LoadChartContainer";
import DeleteRackContainer from "../../../../containers/sidebars/topology/rack/DeleteRackContainer";
import MachineListContainer from "../../../../containers/sidebars/topology/rack/MachineListContainer";
import RackNameContainer from "../../../../containers/sidebars/topology/rack/RackNameContainer";
import "./RackSidebarComponent.css";

const RackSidebarComponent = ({inSimulation, rackId}) => {
    return (
        <div className="rack-sidebar-container flex-column">
            <RackNameContainer/>
            {inSimulation ?
                <div>
                    <LoadBarContainer objectType="rack" objectId={rackId}/>
                    <LoadChartContainer objectType="rack" objectId={rackId}/>
                </div> :
                <div>
                    <DeleteRackContainer/>
                </div>
            }
            <div className="machine-list-container mt-2">
                <MachineListContainer/>
            </div>
        </div>
    );
};

export default RackSidebarComponent;
