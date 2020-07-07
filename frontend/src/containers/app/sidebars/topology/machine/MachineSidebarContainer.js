import { connect } from 'react-redux'
import MachineSidebarComponent from '../../../../../components/app/sidebars/topology/machine/MachineSidebarComponent'

const mapStateToProps = state => {
    return {
        machineId:
            state.objects.rack[
                state.objects.tile[state.interactionLevel.tileId].rackId
                ].machineIds[state.interactionLevel.position - 1],
    }
}

const MachineSidebarContainer = connect(mapStateToProps)(
    MachineSidebarComponent,
)

export default MachineSidebarContainer
