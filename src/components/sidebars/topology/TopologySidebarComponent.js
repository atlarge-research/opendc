import React from "react";
import BuildingSidebarContainer from "../../../containers/sidebars/topology/building/BuildingSidebarContainer";
import RoomSidebarContainer from "../../../containers/sidebars/topology/room/RoomSidebarContainer";
import Sidebar from "../Sidebar";
import RackSidebarComponent from "./rack/RackSidebarComponent";

const TopologySidebarComponent = ({interactionLevel}) => {
    let sidebarContent;

    switch (interactionLevel.mode) {
        case "BUILDING":
            sidebarContent = <BuildingSidebarContainer/>;
            break;
        case "ROOM":
            sidebarContent = <RoomSidebarContainer/>;
            break;
        case "RACK":
            sidebarContent = <RackSidebarComponent/>;
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
