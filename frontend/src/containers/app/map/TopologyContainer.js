import { connect } from 'react-redux'
import TopologyGroup from '../../../components/app/map/groups/TopologyGroup'

const mapStateToProps = state => {
    if (state.currentTopologyId === '-1') {
        return {}
    }

    return {
        topology: state.objects.topology[state.currentTopologyId],
        interactionLevel: state.interactionLevel,
    }
}

const TopologyContainer = connect(mapStateToProps)(TopologyGroup)

export default TopologyContainer
