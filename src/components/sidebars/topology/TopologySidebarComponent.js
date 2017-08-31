import React from "react";
import BuildingSidebarContent from "../../../containers/sidebars/topology/building/BuildingSidebarContent";
import Sidebar from "../Sidebar";

const TopologySidebarComponent = ({interactionLevel}) => {
    let sidebarHeading;
    let sidebarContent;

    switch (interactionLevel.mode) {
        case "BUILDING":
            sidebarHeading = "Building";
            sidebarContent = <BuildingSidebarContent/>;
            break;
        default:
            sidebarHeading = "Error";
            sidebarContent = "Missing Content";
    }

    return (
        <Sidebar isRight={true}>
            <h3>{sidebarHeading}</h3>
            {sidebarContent}
        </Sidebar>
    );
};

export default TopologySidebarComponent;
