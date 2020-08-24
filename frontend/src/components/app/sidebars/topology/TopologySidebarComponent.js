import React from "react";
import BuildingSidebarContainer from "../../../../containers/app/sidebars/topology/building/BuildingSidebarContainer";
import MachineSidebarContainer from "../../../../containers/app/sidebars/topology/machine/MachineSidebarContainer";
import RackSidebarContainer from "../../../../containers/app/sidebars/topology/rack/RackSidebarContainer";
import RoomSidebarContainer from "../../../../containers/app/sidebars/topology/room/RoomSidebarContainer";
import Sidebar from "../Sidebar";

const TopologySidebarComponent = ({ interactionLevel }) => {
  let sidebarContent;

  switch (interactionLevel.mode) {
    case "BUILDING":
      sidebarContent = <BuildingSidebarContainer />;
      break;
    case "ROOM":
      sidebarContent = <RoomSidebarContainer />;
      break;
    case "RACK":
      sidebarContent = <RackSidebarContainer />;
      break;
    case "MACHINE":
      sidebarContent = <MachineSidebarContainer />;
      break;
    default:
      sidebarContent = "Missing Content";
  }

  return <Sidebar isRight={true}>{sidebarContent}</Sidebar>;
};

export default TopologySidebarComponent;
