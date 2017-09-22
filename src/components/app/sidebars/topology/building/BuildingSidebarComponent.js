import React from "react";
import NewRoomConstructionContainer from "../../../../../containers/app/sidebars/topology/building/NewRoomConstructionContainer";

const BuildingSidebarComponent = ({inSimulation}) => {
    return (
        <div>
            <h2>Building</h2>
            {inSimulation ?
                <div className="alert alert-info">
                    <span className="fa fa-info-circle mr-2"/>
                    <strong>Click on individual rooms</strong> to see their stats!
                </div> :
                <NewRoomConstructionContainer/>
            }
        </div>
    );
};

export default BuildingSidebarComponent;
