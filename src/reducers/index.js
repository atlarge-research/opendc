import {combineReducers} from "redux";
import {auth} from "./auth";
import {construction} from "./construction";
import {interactionLevel} from "./interaction-level";
import {map} from "./map";
import {modals} from "./modals";
import {objects} from "./objects";
import {simulationList} from "./simulation-list";
import {currentSimulationId} from "./simulations";
import {currentDatacenterId} from "./topology";

const rootReducer = combineReducers({
    auth,
    objects,
    modals,
    simulationList,
    currentSimulationId,
    currentDatacenterId,
    interactionLevel,
    construction,
    map,
});

export default rootReducer;
