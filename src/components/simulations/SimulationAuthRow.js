import classNames from 'classnames';
import React from 'react';
import SimulationActions from "../../containers/simulations/SimulationActions";
import Shapes from "../../shapes/index";
import {AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP} from "../../util/authorizations";
import {parseAndFormatDateTime} from "../../util/date-time";

const SimulationAuthRow = ({simulationAuth}) => (
    <tr>
        <td>{simulationAuth.simulation.name}</td>
        <td>{parseAndFormatDateTime(simulationAuth.simulation.datetimeLastEdited)}</td>
        <td>
            <span className={classNames("fa", "fa-" + AUTH_ICON_MAP[simulationAuth.authorizationLevel], "mr-2")}/>
            {AUTH_DESCRIPTION_MAP[simulationAuth.authorizationLevel]}
        </td>
        <SimulationActions simulationId={simulationAuth.simulation.id}/>
    </tr>
);

SimulationAuthRow.propTypes = {
    simulationAuth: Shapes.Authorization.isRequired,
};

export default SimulationAuthRow;
