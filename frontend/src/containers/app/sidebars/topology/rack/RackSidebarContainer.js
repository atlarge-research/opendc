import { connect } from 'react-redux'
import RackSidebarComponent from '../../../../../components/app/sidebars/topology/rack/RackSidebarComponent'

const mapStateToProps = state => {
    return {
        rackId: state.objects.tile[state.interactionLevel.tileId].objectId,
        inSimulation: state.currentExperimentId !== -1,
    }
}

const RackSidebarContainer = connect(mapStateToProps)(RackSidebarComponent)

export default RackSidebarContainer
