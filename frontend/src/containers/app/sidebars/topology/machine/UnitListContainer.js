import { connect } from 'react-redux'
import UnitListComponent from '../../../../../components/app/sidebars/topology/machine/UnitListComponent'

const mapStateToProps = (state, ownProps) => {
    return {
        inSimulation: state.currentExperimentId !== '-1',
        unitIds:
            state.objects.machine[
                state.objects.rack[
                        state.objects.tile[state.interactionLevel.tileId].rackId
                    ].machineIds[state.interactionLevel.position - 1]
                ][ownProps.unitType + 'Ids'],
    }
}

const UnitListContainer = connect(mapStateToProps)(UnitListComponent)

export default UnitListContainer
