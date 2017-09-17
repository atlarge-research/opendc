import React from "react";
import NewRoomConstructionContainer from "../../../../containers/sidebars/topology/building/NewRoomConstructionContainer";

const BuildingSidebarComponent = ({inSimulation}) => {
    return (
        <div>
            <h2>Building</h2>
            {inSimulation ?
                undefined :
                <NewRoomConstructionContainer/>
            }
        </div>
    );
};

export default BuildingSidebarComponent;
