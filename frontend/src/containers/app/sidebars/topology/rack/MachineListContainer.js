import { connect } from 'react-redux'
import MachineListComponent from '../../../../../components/app/sidebars/topology/rack/MachineListComponent'

const mapStateToProps = state => {
    return {
        machineIds:
        state.objects.rack[
            state.objects.tile[state.interactionLevel.tileId].rackId
            ].machineIds,
    }
}

const MachineListContainer = connect(mapStateToProps)(MachineListComponent)

export default MachineListContainer
