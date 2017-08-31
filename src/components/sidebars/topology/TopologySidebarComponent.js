import React from "react";
import BuildingSidebarContainer from "../../../containers/sidebars/topology/building/BuildingSidebarContainer";
import RoomSidebarContainer from "../../../containers/sidebars/topology/room/RoomSidebarContainer";
import Sidebar from "../Sidebar";

const TopologySidebarComponent = ({interactionLevel}) => {
    let sidebarContent;

    switch (interactionLevel.mode) {
        case "BUILDING":
            sidebarContent = <BuildingSidebarContainer/>;
            break;
        case "ROOM":
            sidebarContent = <RoomSidebarContainer/>;
            break;
        default:
            sidebarContent = "Missing Content";
    }

    return (
        <Sidebar isRight={true}>
            {sidebarContent}
        </Sidebar>
    );
};

export default TopologySidebarComponent;
