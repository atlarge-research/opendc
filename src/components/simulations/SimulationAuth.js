import classNames from 'classnames';
import React from 'react';
import SimulationActions from "../../containers/simulations/SimulationActions";
import Shapes from "../../shapes/index";
import {AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP} from "../../util/authorizations";
import {parseAndFormatDateTime} from "../../util/date-time";

const SimulationAuth = ({simulationAuth}) => (
    <div className="simulation-row">
        <div>{simulationAuth.simulation.name}</div>
        <div>{parseAndFormatDateTime(simulationAuth.simulation.datetimeLastEdited)}</div>
        <div>
            <span className={classNames("fa", "fa-" + AUTH_ICON_MAP[simulationAuth.authorizationLevel])}/>
            {AUTH_DESCRIPTION_MAP[simulationAuth.authorizationLevel]}
        </div>
        <SimulationActions simulationId={simulationAuth.simulation.id}/>
    </div>
);

SimulationAuth.propTypes = {
    simulationAuth: Shapes.Authorization.isRequired,
};

export default SimulationAuth;
