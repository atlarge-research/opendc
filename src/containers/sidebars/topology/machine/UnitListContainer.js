import {connect} from "react-redux";
import UnitListComponent from "../../../../components/sidebars/topology/machine/UnitListComponent";

const mapStateToProps = (state, ownProps) => {
    return {
        unitIds: state.objects.machine[state.objects.rack[state.objects.tile[state.interactionLevel.tileId].objectId]
            .machineIds[state.interactionLevel.position - 1]][ownProps.unitType + "Ids"],
        inSimulation: state.currentExperimentId !== -1
    };
};

const UnitListContainer = connect(
    mapStateToProps
)(UnitListComponent);

export default UnitListContainer;
