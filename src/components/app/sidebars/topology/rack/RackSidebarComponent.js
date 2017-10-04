import React from "react";
import LoadBarContainer from "../../../../../containers/app/sidebars/elements/LoadBarContainer";
import LoadChartContainer from "../../../../../containers/app/sidebars/elements/LoadChartContainer";
import BackToRoomContainer from "../../../../../containers/app/sidebars/topology/rack/BackToRoomContainer";
import DeleteRackContainer from "../../../../../containers/app/sidebars/topology/rack/DeleteRackContainer";
import MachineListContainer from "../../../../../containers/app/sidebars/topology/rack/MachineListContainer";
import RackNameContainer from "../../../../../containers/app/sidebars/topology/rack/RackNameContainer";
import "./RackSidebarComponent.css";

const RackSidebarComponent = ({ inSimulation, rackId }) => {
  return (
    <div className="rack-sidebar-container flex-column">
      <div className="rack-sidebar-header-container">
        <RackNameContainer />
        <BackToRoomContainer />
        {inSimulation ? (
          <div>
            <LoadBarContainer objectType="rack" objectId={rackId} />
            <LoadChartContainer objectType="rack" objectId={rackId} />
          </div>
        ) : (
          <div>
            <DeleteRackContainer />
          </div>
        )}
      </div>
      <div className="machine-list-container mt-2">
        <MachineListContainer />
      </div>
    </div>
  );
};

export default RackSidebarComponent;
